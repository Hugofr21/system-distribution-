package assign.p2p.network;


import assign.p2p.neigbour.Neighbour;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Peer {
    private final Set<Integer> connectedPorts;
    private final Map<String, Socket> socketMap;
    private final int port;
    public static final String HOST = "localhost";
    private final String Name;
    private Logger mLogger;
    private static int numberName = 1;
    private ServerSocket serverSocket;
    private final ExecutorService executor;
    private final ScheduledExecutorService neighbourExecutor;
    private boolean running = true;
    private final Scanner scanner = new Scanner(System.in);
    private final Random rand;
    private ScheduledFuture<?> antiEntropyTask = null;
    private boolean antiEntropyActive = false;
    public final Neighbour neighbour;

    // timestamps for the map entries and remove entries older
    private static  final long PEER_TIMEOUT = 20000;
    private static final long CLEANUP_INTERVAL = 10;

    public Peer(String name, int port) {
        this.Name = name;
        this.port = port;
        this.executor = Executors.newCachedThreadPool();
        this.neighbourExecutor = Executors.newScheduledThreadPool(6);
        this.rand = new Random();
        this.connectedPorts = Collections.synchronizedSet(new HashSet<>());
        this.socketMap = Collections.synchronizedMap(new HashMap<>());
        this.neighbour = new Neighbour();
        initLogger();
    }

    public static synchronized int getNumberName() {
        return numberName++;
    }

    public String getName() {
        return Name;
    }

    public int getPort() {
        return port;
    }

    private void initLogger() {
        try {
            mLogger = Logger.getLogger("PeerLogger");
            FileHandler handler = new FileHandler(String.format("./%d_%s_%s_Peer_entropy.log" ,numberName, getPort(), HOST), true);
            handler.setFormatter(new SimpleFormatter());
            mLogger.addHandler(handler);
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName(HOST));
            System.out.println("Peer " + Name + " started on PORT: " + port);
            mLogger.info("Server started on port " + port);

            executor.submit(this::listenForConnections);
            neighbourExecutor.scheduleAtFixedRate(this::connectToNeighbours, 0, 5, TimeUnit.SECONDS);

//            neighbourExecutor.scheduleAtFixedRate(this::cleanStalePeers, CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.SECONDS);

            menuPeer();

        } catch (IOException e) {
            mLogger.severe("Error starting peer: " + e.getMessage());
        }
    }

    private void listenForConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                ConnectionPeer connectionPeer = new ConnectionPeer(clientSocket, mLogger, this);
                executor.submit(connectionPeer);
            } catch (IOException e) {
                if (running) {
                    mLogger.warning("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    public void addNeighbour(String name, int port) {
        synchronized (neighbour) {
            Map<String, PeerInfo> peers = neighbour.getNeighbours();
            if (peers != null && peers.containsKey(String.valueOf(port))) {
                mLogger.info("Neighbor already exists: " + name + " on port " + port);
                return;
            }
            neighbour.saveAndUpdate(name, port, HOST);
        }
    }

    private void connectToNeighbours() {
        synchronized (neighbour) {
                Map<String, PeerInfo> peers = neighbour.getNeighbours();

                if (peers != null) {
                    for (Map.Entry<String, PeerInfo> entry : peers.entrySet()){
                        PeerInfo peerInfo = entry.getValue();
                        String key = peerInfo.getName() + ":" + peerInfo.getPort();

                        if (!connectedPorts.contains(peerInfo.getPort()) &&
                                !socketMap.containsKey(key) && peerInfo.getPort() != this.port)
                        {
                            try {
                                Socket socket = new Socket(HOST, peerInfo.getPort());
                                connectedPorts.add(peerInfo.getPort());
                                socketMap.put(key, socket);
                                ConnectionPeer connectionPeer = new ConnectionPeer(socket, mLogger, this);
                                neighbour.updateConnection(peerInfo.getPort(), connectionPeer);
                                executor.submit(connectionPeer);
                                mLogger.info("Connected to peer: " + peerInfo.getName() + " on port " + peerInfo.getPort());
                            } catch (IOException e) {
                                mLogger.warning("Failed to connect to " + peerInfo.getName() + " on port " + peerInfo.getPort());
                               // neighbour.remove(peerInfo.getPort());
                            }
                        }

                    }
                }

                if (neighbour.isEmpty()) {
                    mLogger.info("Removed empty neighbour.");
                }

        }

    }


    private void sendNeighbourMapToPeer(PeerInfo peerInfo) {

        if (peerInfo == null) {
            mLogger.warning("No peer information available for neighbour");
            return;
        }


        String key = peerInfo.getName() + ":" + peerInfo.getPort();
        ConnectionPeer connectionPeer = neighbour.getConnectionPeerByPort(peerInfo.getPort());

        if (connectionPeer == null) {
            mLogger.warning("No peer info found for current peer: " + key);
            return;
        }

        try {
            Socket socket;
            synchronized (socketMap){
                if (socketMap.containsKey(key) && !socketMap.get(key).isClosed()) {
                    socket = socketMap.get(key);
                }else {
                    socket = new Socket(HOST, peerInfo.getPort());
                    socketMap.put(key, socket);
                }
            }

            PrintWriter out = connectionPeer.getPrintWriter();

            synchronized (neighbour) {
                for (Map.Entry<String, PeerInfo> entry : neighbour.getNeighbours().entrySet()) {
                    PeerInfo knownPeerInfo = entry.getValue();
                    String peerMessage = "REGISTER_NEIGHBOUR " + knownPeerInfo.getName() + " " + knownPeerInfo.getPort() + " " + knownPeerInfo.getAddress();
                    out.println(peerMessage);
                    mLogger.info("Sent neighbour info: " + peerMessage);
                }
            }

            socket.close();
            mLogger.info("Successfully sent map to peer: " + peerInfo.getName());
        } catch (IOException e) {
            mLogger.warning("Failed to send map to peer " + peerInfo.getName() + " Error: " + e.getMessage());
        }
    }



    private void cleanStalePeers() {
        long currentTime = System.currentTimeMillis();
        int count = 0;

        synchronized (neighbour) {
                Map<String,PeerInfo> peers = neighbour.getNeighbours();
                if (peers != null) {
                    Iterator<Map.Entry<String, PeerInfo>> peerIterator = peers.entrySet().iterator();
                    while (peerIterator.hasNext()) {

                        Map.Entry<String, PeerInfo> entry = peerIterator.next();
                        PeerInfo peerInfo = entry.getValue();
                        String key = peerInfo.getName() + ":" + peerInfo.getPort();

                        if (!isPeerActive(peerInfo)) {
                            peerIterator.remove();
                            neighbour.remove(peerInfo.getPort());
                            connectedPorts.remove(peerInfo.getPort());
                            Socket socket = socketMap.remove(key);
                            if (socket != null) {
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    mLogger.warning("Error closing socket for inactive peer: " + e.getMessage());
                                }
                            }
                            count++;
                            mLogger.info("Removed inactive peer: " + peerInfo.getName() + " on port " + peerInfo.getPort());
                        } else {

                            sendUpdateLastTimeMessage(peerInfo);
                        }
                    }

                }
        }

        if (count > 0) {
            mLogger.info("Cleaned up " + count + " inactive peers.");
        } else {
            mLogger.info("No inactive peers found during cleanup.");
        }
    }
    private boolean isPeerActive(PeerInfo peerInfo) {
        try (Socket socket = new Socket(peerInfo.getAddress(), peerInfo.getPort())) {
            return true;
        } catch (IOException e) {
            mLogger.warning("Peer " + peerInfo.getName() + " on port " + peerInfo.getPort() + " is inactive: " + e.getMessage());
            return false;
        }
    }

    private void sendUpdateLastTimeMessage(PeerInfo peerInfo) {
        String key = peerInfo.getName() + ":" + peerInfo.getPort();
        ConnectionPeer connectionPeer = neighbour.getConnectionPeerByPort(peerInfo.getPort());
        if (connectionPeer != null) {
            PrintWriter out = connectionPeer.getPrintWriter();
            String message = "ACTIVE_NEIGHBOUR " + peerInfo.getPort();
            out.println(message);
            out.flush();

        }else {
            mLogger.warning("Peer " + key + " not found");
        }

    }

    public void displayNeighbours() {
        System.out.println("\n--- Neighbours List ---" + "count neighbours: " + neighbour.sizeNeighbours());
        synchronized (neighbour) {
            if (neighbour.isEmpty()) {
                System.out.println("No neighbours connected.");
            } else {
                Map<String, PeerInfo> peers = neighbour.getNeighbours();
                for (Map.Entry<String,PeerInfo> entry : peers.entrySet()) {
                    //String key = entry.getKey();
                    PeerInfo peerInfo = entry.getValue();
                    System.out.println(peerInfo);
                }
            }
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executor.shutdown();
            neighbourExecutor.shutdown();
            System.out.println("Peer shutting down.");
        } catch (IOException e) {
            mLogger.warning("Error during shutdown: " + e.getMessage());
        }
    }

    public long  generatePoissonDistribution() {
        double LAMBDA = 4.0;
        double nextArrival = -Math.log(1.0 - rand.nextDouble()) / LAMBDA;
        return (long) (nextArrival * 60000);
    }

    private void startAntiEntropyUpdate() {
        if (antiEntropyActive) {
            return;
        }

        antiEntropyActive = true;
        antiEntropyTask = neighbourExecutor.scheduleWithFixedDelay(() -> {

            try {
                synchronized (neighbour) {
                    if (neighbour.isEmpty()) {
                        return;
                    }
                    long currentTime = System.currentTimeMillis();
                    Map<String, PeerInfo> peers = neighbour.getNeighbours();

                    peers.entrySet().removeIf(entry -> currentTime - entry.getValue().getLastUpdated() > PEER_TIMEOUT);

                    for (Map.Entry<String , PeerInfo> entry : peers.entrySet()) {
                        PeerInfo peerInfo = entry.getValue();
                        sendNeighbourMapToPeer(peerInfo);
                    }
                }
            } catch (Exception e) {
                mLogger.warning("Error during anti-entropy update: " + e.getMessage());
            }
        }, 0, generatePoissonDistribution(), TimeUnit.MILLISECONDS);
    }

    private void stopAntiEntropyUpdate() {
        if (antiEntropyTask != null && !antiEntropyTask.isCancelled()) {
            antiEntropyTask.cancel(false);
            antiEntropyTask = null;
            antiEntropyActive = false;
        } else {
            System.out.println("Anti-Entropy Update is not active.");
        }
    }



    public void menuPeer() {
        while (running) {
            System.out.println("\n--- Peer Menu ---");
            System.out.println("1. View Neighbours");
            System.out.println("2. Anti-Entropy Update");
            System.out.println("3. Stop Anti-Entropy Update");
            System.out.println("4. Update neighbors' map");
            System.out.println("5. Exit");
            System.out.print("Enter your choice: ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        displayNeighbours();
                        break;
                    case 2:
                        startAntiEntropyUpdate();
                        break;
                    case 3:
                        stopAntiEntropyUpdate();
                        break;
                    case 4:
                        cleanStalePeers();
                        break;
                    case 5:
                        shutdown();
                        return;

                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine();
            }
        }
    }


    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Peer <myPeerPort> <neighbour1> <neighbour2>...");
            return;
        }

        try {
            int myPeerPort = Integer.parseInt(args[0]);
            String name = "Peer_"+ getNumberName() + "_" + myPeerPort;

            Peer peer = new Peer(name, myPeerPort);

            for (int i = 1; i < args.length; i++) {
                int neighbourPort = Integer.parseInt(args[i]);
                String neighbourName = "Peer_" + getNumberName() +"_" + neighbourPort;
                peer.addNeighbour(neighbourName, neighbourPort);
            }

            peer.start();

        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + e.getMessage());
        }
    }
}
