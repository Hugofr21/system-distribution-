package assign.p2p.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Logger;

public class ConnectionPeer implements Runnable {
    private final Socket socket;
    private final Logger mLogger;
    private final Peer mPeer;
    private final PrintWriter out;
    private final BufferedReader in;
    private volatile boolean running = false;

    public ConnectionPeer(Socket socket, Logger logger, Peer peer) throws IOException {
        this.mPeer = peer;
        this.socket = socket;
        this.mLogger = logger;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public PrintWriter getPrintWriter(){
        return out;
    }

    @Override
    public void run() {
        running = true;
        try {
            handshake();
            listenForMessages();
        } catch (IOException e) {
            mLogger.warning("Error in connection: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void handshake() throws IOException {
        String peerInfo = mPeer.getName() + " Port: " + mPeer.getPort() + " Host: " + Peer.HOST;
        sendMessage(peerInfo);
        mLogger.info("Sent peer info during handshake: " + peerInfo);

        String registerMessage = "REGISTER " + mPeer.getName() + " " + mPeer.getPort() + " " + Peer.HOST;
        sendMessage(registerMessage);
        mLogger.info("Sent REGISTER message: " + registerMessage);
    }

    private void listenForMessages() throws IOException {
        String message;
        while (running && (message = in.readLine()) != null) {
            processMessage(message);
        }
    }

    private void processMessage(String message) {
        mLogger.info("Processing message: " + message);

        String[] parts = message.split(" ");
        String command = parts[0];
        String[] args = parts.length > 1 ? message.substring(command.length()).trim().split(" ") : new String[0];

        switch (command) {
            case "PING":
                handlePing();
                break;

            case "PONG":
                handlePong(args);
                break;

            case "REGISTER", "REGISTER_NEIGHBOUR":
                handleRegisterAndUpdateMapNeighbour(args);
                break;
            case "ACTIVE_NEIGHBOUR":
                handleCheckNeighborActivityStatus(args);
            default:
                mLogger.warning("Unknown command received: " + command);
                break;
        }
    }

    private void handleCheckNeighborActivityStatus(String[] args) {
        if (args.length >= 1) {
            int port = Integer.parseInt(args[0]);
            sendMessage("ACK_ACTIVE_NEIGHBOUR " + mPeer.getPort());
            Map<String, PeerInfo> peers = mPeer.neighbour.getNeighbours();
            if (peers.containsKey(String.valueOf(port))) {
                PeerInfo peer = peers.get(String.valueOf(port));
                peer.setLastUpdated(System.currentTimeMillis());
                System.out.println("Acknowledged [ACTIVE_NEIGHBOUR] from port: " + peer.toString());
            }
        }
    }


    private void handlePing() {
        sendMessage("PONG " + mPeer.getPort());
        mLogger.info("Responded to PING with PONG");
    }

    private void handlePong(String[] args) {
        mLogger.info("Received PONG response: " + String.join(" ", args));
    }

    private void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            mLogger.info("Sent: " + message);
        } else {
            mLogger.warning("Attempted to send message, but output stream is null.");
        }
    }



    private void handleRegisterAndUpdateMapNeighbour(String[] args) {
        if (args.length >= 3){
            String neighbourName = args[0];
            int neighbourPort = Integer.parseInt(args[1]);
            // String neighbourHost = args[2];

            Map<String,PeerInfo> peers = mPeer.neighbour.getNeighbours();
            if (peers != null){
                for (PeerInfo peer: peers.values()){
                    if (peer.getPort() == neighbourPort){
                        mLogger.info("Port " + neighbourPort + " already exists.");
                        peer.setLastUpdated(System.currentTimeMillis());
                    }
                }
            }
            mPeer.addNeighbour(neighbourName, neighbourPort);
            mLogger.info("Added neighbour " + neighbourName + " to the peer list.");
        }
    }



    private void cleanup() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                mLogger.info("Socket closed.");
            }
            if (out != null) {
                out.close();
                mLogger.info("Output stream closed.");
            }
            if (in != null) {
                in.close();
                mLogger.info("Input stream closed.");
            }
        } catch (IOException e) {
            mLogger.warning("Error during cleanup: " + e.getMessage());
        }
    }
}
