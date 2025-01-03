package assign.tom.chat.network;


import assign.tom.chat.lamport.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Listen implements Runnable {

    private final Socket         socket;
    public BufferedReader in;
    public PrintWriter    out;
    public final String      userName ;
    private volatile boolean running;
    private PeerChat mPeerChat;
    private final TotallyOrderedMulticast multicast;

    public Listen(Socket socket, PeerChat pPeerChat, TotallyOrderedMulticast pTotallyOrderedMulticast)  throws IOException {
        this.socket = socket;
        this.mPeerChat = pPeerChat;
        this.userName = pPeerChat.getNAMED();
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.multicast = pTotallyOrderedMulticast;
    }

    public PrintWriter getOutPrintWriter() {
        return out;
    }

    public BufferedReader getInBufferedReader() {
        return in;
    }

    @Override
    public void run() {
      try {
          running = true;
          while (running) {
              String message = in.readLine();

              if (message == null) break;

              try {
                  Message deserializedMessage = multicast.receiveMessage(message);
                  //System.out.println("User: [ " + userName + "]");
                  //System.out.println("Raw message: " + deserializedMessage + "\n");
                  handleMessage(deserializedMessage);
              } catch (IllegalArgumentException e) {
                  System.out.println("Error processing message: " + e.getMessage());
              }

          }
      } catch (IOException pE) {
          throw new RuntimeException(pE);
      } finally {
          try {
              socket.close();
          } catch (IOException pE) {
              throw new RuntimeException(pE);
          }
      }
    }

    private void handleMessage(Message message) {
        System.out.println("Message type: [ " + message.getMessageType() + "]");
        switch (message.getMessageType()) {
            case "MULTICAST":
                System.out.println("Multicast received: " + message.getContent() + " who " + message.getSender());
                handleBroadcast(message);
                break;
            case "UNICAST":
                System.out.println("Unicast received: " + message.getContent() + " who " + message.getSender());
                break;
            case "ACK":
                System.out.println("ACK received: " + message.getContent() + " who [" + message.getSenderId() + "]");
                break;
            case "MULTICAST_WORD_LIST":
                System.out.println("Word received: " + message.getContent() + " who [" + message.getSenderId() + "]");
                handleUpdateWordList(message.getContent(), message.getTimestamp());
                break;
            default:
                System.out.println("[ERROR] Processing message: " + message);
                break;


        }

    }

    private void handleUpdateWordList(String content, int timestamp) {
        multicast.mClockLamport.setTime(Math.max(multicast.mClockLamport.getTime(), timestamp));
        multicast.mClockLamport.inc();

        List<String> wordsUpdate = Arrays.asList(content.split(" "));

        if (mPeerChat.words == null){
            mPeerChat.words = new ArrayList<>();
        }else {
            mPeerChat.words.clear();
        }

        mPeerChat.words.addAll(wordsUpdate);

        wordsUpdate.forEach(word -> {
            if (!mPeerChat.words.contains(word)) {
                mPeerChat.words.add(word);
            }else {
                System.out.println("[DETAILS] Duplicate word: " + word);

            }
        });
        System.out.println("[INFO] Words: " + mPeerChat.words.size());

        Message ack = new Message(
                userName,
                "ACK",
                multicast.mClockLamport.getTime(),
                "MULTICAST_ACK_LIST",
                userName
        );
        out.println(ack.serialize());
        out.flush();

    }

    private void handleBroadcast(Message message) {
        Message ack = new Message(
                userName, 
                "ACK",
                message.getTimestamp(),
                message.getContent(),
                userName
        );
        out.println(ack.serialize());
        out.flush();

    }
}
