package assign.ring.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Server {
    public  Logger mLogger = Logger.getLogger("Server");
    public boolean running = false;
    private ServerSocket server;
    private final int PORT_SERVER = 6000;
    private final String HOST_SERVER = "localhost";
    private final MangerPeer mPeerManager;


    public Server() {
        this.mPeerManager = new MangerPeer();
        init();
    }

    private void init(){
        try {
            FileHandler handler = new FileHandler("./" + "localhost_Server_"+ PORT_SERVER + "_peer.log", true);
            mLogger.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
            mLogger.info("Server started on  " + HOST_SERVER + ":" + PORT_SERVER);

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
    public InetAddress getSocketAddress() {
        return this.server != null ? this.server.getInetAddress() : null;
    }

    private boolean isActivePort() {
        try{
            ServerSocket test = new ServerSocket(PORT_SERVER, 1, InetAddress.getByName(HOST_SERVER));
            test.close();
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    private void initServer() throws IOException {
        InetAddress inetAddress = InetAddress.getLocalHost();
        String localIP = inetAddress.getHostAddress();
        System.out.println("IP: " + localIP);

        if (!isActivePort()) throw new IOException("Port: " + PORT_SERVER + " is not active");

        server = new ServerSocket(PORT_SERVER, 50, InetAddress.getByName(HOST_SERVER));
        System.out.println("Server listening on port " + PORT_SERVER);
        System.out.println("Server IP address: " + getSocketAddress().getHostAddress());
        running = true;
        while (running) {
            mLogger.info("Server: endpoint running at port " + PORT_SERVER + "...");
            Socket client = server.accept();
            System.out.println("Client connected");

            Thread clientThread = new Thread(() -> {
                try {
                    new ServerHandle(HOST_SERVER, client, mLogger, mPeerManager).run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            clientThread.start();
        }
    }


    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.initServer();
    }



}
