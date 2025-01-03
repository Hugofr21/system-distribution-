package assign.ring.peers;


import assign.ring.server.PeerInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class ConnectionPeer implements Runnable{
    private final Socket   serverSocket;
    private final PeerInfo mPeerInfo;
    private final PeerInfo nextPeerInfo;
    private final Logger mLogger;
    private final AtomicBoolean running;
    private              boolean hasToken;
    public BufferedReader socketReader;
    public  PrintWriter writer;
    private final PeerM mPeerM;

    public ConnectionPeer(PeerInfo myPeerInfo, PeerInfo nextPeerInfo, Socket pSocket, boolean pHasToken, Logger pLogger, PeerM peerM ) throws IOException {
        this.mPeerInfo = myPeerInfo;
        this.nextPeerInfo = nextPeerInfo;
        this.serverSocket = pSocket;
        this.running = new AtomicBoolean(true);
        this.hasToken = pHasToken;
        this.mLogger = pLogger;
        this.socketReader = new BufferedReader(new InputStreamReader(pSocket.getInputStream()));
        this.writer = new PrintWriter(pSocket.getOutputStream(), true);
        this.mPeerM = peerM;

    }



    @Override
    public void run() {
        running.set(true);
        sendPingToNeighbor();
        while (running.get()){
            handleConnections();
        }
    }


    private void handleConnections(){
        try {
            String message = socketReader.readLine().trim();
            System.out.println("Received message" + message);

            if (message.startsWith("TOKEN_HOLDER")){
                receiveToken();
            } else if (message.startsWith("DISCONNECTED")) {
                stop();
            } else if (message.startsWith("PING")){
                handlePing();
            } else if (message.startsWith("PONG")){
                mLogger.info("Received PONG from " + nextPeerInfo.getName());
            }
        }catch (IOException pE){
            mLogger.warning("Error while handling incoming connection: " + pE);
        }
    }

    private void handlePing(){
        try {
            writer.println("PONG");
            writer.flush();
            mLogger.info("Received with PONG to peer " + mPeerInfo.getName());
        }catch (Exception pE){
            mLogger.warning("Error while handling incoming connection: " + pE);
        }
    }

    public void receiveToken() {
        hasToken = true;
        mLogger.info("Peer " + mPeerInfo + " received the token.");
        writer.println("ACK_TOKEN_HOLDER.");
        Thread processingThread = new Thread(() -> {
            mPeerM.processRequest();
        });
        processingThread.start();

    }

    private void sendPingToNeighbor() {
        try {
            writer.println("PING");
            writer.flush();
            mLogger.info("Sent PING to " + nextPeerInfo.getName());
        } catch (Exception e) {
            mLogger.warning("Error sending PING to neighbor: " + e.getMessage());
        }
    }

    public void stop() {
        running.set(false);
        try {
            synchronized (this) {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }

                if (writer != null) {
                    writer.close();
                }

                if (socketReader != null) {
                    socketReader.close();
                }
            }
        } catch (IOException e) {
            mLogger.warning("Error closing server socket: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
