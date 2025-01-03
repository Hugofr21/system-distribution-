package assign.ring.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.BlockingDeque;
import java.util.logging.Logger;

/*
One of the threads in each peer generates requests for the server following a Poisson
distribution with an average frequency of 4 events per minute. Each request is also
random (both the operation and the arguments). These requests are placed in a local
queue.

    random_value = 0 between 1
    event = 4
    min = 60
    λ = 4 / 60.0
    delay_time = -ln(1 - random_value) / λ
*/

public class ServerHandle  implements Runnable {
    private Random rand;
    private final String HOST = "localhost";
    private static final double LAMBDA = 4.0 / 60.0;
    private BlockingDeque<String> requestQueue;

    String clientAddress;
    Socket clientSocket;
    BufferedReader in;
    PrintWriter out;
    Scanner scanner;

    private final Logger mLogger;
    private final MangerPeer mPeerManager;
    private static int numberName = 1;


    public ServerHandle(String clientAddress, Socket clientSocket, Logger plogger, MangerPeer pPeerManager) throws IOException {
        this.clientAddress = clientAddress;
        this.clientSocket  = clientSocket;
        this.rand = new Random();
        this.scanner = new Scanner(System.in);
        this.mPeerManager = pPeerManager;
        this.mLogger = plogger;
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    public double getPoissonDistribution(){
        return -Math.log((1.0 - rand.nextDouble()) / LAMBDA);
    }

    public static synchronized  int getNumberName() {
        return numberName;
    }

    @Override
    public void run() {
        try {

            String request;
            while ((request = in.readLine()) != null) {
                System.out.println("Client request from: " + clientAddress + " [ "  + request + " ] ");
                String result = processRequest(request);

                if (out != null && !clientSocket.isClosed())  {
                    out.println(result);
                    out.flush();
                }

                if ("quit".equalsIgnoreCase(request.trim())) {
                    System.out.println("Closing connection with client: " + clientAddress);
                    break;
                }
            }
        }catch (Exception e) {
            mLogger.warning("Connection error: " + e.getMessage());
        } finally {
            closeConnection();
        }

    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            mLogger.warning("Error closing connection: " + e.getMessage());
        }
    }

    private String processRequest(String request) throws IOException {
        String[] token = request.split(" ");

        if (token[0].equals("REGISTER")) {
            if (token.length != 4) {
                return "Invalid token holder format. Expected: REGISTER <peerHost> <peerPort> <neighbourPort>";
            }
            createdNetworkPeer(token);
        }

        if (token[0].equals("TOKEN_HOLDER")) {
            if (token.length != 2) {
                return "Invalid token holder format. Expected: TOKEN_HOLDER <peerID>";
            }
            int peerId = Integer.parseInt(token[1]);
            mLogger.info("Peer " + peerId + " is holding the token");
            return "Acknowledged token holder: Peer " + peerId;
        } else if (token[0].equals("Acknowledged token")) {
            mLogger.info("Acknowledged token");
        } else if (token[0].equals("UPDATE_TOKEN")) {

        }

        if (token.length != 3) {
            return "Invalid request format. Expected format: <command> <num1> <num2>";
        }

        return chooseEventToSendPeer(token);

    }

    private String createdNetworkPeer(String[] token) {
        int port = Integer.parseInt(token[2]);
        int neighbourPort = Integer.parseInt(token[3]);
        String name = "m" + getNumberName();
        PeerInfo peerInfo = new PeerInfo(name,port, HOST);
        PeerInfo neighbourInfo = new PeerInfo(name ,neighbourPort, HOST);

        mPeerManager.addPeerNetwork(peerInfo, neighbourInfo);

        List<PeerInfo> neighbours = mPeerManager.getPeers();
        System.out.println("Current neighbors of peer with ID " + peerInfo.getName() + ":");
        for (PeerInfo n : neighbours) {
            System.out.println("Neighbor ID: " + n.getName() + ", Host: " + n.getAddress() + ", Port: " + n.getPort());
        }

        mLogger.info("Peer registered: ID " + peerInfo.getName() + ", Host " + peerInfo.getAddress() + ", Port " + peerInfo.getAddress());

        if (mPeerManager.getPeers().size() == 1) {
            System.out.println("First peer registered, sending token to it.");
            sendTokenPeer();
        }
        return "Peer registered successfully: ID " + peerInfo.getName();
    }

    private String chooseEventToSendPeer(String[] token) {
        String command = token[0].toLowerCase();

        if (command.equals("PING")) {
            return "PONG";
        } else if (command.equals("ACK_TOKEN_HOLDER")){
            mLogger.info("Peer confirms that he received the token: port " +  mPeerManager.getCurrentPeerInfoToToken().getPort());
        }

        double n1, n2;
        n1 = Double.parseDouble(token[1]);
        n2 = Double.parseDouble(token[2]);

        if (token.length != 3) {
            return "Invalid request format. Expected format: <command> <num1> <num2>";
        }

        switch (command) {
            case "add":
                return "Result: " + (n1 + n2);

            case "sub":
                return "Result: " + (n1 - n2);

            case "mul":
                return "Result: " + (n1 * n2);

            case "div":
                if (n2 == 0) return "Error: Division by zero";
                return "Result: " + (n1 / n2);
            default:
                return "Invalid command";

        }
    }

    private void sendTokenPeer() {
        if (mPeerManager.getPeers().isEmpty()) {
            mLogger.warning("No peers found!");
            return;
        }

        PeerInfo sendToTokenPeer = mPeerManager.receivedTokenPeer();
        if (sendToTokenPeer == null) {
            mLogger.warning("There is no peer to connect!");
            return;
        }

        String token = "TOKEN_HOLDER " + sendToTokenPeer.getName();
        try {
            mLogger.info("Initializing token first peer: Host " + sendToTokenPeer.getAddress() + " Port " + sendToTokenPeer.getPort());
            out.println(token);
            out.flush();
            mLogger.info("Token first peer initialized: " + sendToTokenPeer.getName());
        } catch (Exception e) {
            mLogger.severe("Error initializing token first peer: " + e.getMessage());
        }
    }

    private Boolean canReceiveToken(PeerInfo currentPeerInfo) {
        PeerInfo nextPeer = mPeerManager.getPairPeers().get(currentPeerInfo);
        return nextPeer != null && mPeerManager.getCurrentPeerInfoToToken().equals(currentPeerInfo);
    }


}
