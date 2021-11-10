import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Server {

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("===== Error usage: java server_port block_duration timeout =====");
            return;
        }
        int serverPort;
        int timeout;
        int blockDuration;
        try {
            serverPort = Integer.parseInt(args[0]);
            timeout = Integer.parseInt(args[1]); timeout = timeout * 1000; // Turn into nanoseconds
            blockDuration = Integer.parseInt(args[1]);
        } catch (NumberFormatException e ){
            System.out.println("Error usage: java Server server_port block_duration timeout");
            return;
        }

        ServerSocket serverSocket = new ServerSocket(serverPort);
        System.out.println("===== Server is running =====");

        Map<String, Profile> userProfilesMap = loadCredentials();

        while (true) {
            Socket clientSocket = null;
            try {
                System.out.println("===== Waiting for connection request from clients...=====");
                clientSocket = serverSocket.accept();
                System.out.println("===== New client socket=====");

                String response = MessageHandler.receiveMessageOverSocket(clientSocket);
                assert(!response.equals("HELLO"));

                String clientListenerPort = MessageHandler.receiveMessageOverSocket(clientSocket);
                Thread clientThread = new ClientHandler(clientSocket, Integer.parseInt(clientListenerPort), userProfilesMap, timeout, blockDuration);
                clientThread.start();

            } catch (Exception e) {
                if (clientSocket != null) {
                    clientSocket.close();
                }
                e.printStackTrace();
            }
        }
    }

    private static Map<String,Profile> loadCredentials() throws IOException {
        Map<String, Profile> userProfilesMap = new HashMap<>();
        try {
            File file = new File("credentials.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitArray = line.split(" ");
                Profile profile = new Profile(splitArray[0], splitArray[1]);
                userProfilesMap.put(splitArray[0], profile);
            }
            assert(!userProfilesMap.isEmpty());
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
        return userProfilesMap;
    }

}