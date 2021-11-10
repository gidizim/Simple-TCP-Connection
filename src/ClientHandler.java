import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Map;

public class ClientHandler extends Thread {
    final Socket clientSocket;
    private final int clientListenerPort;
    private final Map<String, Profile> userProfilesMap;
    private Profile clientProfile;
    private final Thread timeoutThread;
    private final int timeout;
    private final int blockDuration;
    private static final int MAX_PASSWORD_ATTEMPT = 3;


    public ClientHandler(Socket clientSocket, int clientListenerPort, Map<String, Profile> userProfilesMap, int timeout, int blockDuration) {
        this.clientSocket = clientSocket;
        this.clientListenerPort = clientListenerPort;
        this.userProfilesMap = userProfilesMap;
        this.timeout = timeout;
        this.timeoutThread = new Thread(this::setTimeoutThread);
        this.blockDuration = blockDuration;
    }

    public void setClientProfile(Profile clientProfile) {
        this.clientProfile = clientProfile;
        clientProfile.bindListenerPort(clientListenerPort);
    }

    @Override
    public void run() {
        handleLogin();
        this.timeoutThread.start();
        handleActiveUser();
        handleLogout();
    }

    private void handleLogout() {
        try {
            clientProfile.logout();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleActiveUser() {
        while(true) {
            String response = MessageHandler.receiveMessageOverSocket(clientSocket);
            this.timeoutThread.interrupt();
            switch (response) {
                case "MESSAGE":
                    message();
                    break;
                case "BROADCAST":
                    broadcast();
                    break;
                case "WHOELSE":
                    whoelse();
                    break;
                case "WHOELSESINCE":
                    whoelsesince();
                    break;
                case "BLOCK":
                    blockUser();
                    break;
                case "UNBLOCK":
                    unblockUser();
                    break;
                case "LOGOUT":
                    return;
                default: // This will be invalid command
                    System.out.println("User entered an invalid command. Please try again");
            }
        }
    }

    private void blockUser() {
        String user = MessageHandler.receiveMessageOverSocket(clientSocket);
        synchronized (userProfilesMap) {
            if (!userProfilesMap.containsKey(user)) {
                MessageHandler.sendMessageOverSocket(clientSocket, "USER NOT FOUND");
                return;
            }
            if (clientProfile.getUsername().equals(user)) {
                MessageHandler.sendMessageOverSocket(clientSocket, "DESTINATION USER IS SELF");
                return;
            }
            if (clientProfile.isBlocking(user)) {
                MessageHandler.sendMessageOverSocket(clientSocket, "USER IS ALREADY BLOCKED");
                return;
            }
            clientProfile.blockUser(user, userProfilesMap.get(user));
            MessageHandler.sendMessageOverSocket(clientSocket, "OK");
        }
    }

    private void unblockUser() {
        String user = MessageHandler.receiveMessageOverSocket(clientSocket);
        synchronized (userProfilesMap) {
            if (!userProfilesMap.containsKey(user)) {
                MessageHandler.sendMessageOverSocket(clientSocket, "USER NOT FOUND");
                return;
            }
            if (clientProfile.getUsername().equals(user)) {
                MessageHandler.sendMessageOverSocket(clientSocket, "DESTINATION USER IS SELF");
                return;
            }
            if (!clientProfile.isBlocking(user)) {
                MessageHandler.sendMessageOverSocket(clientSocket, "USER IS NOT BOCKED");
                return;
            }
            clientProfile.unblockUser(user);
            MessageHandler.sendMessageOverSocket(clientSocket, "OK");
        }
    }

    private void whoelsesince() {
        String response = MessageHandler.receiveMessageOverSocket(clientSocket);
        int seconds = Integer.parseInt(response);
        String usersOnline = getAllUsersOnlineWithinTimeframe(seconds);
        if (usersOnline.isBlank()) {
            MessageHandler.sendMessageOverSocket(clientSocket, "NONE");
        }
        MessageHandler.sendMessageOverSocket(clientSocket, usersOnline);
    }

    private void whoelse() {
        String usersOnline = getAllUsersOnline();
        if (usersOnline.isBlank()) {
            MessageHandler.sendMessageOverSocket(clientSocket, "NONE");
            return;
        }
        MessageHandler.sendMessageOverSocket(clientSocket, usersOnline);
    }

    private void broadcast() {
        String message = MessageHandler.receiveMessageOverSocket(clientSocket);

        boolean isMessageSentToEveryone = true;
        boolean isMessageSentToAtLeastOneUser = false;
        synchronized (userProfilesMap) {
            for (String destUsername : userProfilesMap.keySet()) {
                if (canBroadcastBeSentToUser(destUsername)) {
                    userProfilesMap.get(destUsername).recieveMessage(new Message(message, clientProfile.getUsername(), destUsername).toString());
                    isMessageSentToAtLeastOneUser = true;
                } else {
                    isMessageSentToEveryone = false;
                }
            }
        }
        if (isMessageSentToEveryone) {
            MessageHandler.sendMessageOverSocket(clientSocket, "OK");
        } else if (isMessageSentToAtLeastOneUser) {
            MessageHandler.sendMessageOverSocket(clientSocket, "MESSAGE ONLY SENT TO SOME USERS");
        } else {
            MessageHandler.sendMessageOverSocket(clientSocket, "MESSAGE NOT SENT");
        }
    }

    private boolean canBroadcastBeSentToUser (String destUsername) {
        synchronized (userProfilesMap) {
            Profile destProfile = userProfilesMap.get(destUsername);
            boolean isUserSelf = destUsername.equals(clientProfile.getUsername());
            boolean isSenderBeingBlocked = destProfile.isBlocking(clientProfile.getUsername());
            boolean isDestUserLoggedIn = destProfile.isLoggedIn();
            return (!isUserSelf) && (!isSenderBeingBlocked) && (isDestUserLoggedIn);
        }
    }

    private void message() {
        String destinationUser = MessageHandler.receiveMessageOverSocket(clientSocket);
        String message = MessageHandler.receiveMessageOverSocket(clientSocket);

        if (!userProfilesMap.containsKey(destinationUser)) {
            MessageHandler.sendMessageOverSocket(clientSocket, "USER NOT FOUND");
            return;
        } else if (clientProfile.getUsername().equals(destinationUser)) {
            MessageHandler.sendMessageOverSocket(clientSocket, "DESTINATION USER IS SELF");
            return;
        }

        // Check if destination user is blocking source user
        Profile destinationUserProfile = userProfilesMap.get(destinationUser);
        if (destinationUserProfile.isBlocking(clientProfile.getUsername())) {
            MessageHandler.sendMessageOverSocket(clientSocket, "BLOCKED");
            return;
        }

        MessageHandler.sendMessageOverSocket(clientSocket, "OK");
        sendMessage(new Message(message, clientProfile.getUsername(), destinationUser));
    }

    private void sendMessage(Message message) {
        userProfilesMap.get(message.receiver).recieveMessage(message.toString());
    }

    public void handleLogin() {
        // Get username from client
        MessageHandler.sendMessageOverSocket(clientSocket, "USERNAME");
        String username = MessageHandler.receiveMessageOverSocket(clientSocket);
        if (isValidUsername(username)) {
            if (isUserLoggedIn(username)) {
                MessageHandler.sendMessageOverSocket(clientSocket, "ALREADY LOGGED IN");
                handleLogin();
            }
            // Is user blocked from loggin in
            if (isUserPreventedFromLoggingIn(userProfilesMap.get(username))) {
                MessageHandler.sendMessageOverSocket(clientSocket, "USER IS BLOCKED");
                return;
            }
            MessageHandler.sendMessageOverSocket(clientSocket, "OK");
            int count = 0;
            while (count < MAX_PASSWORD_ATTEMPT) {
                if (authenticatePassword(username)) {
                    break;
                }
                count++;
                if (count < MAX_PASSWORD_ATTEMPT) {
                    MessageHandler.sendMessageOverSocket(clientSocket, "Fail");
                }
            }
            if (count < MAX_PASSWORD_ATTEMPT) {
                userProfilesMap.get(username).login();
                setClientProfile(userProfilesMap.get(username));
            } else {
                userProfilesMap.get(username).blockLogin(this.blockDuration);
                MessageHandler.sendMessageOverSocket(clientSocket, "MAX ATTEMPT");
                MessageHandler.sendMessageOverSocket(clientSocket, Integer.toString(this.timeout));

                System.out.println("User has reached max password attempts");
            }
        } else {
            createNewUser(username);
        }
        System.out.println("New Login: " + username + " is now logged in");
    }

    private boolean isUserPreventedFromLoggingIn(Profile profile) {
        return profile.getIsLoginBlocked();
    }

    private synchronized boolean isUserLoggedIn(String username) {
        return userProfilesMap.get(username).isLoggedIn();
    }

    private void createNewUser(String username) {
        MessageHandler.sendMessageOverSocket(clientSocket, "NEW USER");
        String password = MessageHandler.receiveMessageOverSocket(clientSocket);
        Profile newUser = new Profile(username, password);
        userProfilesMap.put(username, newUser);
        newUser.login();
        setClientProfile(newUser);
        MessageHandler.sendMessageOverSocket(clientSocket, "OK");
    }

    private boolean authenticatePassword(String username) {
        String response = MessageHandler.receiveMessageOverSocket(clientSocket);
        if (checkValidPassword(username, response)) {
            MessageHandler.sendMessageOverSocket(clientSocket, "Ok");
            return true;
        }
        return false;
    }

    private synchronized boolean isValidUsername(String username) {
        return userProfilesMap.containsKey(username);
    }

    private synchronized boolean checkValidPassword(String username, String passwordAttempt) {
        Profile profile = userProfilesMap.get(username);
        return profile.isValidPassword(passwordAttempt);

    }

    private synchronized String getAllUsersOnline() {
        StringBuilder usersOnline = new StringBuilder();
        for (String destUsername : userProfilesMap.keySet()) {
            if ((!destUsername.equals(clientProfile.getUsername())) && userProfilesMap.get(destUsername).isLoggedIn()) {
                usersOnline.append(destUsername).append(System.lineSeparator());
            }
        }
        return usersOnline.toString();
    }

    private String getAllUsersOnlineWithinTimeframe(int seconds) {
        StringBuilder usersOnlineWithin = new StringBuilder();
        LocalDateTime minBound = LocalDateTime.now().minusSeconds(seconds);
        synchronized(userProfilesMap) {
            for (String destUsername : userProfilesMap.keySet()) {
                if ((!destUsername.equals(clientProfile.getUsername())) && userProfilesMap.get(destUsername).loggedInWithinTimeframe(minBound)) {
                    usersOnlineWithin.append(destUsername).append(System.lineSeparator());
                }
            }
        }
        return usersOnlineWithin.toString();
    }

    private void setTimeoutThread() {
        while(true) {
            try {
                Thread.sleep(this.timeout);
                this.clientProfile.recieveMessage("LGT");
                return;
            } catch (InterruptedException e) {
                System.out.println("Interupt");
            }
        }
    }

}
