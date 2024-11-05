package tokenRing.peers;

import tokenRing.server.PeerInfo;
import utils.Menu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class PeerM {
    private ServerSocket serverSocket;
  private PeerInfo mPeerInfo;
  private PeerInfo nextPeerInfo;
  private Logger mLogger;
  private boolean running = false;
  private boolean hasToken;
  private Scanner scanner;
  private static final int SERVER_PORT = 8023;
  private final ExecutorService connectionExecutor;
  private static int numberName = 1;

  public PeerM(PeerInfo peerInfo, PeerInfo nextPeerInfo) {
    this.scanner = new Scanner(System.in);
    this.hasToken = false;
    this.mPeerInfo = peerInfo;
    this.nextPeerInfo = nextPeerInfo;
    this.connectionExecutor = Executors.newSingleThreadExecutor();
    initLogger();
  }

  private void initLogger() {
    mLogger = Logger.getLogger("logfile");
    try {
      FileHandler handler = new FileHandler("./" + mPeerInfo.getAddress() + "_" + mPeerInfo.getPort() + "_peer.log", true);
      mLogger.addHandler(handler);
      SimpleFormatter formatter = new SimpleFormatter();
      handler.setFormatter(formatter);
    } catch (Exception e) {
      e.printStackTrace();
    }

    mLogger.info("Peer name: " + mPeerInfo.getName() + ", Host: " + mPeerInfo.getName() + ", Port: " + mPeerInfo.getPort());
  }

  public static synchronized  int getNumberName() {
    return numberName;
  }

  public void setNumberName(int numberName) {
    this.numberName = numberName;
  }

  public void startPeer() {
    mLogger.info("Peer started on port " + mPeerInfo.getPort());
    mLogger.info("Neighbour:\n" + nextPeerInfo.getName());
    connectionExecutor.submit(this::acceptConnections);
    running = true;
    while (running) {
      String line = scanner.nextLine();
      try {
        handleConnection(line);
      } catch (IOException e) {
        mLogger.severe("Error handling connection: " + e.getMessage());
      }
    }
  }

  private void acceptConnections() {
    try {
      serverSocket = new ServerSocket(mPeerInfo.getPort(), 50, InetAddress.getByName("localhost"));
      mLogger.info("Server socket created on port " + mPeerInfo.getPort());
      while (!serverSocket.isClosed()) {
        Socket clientSocket = serverSocket.accept();
        mLogger.info("Accepted connection from " + clientSocket.getRemoteSocketAddress());
        connectionExecutor.submit(() -> new ConnectionPeer(mPeerInfo, nextPeerInfo, clientSocket, hasToken, mLogger).run());
      }
    } catch (IOException e) {
      mLogger.severe("Error setting up server socket: " + e.getMessage());
    } finally {
      try {
        if (serverSocket != null && !serverSocket.isClosed()) {
          serverSocket.close();
        }
      } catch (IOException e) {
        mLogger.severe("Error closing server socket: " + e.getMessage());
      }
    }
  }

  private void handleConnection(String command) throws IOException {
    switch (command) {
      case "1":
        if (hasToken) {
          passToken();
        }
        break;
      case "2":
        String message = Menu.handleOperation(scanner);
        sendOperationToServer(message);
        break;
      case "3":
        running = false;
        System.out.println("Exiting...");
        break;
      case "help":
        Menu.helpMenu();
        break;
      default:
        System.out.println("Unknown command. Type 'help' for options.");
    }
  }

  private void passToken() {
    try {
      if (!hasToken) return;

      mLogger.info("Peer " + mPeerInfo.getName() + " passed the token.");
      System.out.println("Token passed.");
      Socket socket = new Socket(nextPeerInfo.getAddress(), nextPeerInfo.getPort());
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
      out.println("TOKEN_HOLDER " + nextPeerInfo.getName());

      // Await acknowledgment
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      String response = in.readLine().trim();
      if ("Acknowledged token holder".equals(response)) {
        hasToken = false;
        mLogger.info("Peer " + nextPeerInfo.getName() + " has the token.");
        System.out.println("Peer ID: " + mPeerInfo.getName() + " passed the token to " + nextPeerInfo.getName());
      } else {
        System.out.println("Peer ID: " + mPeerInfo.getName() + " failed to pass the token");
        mLogger.warning("Peer ID: " + mPeerInfo.getName() + " failed to pass the token");
      }
      socket.close();
    } catch (IOException e) {
      mLogger.warning("Error while trying to pass the token.");
      e.printStackTrace();
    }
  }

  private void sendOperationToServer(String operation) {
    try (
            Socket socket = new Socket("localhost", SERVER_PORT);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

      out.println(operation);
      String response = in.readLine();
      System.out.println("Server response: Operation " + operation + " --> " + response);
    } catch (IOException e) {
      if (e instanceof ConnectException) {
        System.out.println("Peer " + mPeerInfo.getName() + " failed to connect to server.");
      }else {
        mLogger.warning("Error communicating with server: " + e.getMessage());
      }
    }
  }

  public void stop() {
    running = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException e) {
      mLogger.warning("Error closing server socket: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: java Peer <myPeerPort> <nextPeerPort>");
      System.exit(1);
    }

    int myPeerPort = Integer.parseInt(args[0]);
    int nextPeerPort = Integer.parseInt(args[1]);
    String name = "m" + getNumberName();
    String HOST_PEER = "localhost";
    PeerInfo peerInfo = new PeerInfo(name,myPeerPort, HOST_PEER);
    PeerInfo nextPeerInfo = new PeerInfo(name,nextPeerPort, HOST_PEER);
    PeerM peer = new PeerM(peerInfo, nextPeerInfo);
    peer.startPeer();

    String register = "REGISTER " + peerInfo.getAddress() + " " + peerInfo.getPort() + " " + nextPeerInfo.getPort();
    peer.sendOperationToServer(register);
  }

}
