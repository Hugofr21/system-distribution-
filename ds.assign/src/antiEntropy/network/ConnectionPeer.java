package antiEntropy.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

public class ConnectionPeer implements Runnable {
    private Socket socket;
    private Logger mLogger;
    private Peer mPeer;
    private PrintWriter out;
    private BufferedReader in;
    private boolean running = false;

    public ConnectionPeer(Socket socket, Logger pLogger, Peer peer) {
        this.mPeer = peer;
        this.socket = socket;
        this.mLogger = pLogger;
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String peerInfo = mPeer.getName() + " Port: " + mPeer.getPort() + " Host: " + Peer.HOST;
                out.println(peerInfo);
                System.out.println("Sent peer info: " + peerInfo);

                sendMessage("PING");

                String line;
                while ((line = in.readLine()) != null) {
                    processSend(line);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                clear();
            }
        }

    }

    private void processSend(String line) {
        mLogger.info("Message process: " + line);
        String[] parts = line.split(" ");
        String command = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "PING":
                sendMessage("PONG " + mPeer.getPort());
                break;
            case "PONG":
                mLogger.info("Connected to ");
                case "WORD":
                    break;
                default:
                    mLogger.info("Unknown command: " + command);
        }
        System.out.println("Received peer info: " + line);
        mLogger.info("Received peer info: " + line);

        if (line.startsWith("PONG")) {
            System.out.println("Received PONG from: " + line);
        } else {
            System.out.println("Unknown message received: " + line);
        }

    }

    private void sendMessage(String message) {
        out.println(message);
        System.out.println("Sent: " + message);
        mLogger.info("Sent: " + message);
    }

    private void clear(){
        running = false;
        try {

            if (socket != null && !socket.isClosed()) socket.close();
            if (out != null) out.close();
            if (in != null) in.close();
        } catch (IOException e) {
            mLogger.warning("Error closing connection: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }



}
