package assign.tom.chat.network;

import assign.tom.chat.lamport.ClockLamport;
import assign.tom.chat.lamport.Message;
import assign.tom.chat.user.UserInfo;

import java.io.PrintWriter;
import java.net.Socket;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

public class TotallyOrderedMulticast {
    private int nextTimestamp;
    public final ClockLamport mClockLamport;
    private final PriorityBlockingQueue<Message> messagesQueue;
    private PeerChat mPeerChat;
    private final Map<String, Socket> activesSockets;
    private final Map<String, Integer> acknowledgments;
    private final String peerId;

    public TotallyOrderedMulticast(String peerId, PeerChat peerChat) {
        this.mClockLamport = new ClockLamport(0);
        this.messagesQueue = new PriorityBlockingQueue<Message>(11, (m1, m2) -> {
            int tmpCompare = Integer.compare(m1.getTimestamp(), m2.getTimestamp());
            if (tmpCompare != 0) return tmpCompare;
            return m1.getSenderId().compareTo(m2.getSenderId());
        });
        this.mPeerChat = peerChat;
        this.peerId = peerId;
        this.activesSockets = peerChat.getConnections();
        this.acknowledgments = new HashMap<>();
    }

    public synchronized Message sendMessage(String content, boolean isMulticast , String senderId) {
        mClockLamport.inc();
        int timestamp = mClockLamport.getTime();
        String messageType = isMulticast ? "MULTICAST" : "UNICAST";
        Message message = new Message(senderId, messageType, timestamp, content, senderId);
        messagesQueue.add(message);
        acknowledgments.put(message.getSenderId(), 0);
        return message;
    }

    public void sendMessageToClient(Listen listen, UserInfo pUserInfo, String content, boolean isMulticast) {
        PrintWriter out = listen.getOutPrintWriter();
        if (out != null) {
            //int time = (int)System.currentTimeMillis() % Integer.MAX_VALUE;
            Message msg = sendMessage(content, isMulticast, pUserInfo.getName());
            out.println(msg.serialize());
            out.flush();
            System.out.println("[SUCCESS] Message sent to: " + pUserInfo.getName());
        }
        else {
            System.out.println("[ERROR] Could not get PrintWriter for: " + pUserInfo.getName());
        }
    }


    public synchronized void sendGlobalOrderWordsList(UserInfo name, Listen listen){
        PrintWriter out = listen.getOutPrintWriter();
        if (out != null && mPeerChat.words != null) {
                String wordsListContents = String.join(" ", mPeerChat.words);
                mClockLamport.inc();

                Message msgListWords = new Message(
                        name.getName(),
                        "MULTICAST_WORD_LIST",
                        mClockLamport.getTime(),
                        wordsListContents,
                        name.getName()
                );

                messagesQueue.add(msgListWords);
                out.println(msgListWords.serialize());
                out.flush();

                System.out.println("[SUCCESS] Message sent to: " + name.getName());
        }else {
            System.out.println("[ERROR] Could not get PrintWriter for: " + name.getName());
        }
    }

    public Message receiveMessage(String receivedMessage) {
        Message deserializedMessage = Message.deserialize(receivedMessage);
        mClockLamport.setTime(deserializedMessage.getTimestamp());
        messagesQueue.add(deserializedMessage);
        return deserializedMessage;
    }

}
