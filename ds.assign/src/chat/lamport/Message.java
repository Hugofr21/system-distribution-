package chat.lamport;

public class Message implements Comparable<Message> {
    private String sender;
    private String message;
    private int timestamp;
    private String content;

    public Message(String sender, String message, int timestamp, String content) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
        this.content = content;
    }
    public String getSender() {
        return sender;
    }
    public String getMessage() {
        return message;
    }
    public int getTimestamp() {
        return timestamp;
    }
    public String getContent() {
        return content;
    }

    @Override
    public int compareTo(Message o) {
        if (this.timestamp != o.timestamp) {
            return Integer.compare(this.timestamp, o.timestamp);
        }
        return this.sender.compareTo(o.sender);
    }
}
