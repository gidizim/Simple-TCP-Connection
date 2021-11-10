// Java implementation for a client
// Save file as Client.java

import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

// Client class
public class Client {
    private int listenerPort;
    private int serverPort;
    private Socket socket;
    private static Set<Integer> listenerPorts = new HashSet<>();
    private ServerSocket listenerSocket;
    private Thread receiveThread;


    public Client(int serverPort) {
        try {
            this.serverPort = serverPort;
            InetAddress ip = InetAddress.getByName("localhost");
            this.socket = new Socket(ip, serverPort);
            setUpConnections();
            createThreads();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void createThreads() {
        Thread receiveThread = new Thread(this::recieveMessage);
        this.receiveThread = receiveThread;
    }

    private void setUpConnections() throws IOException {
        MessageHandler.sendMessageOverSocket(socket, "HELLO");
        int randomSocket = generateRandomPort();
        listenerPorts.add(randomSocket);
        this.listenerSocket = new ServerSocket(randomSocket);
        MessageHandler.sendMessageOverSocket(socket, Integer.toString(randomSocket));
    }

    private int generateRandomPort() {
        int randomSocket = (int) Math.floor(Math.random() * 45000) + 10000;
        while (listenerPorts.contains(randomSocket)) {
            randomSocket = (int) Math.floor(Math.random() * 45000) + 10000;
        }
        return randomSocket;
    }

    private void run() {
        try {
            handleLogin();
            this.receiveThread.start();
            handleActiveUser();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private void recieveMessage() {
        while (true) {
            try {
                Socket messageReceiverSocket = listenerSocket.accept();
                String response = MessageHandler.receiveMessageOverSocket(messageReceiverSocket);
                if (response.equals("LGT")) {
                    System.out.println("You have been inactive for to long. Please log back in");
                    handleLogout();
                    return;
                }
                System.out.println(response);
            } catch (SocketException e) {
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleActiveUser() {
        try {
            while (true) {
                String response = promptCommand();
                String[] commands = response.split(" ", 3);
                switch (commands[0]) {
                    case "message":
                        if (commands.length < 3) {
                            System.out.println("Error usage: message <user> <message>");
                            break;
                        }
                        commands = response.split(" ", 3);
                        handleMessage(commands[1], commands[2]);
                        break;
                    case "broadcast":
                        if (commands.length < 2) {
                            System.out.println("Error usage: broadcast <message>");
                            break;
                        }
                        commands = response.split(" ", 2);
                        handleBroadcast(commands[1]);
                        break;
                    case "whoelse":
                        if (commands.length > 1) {
                            System.out.println("Error usage: whoelse");
                            break;
                        }
                        handleWhoelse();
                        break;
                    case "whoelsesince":
                        if (!(commands.length == 2)) {
                            System.out.println("Error usage: whoelsesince <time>");
                            break;
                        }
                        handleWhoelsesince(commands[1]);
                        break;
                    case "block":
                        if (!(commands.length == 2)) {
                            System.out.println("Error usage: block <user>");
                            break;
                        }
                        handleBlockUser(commands[1]);
                        break;
                    case "unblock":
                        if (!(commands.length == 2)) {
                            System.out.println("Error usage: unblock <user>");
                            break;
                        }
                        handleUnblockUser(commands[1]);
                        break;
                    case "help":
                        showValidCommands();
                        break;
                    case "logout":
                        handleLogout();
                        return;
                    default:
                        System.out.println("You entered an invalid command. Please type in 'help' if you need assistance");
                }
            }
            // Looking for SocketException
        } catch (Exception e) {
            return;
        }

    }

    private void handleLogout() {
        try {
            MessageHandler.sendMessageOverSocket(socket, "LOGOUT");
            this.socket.close();
            this.listenerSocket.close();
            System.out.println("Goodbye for now");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleBlockUser(String user) {
        MessageHandler.sendMessageOverSocket(socket, "BLOCK");
        MessageHandler.sendMessageOverSocket(socket, user);
        String response = MessageHandler.receiveMessageOverSocket(socket);
        switch (response) {
            case "USER NOT FOUND" -> {
                System.out.println("Error: User does not exist.");
                return;
            }
            case "DESTINATION USER IS SELF" -> {
                System.out.println("Error: You can not block yourself.");
                return;
            }
            case "USER IS ALREADY BLOCKED" -> {
                System.out.println("Error: User is already blocked");
                return;
            }
            default -> System.out.println(user + " is now blocked");
        }
    }

    private void handleUnblockUser(String user) {
        MessageHandler.sendMessageOverSocket(socket, "UNBLOCK");
        MessageHandler.sendMessageOverSocket(socket, user);
        String response = MessageHandler.receiveMessageOverSocket(socket);
        switch (response) {
            case "USER NOT FOUND" -> {
                System.out.println("Error: User does not exist.");
                return;
            }
            case "DESTINATION USER IS SELF" -> {
                System.out.println("Error: You can not unblock yourself.");
                return;
            }
            case "USER IS NOT BOCKED" -> {
                System.out.println("Error: User is already not blocked");
                return;
            }
            default -> System.out.println(user + " is now unblocked");
        }
    }

    private void handleWhoelsesince(String command) {
        try {
            int seconds = Integer.parseInt(command);
            if (seconds < 0) {
                System.out.println("Invalid number");
            }
            MessageHandler.sendMessageOverSocket(socket, "WHOELSESINCE");
            MessageHandler.sendMessageOverSocket(socket, command);
            String response = MessageHandler.receiveMessageOverSocket(socket);
            if (response.equals("NONE")) {
                System.out.println("No other users online in that time frame");
                return;
            }
            System.out.println(response);
        }
        catch (NumberFormatException e) {
            System.out.println("Invalid number. Please try again");
        }
    }

    private void showValidCommands() {
        System.out.println("Commands:");
        System.out.println("message <user> <message>");
        System.out.println("broadcast <message>");
        System.out.println("whoelse");
        System.out.println("whoelsesince <time>");
        System.out.println("block <user>");
        System.out.println("unblock <user>");
        System.out.println("logout");
    }

    private void handleWhoelse() {
        MessageHandler.sendMessageOverSocket(socket, "WHOELSE");
        String response = MessageHandler.receiveMessageOverSocket(socket);
        if (response.equals("NONE")) {
            System.out.println("No other users online");
            return;
        }
        System.out.println(response);
    }

    private void handleBroadcast(String messsage) {
        MessageHandler.sendMessageOverSocket(socket, "BROADCAST");
        MessageHandler.sendMessageOverSocket(socket, messsage);
        String response = MessageHandler.receiveMessageOverSocket(socket);
        if (response.equals("OK")) {
            System.out.println("Message sent to everyone");
        } else if (response.equals("MESSAGE ONLY SENT TO SOME USERS")) {
            System.out.println("Message was only sent to some users");
        } else {
            System.out.println("No one online. Message not sent");
        }
    }

    private void handleMessage(String destinationUser, String message) {
        MessageHandler.sendMessageOverSocket(socket, "MESSAGE");
        MessageHandler.sendMessageOverSocket(socket, destinationUser);
        MessageHandler.sendMessageOverSocket(socket, message);
        String response = MessageHandler.receiveMessageOverSocket(socket);
        switch (response) {
            case "USER NOT FOUND":
                System.out.println("Error: User does not exist.");
                return;
            case "DESTINATION USER IS SELF":
                System.out.println("Error: You can not send a message to yourself.");
                return;
            case "BLOCKED":
                System.out.println("Error: You can not send a message to this user.");
                return;
            default:
                System.out.println("Message sent");
        }
    }

    private String promptCommand() {
        Scanner scn = new Scanner(System.in);
        return scn.nextLine();
    }

    private void handleLogin() {
        String response = MessageHandler.receiveMessageOverSocket(socket);
        assert (response.equals("USERNAME"));
        MessageHandler.sendMessageOverSocket(socket, getUsername());
        response = MessageHandler.receiveMessageOverSocket(socket);
        if (response.equals("OK")) {
            if (promptPassword()) {
                diplaySuccessfulLogin();
            } else {
                response = MessageHandler.receiveMessageOverSocket(socket);
                System.out.println("Invalid Password. Your account has been blocked. Please try again after " + response + " seconds");
                System.exit(1);
            }
        } else if (response.equals("ALREADY LOGGED IN")) {
            // TODO: has not been tested
            System.out.println("User is already logged in. Please try again later");
            handleLogin();
        } else if (response.equals("USER IS BLOCKED")) {
            System.out.println("User is blocked from logging in. Please try again later");
            System.exit(1);
        } else { // New user
            System.out.println("Response is: " + response);
            assert(response.equals("NEW USER"));
            createNewUser();
        }
    }

    private void createNewUser() {
        System.out.println("Create a a password");
        MessageHandler.sendMessageOverSocket(socket, getPassword());
        String response = MessageHandler.receiveMessageOverSocket(socket);
        if (response.equals("OK")) {
            diplaySuccessfulLogin();
        }
    }


    private boolean promptPassword() {
        MessageHandler.sendMessageOverSocket(socket, getPassword());
        String response = MessageHandler.receiveMessageOverSocket(socket);
        while (!response.equals("MAX ATTEMPT")) {
            if (response.equals("OK")) {
                return true;
            }
            System.out.printf("Invalid Password. ");
            System.out.println("Please try again");
            MessageHandler.sendMessageOverSocket(socket, getPassword());
            response = MessageHandler.receiveMessageOverSocket(socket);
        }
        return false;
    }

    private void diplaySuccessfulLogin() {
        System.out.println("===== Welcome " +  "=====");
    }


    private String getUsername() {
        System.out.printf("Username: ");
        Scanner scn = new Scanner(System.in);
        return scn.nextLine();
    }

    private String getPassword() {
        System.out.printf("Password: ");
        Scanner scn = new Scanner(System.in);
        return scn.nextLine();
    }

    private void handleIncorrectPassword() {
        System.out.println("Incorrect password: Please enter your password again");
        MessageHandler.sendMessageOverSocket(socket, getPassword());
        String response = MessageHandler.receiveMessageOverSocket(socket);
        if (response.equals("Ok")) {
            diplaySuccessfulLogin();
        } else {
            // TODO: Handle 3rd incorrect password attempt
            System.out.println("Incorrect password: Please enter your password again");
        }
    }


    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println(args.length);
            System.out.println("===== Error usage: java Client server_port =====");
            return;
        }
        int serverPort = 0;
        try {
            serverPort = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("===== Error usage: java Client server_port =====");
        }
        Client client = new Client(serverPort);
        client.run();
    }
}