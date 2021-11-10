
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Profile {
    private final String username;
    private final String password;
    private boolean loggedIn;
    private final ArrayList<String> awaitingMessagesInbound = new ArrayList<>();
    private int listenerPort;
    private final Map<String, Profile> blockedUsersList = new HashMap<>();
    private LocalDateTime lastLoginDate;
    private boolean isLoginBlocked = false; // TODO: block user and get timer
    private LocalDateTime loginBlockedUntil = null;


    public Profile(String username, String password) {
        this.username = username;
        this.password = password;
        this.loggedIn = false;
    }

    public void bindListenerPort(int listenerPort) {
        this.listenerPort = listenerPort;
    }

    public boolean isValidPassword(String passwordAttempt) {
        return password.equals(passwordAttempt);
    }

    public void blockUser(String username, Profile profile) {
        blockedUsersList.put(username, profile);
    }

    public void unblockUser(String username) {
        blockedUsersList.remove(username);
    }

    public boolean isBlocking(String username) {
        return blockedUsersList.containsKey(username);
    }

    public String getUsername() {
        return username;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void login() {
        this.loggedIn = true;
        recieveAllAwaitingMessages();
        lastLoginDate = LocalDateTime.now();
    }

    public void logout() {
        this.loggedIn = false;
    }

    public void recieveMessage(String message) {
        if (isLoggedIn()) {
            try {
                Socket listenerSocket = establishConnection();
                MessageHandler.sendMessageOverSocket(listenerSocket, message);
                listenerSocket.close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        awaitingMessagesInbound.add(message);
    }

    private Socket establishConnection() throws IOException {
        InetAddress ip = InetAddress.getByName("localhost");
        return new Socket(ip, listenerPort);
    }


    private void recieveAllAwaitingMessages() {
        if (awaitingMessagesInbound.isEmpty()) {
            return;
        }
        for (String message : awaitingMessagesInbound) {
            recieveMessage(message);
        }
    }

    public boolean loggedInWithinTimeframe(LocalDateTime minBound) {
        if (this.lastLoginDate == null) {
            return false;
        }
        return lastLoginDate.isAfter(minBound);
    }

    public void blockLogin(int seconds) {
        this.isLoginBlocked = true;
        this.loginBlockedUntil = LocalDateTime.now().plusSeconds(seconds);
    }

    private boolean isLoginStillBlocked() {
        if (this.loginBlockedUntil == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(this.loginBlockedUntil)) {
            this.loginBlockedUntil = null;
            this.isLoginBlocked = false;
            return false;
        }
        return true;
    }

    public boolean getIsLoginBlocked() {
        if (!this.isLoginBlocked) {
            return false;
        }
        return isLoginStillBlocked();
    }
}
