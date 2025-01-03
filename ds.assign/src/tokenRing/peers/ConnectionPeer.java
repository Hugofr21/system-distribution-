package tokenRing.peers;

import tokenRing.server.PeerInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.logging.Logger;

public class ConnectionPeer implements Runnable{
    private Socket   serverSocket;
    private PeerInfo mPeerInfo;
    private PeerInfo nextPeerInfo;
    Logger mLogger;
    private              boolean running = false;
    private              boolean hasToken;
    private BufferedReader reader;

    public ConnectionPeer(PeerInfo myPeerInfo,  PeerInfo nextPeerInfo, Socket pSocket, boolean pHasToken, Logger pLogger) {
        this.mPeerInfo = myPeerInfo;
        this.nextPeerInfo = nextPeerInfo;
        this.serverSocket = pSocket;
        this.hasToken = pHasToken;
        this.mLogger = pLogger;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void run() {
        running = true;
        while (running){
            handleConnections();
        }
    }

    private void handleConnections(){
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String message = reader.readLine().trim();

            if (message.startsWith("TOKEN_HOLDER")){
                receiveToken();
            } else if (message.startsWith("DISCONNECTED")) {

            }
        }catch (IOException pE){
            mLogger.warning("Error while handling incoming connection: " + pE);
        }
    }

    public String receiveToken() {
        this.hasToken = true;
        mLogger.info("Peer " + mPeerInfo + " received the token.");
        return "Acknowledged token holder.";
    }
}
