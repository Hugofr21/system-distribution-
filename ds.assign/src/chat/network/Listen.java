package chat.network;

import antiEntropy.network.Peer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Listen implements Runnable {

    private Socket         socket;
    private BufferedReader in;
    private PrintWriter    out;
    private String      userName ;
    private PeerChat mPeerChat;

    public Listen(Socket socket, String userName, PeerChat pPeerChat)  throws IOException {
        this.socket = socket;
        this.userName = userName;
        mPeerChat = pPeerChat;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run() {
      try {
          String line;
          while ((line = in.readLine()) != null){
              System.out.println("User: " + userName);
              System.out.println("\n" + line);
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
}
