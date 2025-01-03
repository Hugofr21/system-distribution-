package assign.tom.chat.lamport;

public class Message implements Comparable<Message> {
    private final String sender;
    private final String messageType;
    private final int timestamp;
    private final  String content;
    private final String senderId;

    public Message(String sender, String message, int timestamp, String content, String senderId) {
        this.sender = sender;
        this.messageType = message;
        this.timestamp = timestamp;
        this.content = content;
        this.senderId = senderId;
    }


    public String getSender() {
        return sender;
    }
    public String getMessageType() {
        return messageType;
    }
    public int getTimestamp() {
        return timestamp;
    }
    public String getContent() {
        return content;
    }
    public String getSenderId() {return senderId;}

    @Override
    public int compareTo(Message o) {
        if (this.timestamp != o.timestamp) {
            return Integer.compare(this.timestamp, o.timestamp);
        }
        return this.sender.compareTo(o.sender);
    }

    public String serialize() {
        return String.format("%s|%s|%d|%s|%S", sender, messageType, timestamp, content, senderId);
    }

    public static Message deserialize(String message) {
        String[] lines = message.split("\\|");
        System.out.println("deserialize: " + message);
        if (lines.length != 5) {
            throw new IllegalArgumentException("Invalid message format deserialize!");
        }
        return new Message(
                lines[0],
                lines[1],
                (int)Long.parseLong(lines[2]) % Integer.MAX_VALUE,
                lines[3],
                lines[4]);
    }
}
