package ece428.mp1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Network {
    
    public static void sendData(final String hostAddress, final Integer portNumber, final String data) {
        try {
            final Socket socket = new Socket(hostAddress, portNumber);
            final DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.writeUTF(data);
            dataOutputStream.close();
            socket.close();
        } catch (final Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    public static String receiveData(final ServerSocket serverSocket) {
        String line = "";
        try {
            final Socket socket = serverSocket.accept();
            final DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            line = dataInputStream.readUTF();
            dataInputStream.close();
            socket.close();
        } catch (final Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        return line;
    }
}
