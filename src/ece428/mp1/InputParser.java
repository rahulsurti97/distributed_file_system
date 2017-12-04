package ece428.mp1;


public class InputParser {

    String masterHostAddress;
    NodeID nodeID;
    String inputCommand;
    MembershipList membershipList;
    MasterData masterData;

    public InputParser(final String inputCommand,
                       final String masterHostAddress,
                       final NodeID nodeID,
                       final MembershipList membershipList,
                       final MasterData masterData)
            throws Exception {
        this.masterHostAddress = masterHostAddress;
        this.nodeID = nodeID;
        this.inputCommand = inputCommand;
        this.membershipList = membershipList;
        this.masterData = masterData;
    }


    public void run() throws Exception {
        final String[] commandSplit = this.inputCommand.trim().split("\\s+");
        if (this.inputCommand.isEmpty()) {
            return;
        }
        final String cmd = commandSplit[0].toLowerCase();
        boolean invalid = false;
        StringBuilder line = new StringBuilder();
        line.append("request ")
                .append(this.nodeID.getIPAddress().getHostAddress()).append(" ")
                .append(this.nodeID.getIPAddress().getHostName()).append(" ")
                .append(cmd.toLowerCase()).append(" ");
        switch (cmd) {
            case "put":
                if (commandSplit.length == 3) {
                    final String localFileName = commandSplit[1];
                    final String SDFSFileName = commandSplit[2];
                    line.append(localFileName).append(" ").append(SDFSFileName);
                } else {
                    invalid = true;
                }
                break;
            case "get":
                if (commandSplit.length == 3) {
                    line.append(commandSplit[1]).append(" ").append(commandSplit[2]);
                } else {
                    invalid = true;
                }
                break;
            case "delete":
                if (commandSplit.length == 2) {
                    line = new StringBuilder();
                    line.append("master delete ").append(commandSplit[1]);
                } else {
                    invalid = true;
                }
                break;
            case "ls":
                if (commandSplit.length == 2) {
                    line.append(commandSplit[1]);
                } else {
                    invalid = true;
                }
                break;
            case "store":
                if (commandSplit.length == 1) {
                    final TerminalRunner terminalRunner = new TerminalRunner("store");
                    terminalRunner.run();
                    return;
                } else {
                    invalid = true;
                }
                break;
            case "master":
                if (commandSplit.length == 1) {
                    if (this.membershipList.isMainMaster()) {
                        System.out.println("MAIN");
                    }
                    System.out.println(this.masterData.toString());
                    return;
                } else {
                    invalid = true;
                }
                break;
            case "local":
                if (commandSplit.length == 1) {
                    final TerminalRunner terminalRunner = new TerminalRunner("local");
                    terminalRunner.run();
                    return;
                } else {
                    invalid = true;
                }
                break;
            case "memlist":
                if (commandSplit.length == 1) {
                    System.out.println(this.membershipList.toString());
                    return;
                } else {
                    invalid = true;
                }
                break;
            case "clear":
                for (int i = 0; i < 50; i++) {
                    System.out.println("");
                }
                return;
            default:
                invalid = true;
        }
        if (!invalid) {
            Network.sendData(this.membershipList.getMainMaster().getIPAddress().getHostAddress(), Servent.RECEIVE_PORT_SDFS, line.toString());
        } else {
            System.out.println("> INVALID COMMAND!");
        }
    }
}
