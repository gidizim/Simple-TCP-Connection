import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class MessageHandler {

    public static String receiveMessageOverSocket(Socket clientSocket) {
        String response = null;
        try {
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            Integer responseLength = dis.readInt();
            response = new String(dis.readNBytes(responseLength));
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert(response != null);
        return response;
    }

    public static void sendMessageOverSocket(Socket clientSocket, String msg) {
        try {
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            dos.writeInt(msg.length());
            dos.writeBytes(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
