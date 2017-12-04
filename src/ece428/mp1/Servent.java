package ece428.mp1;


import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Servent {
    public static Integer SEND_PORT = 1234;
    public static Integer RECEIVE_PORT = 1235;
    public static Integer SEND_PORT_SDFS = 1236;
    public static Integer RECEIVE_PORT_SDFS = 1237;
    public final NodeID INTRODUCER_NODE;
    protected final Integer MACHINE_NUMBER = Integer.parseInt(new BufferedReader(new FileReader("../number.txt")).readLine());
    public MasterData masterData;
    protected MembershipList membershipList;
    protected ArrayList<NodeID> heartBeatList;
    protected DatagramSocket socketClient;
    protected DatagramSocket serverSocket;
    protected NodeID self;
    protected PrintStream printStream;
    protected ServerSocket SDFSServerSocket;

    public Servent() throws IOException {
        //region MP2 Stuff
        this.printStream = new PrintStream(new FileOutputStream(new File("../output.txt")));
//        this.printStream.println("First line!");

        this.membershipList = new MembershipList(this.self);
        this.INTRODUCER_NODE = new NodeID(InetAddress.getByName("fa17-cs425-g39-01.cs.illinois.edu"));

        InetAddress inetAddress = null;
        try {
            if (this.MACHINE_NUMBER == 10) {
                inetAddress = InetAddress.getByName("fa17-cs425-g39-" + this.MACHINE_NUMBER.toString() + ".cs.illinois.edu");
            } else {
                inetAddress = InetAddress.getByName("fa17-cs425-g39-0" + this.MACHINE_NUMBER.toString() + ".cs.illinois.edu");
            }
        } catch (final UnknownHostException e) {
            e.printStackTrace();
            System.out.println(e.getLocalizedMessage());
        }
        this.self = new NodeID(inetAddress);
        this.membershipList.addNewNode(this.self);
        this.membershipList.addNewNode(this.INTRODUCER_NODE);

        this.serverSocket = new DatagramSocket(
                SEND_PORT,
                inetAddress
        );
        //endregion

        // MP3 Additions
        this.SDFSServerSocket = new ServerSocket(RECEIVE_PORT_SDFS);
        this.masterData = new MasterData();

        try {
            final TerminalRunner terminalRunner = new TerminalRunner("clear");
            terminalRunner.run();
        } catch (final Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    public void setMasterData(final MasterData masterData) {
        Servent.this.masterData = masterData;
    }

    /**
     * Starts the servent.
     */
    public void startServent() throws Exception {
        // Heartbeating + Failure Detection methods
        startHeartbeatServer();
        heartBeat();

        //start master server script
        startSDFSServer();
        startSDFSInput();
    }

    /**
     * Starts the "server" part of the servent on a new thread.
     */
    protected void startHeartbeatServer() {
        new Thread() {
            @Override
            public synchronized void run() {
                try {
                    while (true) {
                        final byte[] incomingByteStream = new byte[
                                (int) (Math.pow(2, 10) * Servent.this.membershipList.listEntries.size())
                                ];
                        final DatagramPacket incomingPacket = new DatagramPacket(
                                incomingByteStream, incomingByteStream.length
                        );

                        // THIS LINE IS BLOCKING
                        // It waits for this machine to receive some packet
                        Servent.this.serverSocket.receive(incomingPacket);
                        retrieveData(incomingPacket);
                    }
                } catch (final IOException e) {
                    System.err.println(e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * This is where we send heartbeats to K random nodes.
     */
    protected void heartBeat() {
        new Thread() {
            @Override
            public synchronized void run() {
                try {
                    while (true) {
                        Servent.this.membershipList.incrementHeartBeatCount(Servent.this.self);
                        Servent.this.heartBeatList = getKNodes();

                        for (final NodeID nodeID : Servent.this.heartBeatList) {
                            heartBeat(nodeID);
                        }
                        Thread.sleep(500);
                    }
                } catch (final InterruptedException e) {
                    System.out.println(e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void startSDFSServer() {
        new Thread() {
            @Override
            public void run() {
                String line;
                RequestHandler requestHandler;
                ResponseHandler responseHandler;
                MasterHandler masterHandler;
                while (true) {
                    line = Network.receiveData(Servent.this.SDFSServerSocket);
                    if (line.startsWith("request")) {
                        requestHandler = new RequestHandler(Servent.this);
                        requestHandler.handleRequest(line);
                    } else if (line.startsWith("response")) {
                        try {
                            responseHandler = new ResponseHandler(Servent.this);
                            responseHandler.handleResponse(line);
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                    } else if (line.startsWith("master")) {
                        masterHandler = new MasterHandler(Servent.this);
                        masterHandler.handleMaster(line);
//                        System.out.println("MASTER_DATA: " + Servent.this.masterData.serialize());
                    } else {
                        System.out.println("Could not parse Network.receiveData()");
                    }
                }
            }
        }.start();
    }

    public void startSDFSInput() {
        new Thread() {
            @Override
            public void run() {
                // handle inputs here
                System.out.println("Welcome to SDFS, input a command!");
                InputParser inputParser;
                while (true) {
                    try {
                        //need to figure out correct master
                        inputParser = new InputParser(
                                new Scanner(System.in).nextLine(),
                                Servent.this.INTRODUCER_NODE.getIPAddress().getHostAddress(),
                                Servent.this.self,
                                Servent.this.membershipList,
                                Servent.this.masterData
                        );
                        inputParser.run();
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
    // endregion

    /**
     * Retrieves the data from an incoming packet and updates the membership list acordingly.
     *
     * @param incomingPacket - The incoming packet from other servents.
     * @throws IOException
     */
    protected void retrieveData(final DatagramPacket incomingPacket) throws IOException {
        final String data = new String(incomingPacket.getData());
        final MembershipList other = new ObjectSerialization(data).getMembershipList();

        final MembershipListEntry selfInOther = other.listEntries.get(this.self);
        final MembershipListEntry selfInMembershipList = this.membershipList.listEntries.get(this.self);


        if (selfInOther != null && selfInOther.getLocalTime() < 0) {
            selfInMembershipList.setHeartBeatCounter(selfInOther.getHeartBeatCounter());
        }

        this.membershipList.updateEntries(other);
        this.membershipList.setSelf(this.self);
        this.printStream.println(other.toString());
        selfInMembershipList.updateLocalTime();

        if (selfInMembershipList.isMaster()) {
            final ArrayList<NodeID> curDeadNodes = this.membershipList.getDeadNodesArray();
            this.masterData.rereplicate(curDeadNodes, this.membershipList);

            if (this.membershipList.electMasters()) {
                this.masterData.syncMasters(this.membershipList);
            }
        }
        //        System.out.println("Length: " + incomingPacket.getData().length);
    }

    /**
     * We select K random nodes EVERY time we run this function. However, we also check to make sure
     * that the node is alive before we mark it as one of our K nodes.
     */
    protected ArrayList<NodeID> getKNodes() {
        final ArrayList<NodeID> allKeys = new ArrayList<>(this.membershipList.listEntries.keySet());
        allKeys.remove(this.self);
        if (allKeys.size() <= 5) {
            return allKeys;
        }
        final ArrayList<NodeID> returnList = new ArrayList<>();
        final Random rand = new Random();
        allKeys.remove(this.INTRODUCER_NODE);
        NodeID node;
        for (int i = 0; i < 4; i++) {
            while (allKeys.size() > 0) {
                node = allKeys.remove(rand.nextInt(allKeys.size()));
                if (this.membershipList.listEntries.get(node).getAlive()) {
                    returnList.add(node);
                    break;

                }
            }
        }
        returnList.add(this.INTRODUCER_NODE);
        return returnList;
    }

    /**
     * This sends a heartbeat to ONE node, which is passed in. Wrapped into a function for easier debugging.
     *
     * @param nodeID
     */
    protected void heartBeat(final NodeID nodeID) {
        try {
            Servent.this.socketClient = new DatagramSocket(
                    RECEIVE_PORT,
                    this.self.getIPAddress()
            );

            final byte[] data = new ObjectSerialization(Servent.this.membershipList).toString().getBytes();
            final DatagramPacket sendPacket = new DatagramPacket(
                    data, data.length,
                    nodeID.getIPAddress(),
                    SEND_PORT
            );
            Servent.this.socketClient.send(sendPacket);

            Servent.this.socketClient.close();
        } catch (final IOException e) {
            System.err.println(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
}