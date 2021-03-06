package ece428.mp1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Introducer extends Servent {
    protected PriorityQueue<NodeID> priorityQueue;

    /**
     * The introducer inheritcs from the Servent because it ALSO acts as a Servent.
     * We set a priority queue to ensure that the max(5, priorityQueue.size()) nodes are
     * the K nodes that the priority queue selects.
     *
     * @throws IOException
     */
    public Introducer() throws IOException {
        super();
        this.priorityQueue = new PriorityQueue<NodeID>(new Comparator<NodeID>() {
            @Override
            public int compare(final NodeID n1, final NodeID n2) {
                if (n1.getStartTime() < n2.getStartTime()) {
                    return -1;
                }
                return 1;
            }
        });
        final MembershipListEntry masterEntry = this.membershipList.listEntries.get(this.self);
        masterEntry.setMaster(true);
        this.membershipList.listEntries.put(this.self, masterEntry);
    }


    /**
     * This is the K nodes that the introducer will select. We pick the top 5 elements from our priority queue.
     *
     * @return
     */
    @Override
    protected ArrayList<NodeID> getKNodes() {
        final ArrayList<NodeID> returnList = new ArrayList<NodeID>();
        for (int i = 0; i < 5; i++) {
            if (this.priorityQueue.size() == 0) {
                break;
            }
            returnList.add(this.priorityQueue.poll());
        }
        return returnList;
    }


    /**
     * We clear the priority queue and add in the new nodes into the priority queue.
     * We do this because the priority queue has to be upated on nodes that have been failed or not.
     *
     * @param incomingPacket - The incoming packet from other servents.
     * @throws IOException
     */
    @Override
    protected void retrieveData(final DatagramPacket incomingPacket) throws IOException {
        super.retrieveData(incomingPacket);
        this.priorityQueue.clear();
        final Iterator it = this.membershipList.listEntries.entrySet().iterator();
        while (it.hasNext()) {
            final HashMap.Entry pair = (HashMap.Entry) it.next();
            final NodeID key = (NodeID) pair.getKey();
            this.priorityQueue.add(key);
        }
        verifyMasters();
    }

    @Override
    public void startServent() throws Exception {
        // Heartbeating + Failure Detection methods
        super.startHeartbeatServer();
        Thread.sleep(1500);
        super.heartBeat();

        //start master server script
        startSDFSServer();
        startSDFSInput();
    }

    private void verifyMasters() {
        final Iterator i = this.membershipList.listEntries.entrySet().iterator();
        ConcurrentHashMap.Entry pair;
        NodeID key;
        MembershipListEntry entry;

        int masterCount = 0;
        while (i.hasNext()) {
            pair = (ConcurrentHashMap.Entry) i.next();
            key = (NodeID) pair.getKey();
            entry = this.membershipList.listEntries.get(key);
            if (entry.isMaster() && entry.getAlive()) {
                masterCount++;
            }
        }

        if (masterCount == 4) {
            entry = this.membershipList.listEntries.get(this.self);
            entry.setMaster(false);
            this.membershipList.listEntries.put(this.self, entry);
        }
    }
}
