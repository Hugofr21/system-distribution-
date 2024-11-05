package antiEntropy.network;
import tokenRing.server.PeerInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Peer {
    private final List<Neighbour> mNeighbours;
    private final int port;
    public static final String HOST = "localhost";
    private final String Name;
    private Logger mLogger;
    private static int numberName = 1;
    private ServerSocket serverSocket;
    private final Set<PeerInfo> connectedPeers = Collections.synchronizedSet(new HashSet<>());
    private ExecutorService executor;
    private boolean running = true;

    public Peer(String name, int port, List<Neighbour> neighbours) {
        this.Name = name;
        this.mNeighbours = neighbours;
        this.port = port;
        this.executor = Executors.newCachedThreadPool();
        init();
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

    private boolean isConnected(PeerInfo info) {
        return connectedPeers.contains(info);
    }

    private void init() {
        try {
            mLogger = Logger.getLogger("logfile");
            FileHandler handler = new FileHandler("./" + port + "_" + HOST + "_" + Name + "_peer_entropy.log", true);
            mLogger.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startConnectionWithClient(Socket socket) {
        ConnectionPeer connectionPeer = new ConnectionPeer(socket, mLogger, this);
        executor.execute(connectionPeer);
        connectedPeers.add(new PeerInfo(getName(), socket.getPort(), HOST));
        System.out.println("Started connection with client from " + socket.getInetAddress().getHostAddress());
    }

    private void startServer() throws IOException {
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName(HOST));
            System.out.println("Server started on port " + port);

            Thread acceptThread = new Thread(() -> {
                while (running && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        startConnectionWithClient(clientSocket);
                    } catch (IOException e) {
                        if (running) {
                            mLogger.warning("Error accepting connection: " + e.getMessage());
                        }
                    }
                }
            });
            acceptThread.start();

            // Agendar conexão com vizinhos após delay
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    connectToNeighbours();
                }
            }, 5000);

        } catch (IOException e) {
            mLogger.severe("Could not start server: " + e.getMessage());
            throw e;
        }
    }

    private void connectToNeighbours() {
        for (Neighbour neighbour : mNeighbours) {
            Map<String, PeerInfo> peers = neighbour.getNeighbours();
            if (peers != null) {
                for (Map.Entry<String, PeerInfo> entry : peers.entrySet()) {
                    PeerInfo peerInfo = entry.getValue();
                    if (!isConnected(peerInfo)) {
                        connectToPeer(peerInfo);
                    }
                }
            }
        }
    }

    private void connectToPeer(PeerInfo peerInfo) {
        try {
            Socket socket = new Socket(HOST, peerInfo.getPort());
            startConnectionWithClient(socket);
            mLogger.info("Connected to peer: " + peerInfo.getName() + " on port " + peerInfo.getPort());
        } catch (IOException e) {
            mLogger.warning("Failed to connect to " + peerInfo.getName() + " on port " + peerInfo.getPort() + ": " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executor.shutdown();
        } catch (IOException e) {
            mLogger.warning("Error during shutdown: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Peer <myPeerPort> <neighbour1> <neighbour2>...");
            System.exit(1);
        }

        try {
            String name = "m" + getNumberName();
            int myPeerPort = Integer.parseInt(args[0]);
            List<Neighbour> neighbours = new ArrayList<>();

            for (int i = 1; i < args.length; i++) {
                int neighbourPort = Integer.parseInt(args[i]);
                Neighbour neighbour = new Neighbour();
                String nameNeighbour = "m" + getNumberName();
                neighbour.update(nameNeighbour, neighbourPort, HOST);
                neighbours.add(neighbour);
            }

            Peer peer = new Peer(name, myPeerPort, neighbours);
            System.out.println("Peer " + name + " starting on PORT: " + myPeerPort + " HOST: " + HOST);

            peer.startServer();

            Runtime.getRuntime().addShutdownHook(new Thread(peer::shutdown));

        } catch (IOException e) {
            System.err.println("Error starting peer: " + e.getMessage());
            System.exit(1);
        }
    }
}
