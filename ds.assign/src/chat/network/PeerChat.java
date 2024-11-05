package chat.network;

import chat.lamport.ClockLamport;
import chat.network.user.UserInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class PeerChat {
    private ServerSocket serverSocket;
    private final String SERVER_PEER = "localhost";
    private int SERVER_PORT;
    Logger mLogger;
    Scanner mScanner;
    private final UserInfo mUserInfo;
    private Map<String, Socket> connections = new HashMap<>();
    private ClockLamport mClockLamport;
    private PriorityBlockingQueue messagesQueue = new PriorityBlockingQueue();

    public PeerChat(String username, int port) {
        this.mUserInfo = new UserInfo(username);
        this.SERVER_PORT = port;
        this.mScanner = new Scanner(System.in);
        mLogger = Logger.getLogger("logfile");
        try {
            FileHandler handler = new FileHandler("./" + SERVER_PORT + "_" + SERVER_PEER + "_peer.log", true);
            mLogger.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void start() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Peer started on port " + SERVER_PORT);
            new Thread(this::acceptConnections).start();

            while (true) {
                System.out.println("Enter the peer's name to connect (or 'quit' to close): ");
                String peerName = mScanner.nextLine();

                if (peerName.equalsIgnoreCase("quit")) {
                    break;
                }

                System.out.println("Enter the port of the peer you want to connect to: ");
                int peerPort = Integer.parseInt(mScanner.nextLine());

                if (!connections.containsKey(peerName)) {
                    connectionToPeer(peerName, peerPort);
                }

                if (connections.containsKey(peerName)) {
                    chatWithPeer(peerName);
                }

//                System.out.println("Type the message to " + peerName + ": ");
//                String message = mScanner.nextLine();
//                sendMessage(peerName, message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeAllConnections();
        }
    }

    private void connectionToPeer(final String pPeerName, int peerPort) {
        try {
            Socket socket = new Socket(SERVER_PEER, peerPort);
            connections.put(pPeerName, socket);
            new Thread(new Listen(socket, mUserInfo.getName(),this)).start();
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(mUserInfo.getName());
            System.out.println("Connected to " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
        } catch (IOException pE) {
            System.out.println("Failed to connect to " + pPeerName + " on port " + peerPort);
            pE.printStackTrace();
        }
    }

    private void acceptConnections() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                new Thread(new Listen(socket, mUserInfo.getName(),this)).start();

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String peer = in.readLine();
                connections.put(peer, socket);
                System.out.println("Peer connected: " + peer + " (" + socket.getInetAddress().getHostAddress() + ")");
            } catch (IOException e) {
                if (serverSocket.isClosed()) {
                    System.out.println("Peer disconnected");
                    break;
                }
                e.printStackTrace();
            }
        }
    }

    private void sendMessage(String peerName, String message) {
        Socket socket = connections.get(peerName);
        if (socket != null && !socket.isClosed()) {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(mUserInfo.getName() + ": " + message);
                System.out.println("Message sent to " + peerName + " (" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ")");
            } catch (IOException e) {
                System.out.println("Failed to send message to " + peerName);
                e.printStackTrace();
            }
        } else {
            System.out.println("Peer not connected: " + peerName);
        }
    }

    private void chatWithPeer(String peerName) {
        Socket socket = connections.get(peerName);
        if (socket != null && !socket.isClosed()) {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                while (true) {
                    System.out.println("Type message to " + peerName + " (or 'back' to return to main menu): ");
                    String message = mScanner.nextLine();
                    if (message.equalsIgnoreCase("back")) {
                        break;
                    }
                    out.println(mUserInfo.getName() + ": " + message);
                    System.out.println("Message sent to " + peerName);
                }
            } catch (IOException e) {
                System.out.println("Error chatting with " + peerName);
                e.printStackTrace();
            }
        } else {
            System.out.println("Peer not connected: " + peerName);
        }
    }

    private void closeAllConnections() {
        for (Map.Entry<String,Socket> entry : connections.entrySet()) {
            try {
                String peer = entry.getKey();
                Socket socket = entry.getValue();

                if (!socket.isClosed()) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(mUserInfo.getName() + ":" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + "is connected");
                    socket.close();
                    System.out.print("Disconnected from " + peer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Closed server socket");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Peer <username> <port>");
            return;
        }


        String username = args[0];
        int port = Integer.parseInt(args[1]);

        new PeerChat(username, port).start();

    }
}