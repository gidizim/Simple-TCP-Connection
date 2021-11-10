import java.util.Date;

public class Message {
    public final String message;
    public final String sender;
    public final String receiver;
    public final String dateSent;

    public Message(String message, String sender, String receiver) {
        this.message = message;
        this.sender = sender;
        this.receiver = receiver;
        this.dateSent = new Date().toString();
    }

    @Override
    public String toString() {
        return String.format("%s from %s: %s", this.dateSent, this.sender, this.message);
    }
}
