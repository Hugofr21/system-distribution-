package tokenRing.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Server {
    private int mPort_Server;
    private ServerSocket mServerSocket;
    private String mHost_Server;
    public  Logger mLogger = Logger.getLogger("Server");
    public boolean running = false;
    private ServerSocket server;
    private final int PORT_SERVER = 5000;
    private final String HOST_SERVER = "localhost";
    private MangerPeer mPeerManager;


    public Server() {
        this.mPeerManager = new MangerPeer();
        try {
            this.server = new ServerSocket(PORT_SERVER, 1, InetAddress.getByName(HOST_SERVER));
            FileHandler handler = new FileHandler("./" + PORT_SERVER + "_peer.log", true);
            mLogger.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
            mLogger.info("Server started on" + HOST_SERVER + ":" + PORT_SERVER);

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
    public InetAddress getSocketAddress() {
        return this.server.getInetAddress();
    }

    private void initServer() throws IOException {
        InetAddress inetAddress = InetAddress.getLocalHost();
        String localIP = inetAddress.getHostAddress();
        System.out.println("IP: " + localIP);
        server = new ServerSocket(PORT_SERVER, 50, InetAddress.getByName(HOST_SERVER));
        System.out.println("Server listening on port " + PORT_SERVER);
        System.out.println("Server IP address: " + getSocketAddress().getHostAddress());

        while (true) {
            mLogger.info("Server: endpoint running at port " + PORT_SERVER + "...");
            Socket client = server.accept();
            System.out.println("Client connected");

            new Thread(() -> {
                try {
                    new ServerHandle(HOST_SERVER, client, mLogger, mPeerManager).run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void sendTokenPeer() {
        if (mPeerManager.getPeers().isEmpty()) {
            System.out.println("No peers found!");
            return;
        }

        PeerInfo sendToTokenPeer = mPeerManager.receivedTokenPeer();
        if (sendToTokenPeer == null) {
            System.out.println("There is no peer to connect!");
            return;
        }

        String token = "TOKEN_HOLDER " + sendToTokenPeer.getName();
        try {
            System.out.println("Initializing token first peer: Host " + sendToTokenPeer.getAddress() + " Port " + sendToTokenPeer.getPort());
            Socket firstPeerIdSocket = new Socket(sendToTokenPeer.getAddress(), sendToTokenPeer.getPort());
            PrintWriter out = new PrintWriter(firstPeerIdSocket.getOutputStream(), true);
            out.println(token);
            mPeerManager.currentPeerInfoToToken(sendToTokenPeer);
            firstPeerIdSocket.close();
            mLogger.info("Token first peer initialized: " + sendToTokenPeer.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.initServer();
    }



}
