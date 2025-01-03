package assign.tom.chat.network;



import assign.tom.chat.user.UserInfo;
import assign.tom.system.FileSystem;
import assign.tom.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class PeerChat {
    private String NAME_ID;
    private ServerSocket serverSocket;
    private final String SERVER_PEER = "localhost";
    private final int SERVER_PORT;
    Logger mLogger;
    Scanner mScanner;
    private final UserInfo mUserInfo;
    private final Map<String, Socket> connections;
    private final Map<String, Listen> listenConnections;
    private final Map<UserInfo, Integer> neighbour;
    private final FileSystem mFileSystem;
    private TotallyOrderedMulticast multicast;
    public List<String> words;
    Random random;

    public PeerChat(String username, int port) {
        String TXT_FILE_WORDS = "wordList.txt";
        String JSON_FILE_PEERS = "bootstrapper.json";
        this.NAME_ID = null;
        this.mUserInfo = new UserInfo(username);
        this.SERVER_PORT = port;
        this.mScanner = new Scanner(System.in);
        this.mFileSystem =   new FileSystem(TXT_FILE_WORDS, JSON_FILE_PEERS);
        this.neighbour = new HashMap<>();
        this.multicast = new TotallyOrderedMulticast(NAME_ID, this);
        this.connections = new HashMap<>();
        this.listenConnections = new HashMap<>();
        this.random = new Random();
        init();
    }

    private void init(){
        mLogger = Logger.getLogger("logfile");
        try {
            FileHandler handler = new FileHandler( String.format("%d_%S_peer.log", SERVER_PORT,SERVER_PEER), true);
            assert mLogger != null;
            mLogger.addHandler(handler);
            handler.setFormatter(new SimpleFormatter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public String getNAMED(){ return mUserInfo.getName(); }

    public void setNAME_ID(String NAME_ID) {
        this.NAME_ID = NAME_ID;
    }

    public Map<String, Socket> getConnections(){
        return connections;
    }

    public long  generatePoissonDistribution() {
        double LAMBDA = 4.0;
        double nextArrival = -Math.log(1.0 - random.nextDouble()) / LAMBDA;
        return (long) (nextArrival * 20000);
    }

    private void start() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Peer started on port " + SERVER_PORT);
            new Thread(this::acceptConnections).start();

            words = FileSystem.read();

            if (words != null && !words.isEmpty()) {
                words.forEach(w -> System.out.println(" - " + w));
            } else {
                System.out.println("No words found in the file.");
            }

            while (true) {
                Utils.helpMenu();
                String receive = mScanner.nextLine();

                switch (receive) {
                    case "1":
                        chatWithPersonByName();
                        break;
                    case "2":
                        System.out.print("Enter Connect multicast to group network: ");
                        chatWithMultiPerson();
                        break;
                    case "3":
                        System.out.println("List of words:");
                        if (words != null && !words.isEmpty()) {
                            words.forEach(w -> System.out.println(" - " + w));
                        }
                        break;
                    case "4":
                        System.out.println("List of neighbors:");
                        if (!neighbour.isEmpty()) {
                            neighbour.forEach((userInfo, port) ->
                                    System.out.println(userInfo.getName() + " on port " + port));
                        } else {
                            System.out.println("No neighbors found.");
                        }
                        break;
                    case "6":
                        System.out.println("Exiting...");
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }

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
            Listen listen  = new Listen(socket,this,multicast);
            new Thread(listen).start();
            PrintWriter out = listen.getOutPrintWriter();
            out.println(mUserInfo.getName());
            listenConnections.put(pPeerName, listen);
            System.out.println("[SUCCESS] Connected to " + pPeerName +" at " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
        } catch (IOException pE) {
            System.out.println("[ERROR] Failed to connect to " + pPeerName + " on port " + peerPort);
            pE.printStackTrace();
        }
    }

    private void acceptConnections() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                Listen listen  = new Listen(socket,this,multicast);
                new Thread(listen).start();
                BufferedReader in = listen.getInBufferedReader();
                String peer = in.readLine();
                connections.put(peer, socket);
                System.out.println("[DEBUG] Peer " + peer + " connected.");
                listenConnections.put(peer, listen);
                System.out.println("Peer connected: " + peer + " (" + socket.getInetAddress().getHostAddress() + ")");
            } catch (IOException e) {
                if (serverSocket.isClosed()) {
                    System.out.println("[ERROR] Peer disconnected");
                    break;
                }
                e.printStackTrace();
            }
        }
    }

    private void chatWithPersonByName(){
        System.out.print("Enter the peer name: ");
        String name = mScanner.nextLine();

        Optional<Map.Entry<UserInfo, Integer>> optNeigbour = neighbour.entrySet().stream()
                .filter(e -> e.getKey().getName().equals(name))
                .findFirst();

        if (optNeigbour.isEmpty() || optNeigbour.get().getValue() < 1) {
             System.out.println("[ERROR] Invalid peer name.");
            return;
        }
        Map.Entry<UserInfo, Integer> neighbour = optNeigbour.get();
        int port = neighbour.getValue();

        if (!connections.containsKey(name)) {
            System.out.println("[INFO] Creating new connection to: " + name);
            connectionToPeer(name, port);
        }else {
            System.out.println("[ERROR] Peer " + name + " already exists.");
        }

        if (connections.containsKey(name)) {
            Listen listen = listenConnections.get(name);
            if (listen != null) {
                new Thread(()->sendListWordForPeer(listen)).start();
                while (true){
                    String messageLine = mScanner.nextLine();
                    if ("exit".equals(messageLine)) {
                        System.out.println("[INFO] Exiting...");
                        break;
                    }
                    multicast.sendMessageToClient(listen, mUserInfo, messageLine, false);
                }
            }
        }
    }

    private void sendListWordForPeer(Listen listen) {
        if (words == null && words.isEmpty()) {
            System.out.println("[INFO] List of words:");
            return;
        }
        int current = 0;
        while (current < words.size()) {
            try {
                long delay = generatePoissonDistribution();
                Thread.sleep(delay);

                String word = words.get(current);

                multicast.sendMessageToClient(listen, mUserInfo, word, false);
                System.out.println("[INFO] " + word + " sent.");
                current++;

            }catch (InterruptedException e){
                System.out.println("[ERROR] Thread interrupted.");
                break;
            }
        }
        System.out.println("[INFO] List of words sent.");
    }


    private void chatWithMultiPerson() {
        if (neighbour.isEmpty()) {
            System.out.println("No neighbours found.");
            return;
        }

        System.out.println("Total neighbours to connect: " + neighbour.size());

        neighbour.forEach((userInfo, port) -> {
            String neighborName = userInfo.getName();
            System.out.println("\n------ Processing neighbor: " + neighborName + " ------");
            System.out.println("Attempting connection on port " + port);

            try {
                if (!connections.containsKey(neighborName)) {
                    System.out.println("[INFO] Creating new connection to: " + neighborName);
                    connectionToPeer(neighborName, port);
                }

                Socket socket = connections.get(neighborName);
                if (socket == null || !socket.isConnected()) {
                    System.out.println("[INFO] Reconnecting to: " + neighborName);
                    connectionToPeer(neighborName, port);
                }

                Listen listen = listenConnections.get(neighborName);
                if (listen != null) {
                    String content = "Hello World!!!";
                    multicast.sendMessageToClient(listen,mUserInfo,content,true );
                    if (words != null &&!words.isEmpty()){
                        multicast.sendGlobalOrderWordsList(mUserInfo, listen);
                        new Thread(()->sendListWordForPeer(listen)).start();
                    }
                } else {
                    System.out.println("[ERROR] No listener found for: " + neighborName);
                }
            }catch (Exception e) {
                System.out.println("[ERROR] Failed to connect to " + neighborName + " on port " + port);
                System.out.println("[DETAILS] " + e.getMessage());
            }
        });
        System.out.println("\nBroadcast complete to all neighbors");
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
                System.out.println("[ERROR] Closed server socket");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectionsAllPeersBootstrapper(int port, String username) {
        try {
            List<String> peers = FileSystem.readJson();
            if (!peers.isEmpty()) {
                for (String peer : peers) {
                    String[] parts = peer.split(" ");
                    String id = parts[0];
                    String usernamePeer = parts[1];
                    int portPeers = Integer.parseInt(parts[2]);
                    if (port == portPeers) {
                        System.out.println("Init peer name " + id + " username: " + username + " on port " + port);
                        setNAME_ID(id);
                    }else {
                        UserInfo newUserInfo = new UserInfo(usernamePeer);
                        synchronized (neighbour) {
                            neighbour.put(newUserInfo, portPeers);
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("[ERROR] Fail read the file JSON: " + e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("[ERROR] File not found or path invalid: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Occurred a err desperado: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Peer <username> <port>");
            return;
        }

        int port = Integer.parseInt(args[1]);
        String username = args[0];
        PeerChat peerChat = new PeerChat(username, port);
        peerChat.connectionsAllPeersBootstrapper(port, username);
        peerChat.start();

    }
}