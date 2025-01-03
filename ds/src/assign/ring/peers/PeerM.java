package assign.ring.peers;


import assign.ring.server.PeerInfo;
import assign.ring.utils.Menu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class PeerM {
  private Socket serverSocket;
  private final PeerInfo mPeerInfo;
  private final PeerInfo nextPeerInfo;
  private Logger mLogger;
  private final AtomicBoolean running;
  private boolean hasToken;
  private final Scanner scanner;
  private static final int SERVER_PORT = 6000;
  private final ExecutorService connectionExecutor;
  private static int numberName = 1;

  private final BlockingDeque<String> requestQueue;
  private final Random random;
  private static final String[] operationsCalculator = {"add", "div", "mul", "sub"};
  private volatile boolean isProcessing = false;

  private Socket neighbourSocket;
  private ConnectionPeer connectionPeer;
  private ConnectionPeer connectionPeerServer;

  public PeerM(PeerInfo peerInfo, PeerInfo nextPeerInfo) {
    this.scanner = new Scanner(System.in);
    this.running = new AtomicBoolean(false);
    this.hasToken = false;
    this.mPeerInfo = peerInfo;
    this.nextPeerInfo = nextPeerInfo;
    this.requestQueue = new LinkedBlockingDeque<>();
    this.random = new Random();
    this.connectionExecutor = Executors.newFixedThreadPool(6);
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

  public static synchronized int getNumberName() {
    return numberName;
  }

  public void setNumberName(int numberName) {
    PeerM.numberName = numberName;
  }

  public void startPeer() {
    mLogger.info("This peer started on port " + mPeerInfo.getPort() + " name " + nextPeerInfo.getName());
    mLogger.info("Neighbour: " + nextPeerInfo.getName() + " Port: " + nextPeerInfo.getPort());
    connectionExecutor.submit(this::connectToServer);
    connectionExecutor.submit(this::acceptConnectionNeighbour);
    connectionExecutor.submit(this::generateRequestsServer);

    // Main loop for handling user commands
    running.set(true);
    while (running.get()) {
      String line = scanner.nextLine();
      try {
        System.out.println("\n #### Menu #### ");
        System.out.println("1. Send operation to calculate");
        System.out.println("2. Pass the token to the next peer.");
        System.out.println("3. Exit");

        handleConnectionSend(line);
      } catch (IOException e) {
        mLogger.severe("Error handling connection: " + e.getMessage());
      }
    }
  }

  private void generateRequestsServer() {
    try {
      while (running.get()) {
        if (requestQueue.size() < 10) {
          Menu.generatePoissonDistribution(random);
          String operation = Menu.generateRandomOperation(random, operationsCalculator);
          requestQueue.put(operation);
          mLogger.info("Request added to queue: " + operation);
        }
      }
    } catch (Exception e) {
      mLogger.severe("Error generating requests: " + e.getMessage());
    }
  }

  public void processRequest() {
    mLogger.info("Starting request processing");
    isProcessing = true;
    hasToken = true;
    int processedRequests = 0;
    while (running.get()) {
      try {
        if (!requestQueue.isEmpty() && processedRequests >= 3) {
          mLogger.info("Processed mininum requests, passing token");
          passTokenToNeighbour();
          break;
        }

        if (!requestQueue.isEmpty()) {
          String request = requestQueue.take();
          mLogger.info("Processing request: " + request);
          String sendServer = sendOperationToServer(request);
          if (sendServer != null) {
            mLogger.info("Sending server: " + sendServer);
            processedRequests++;
          }
        }else {
          Thread.sleep(100);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        mLogger.warning("Request processing interrupted");
        break;
      } catch (Exception e) {
        mLogger.severe("Error processing request: " + e.getMessage());
      }
    }

    isProcessing = false;
    mLogger.info("Request processing finished");
  }


  private void connectToServer() {
    try {
      serverSocket = new Socket(InetAddress.getByName("localhost").getHostAddress(), SERVER_PORT);
      mLogger.info("Server socket created on port " + SERVER_PORT + " Host: " + InetAddress.getByName("localhost").getHostAddress());

      connectionPeerServer = new ConnectionPeer(mPeerInfo, nextPeerInfo, serverSocket, hasToken, mLogger, this);
      String register = "REGISTER " + mPeerInfo.getName() + " " + mPeerInfo.getPort() + " " + nextPeerInfo.getPort();
      connectionPeerServer.writer.println(register);
      connectionPeerServer.writer.flush();
      mLogger.info("Registered with server " + SERVER_PORT);
      connectionPeerServer.run();
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

  private void handleConnectionSend(String command) throws IOException {
    switch (command) {
      case "1":
        String message = Menu.handleOperation(scanner);
        if (hasToken) {
          sendOperationToServer(message);
        }
        break;
      case "2":
        if (hasToken) {
            passTokenToNeighbour();
        }
        break;
      case "3":
        running.set(false);
        System.out.println("Exiting...");
        break;
      case "help":
        Menu.helpMenu();
        break;
      default:
        System.out.println("Unknown command. Type 'help' for options.");
    }
  }

  private void acceptConnectionNeighbour() {
    connectionExecutor.submit(() -> {

      try (ServerSocket serverSocket = new ServerSocket(mPeerInfo.getPort())) {
        Thread.sleep(5000);
          mLogger.info("[INFO] Listening for peer connections on port " + nextPeerInfo.getPort());
        while (running.get()){
          Socket clientSocket = serverSocket.accept();
          System.out.println("[DETAILS] Accepted incoming connection from " + clientSocket.getRemoteSocketAddress());

          ConnectionPeer incomingConnection = new ConnectionPeer(mPeerInfo, nextPeerInfo, clientSocket, hasToken, mLogger, this);
          incomingConnection.run();
        }
      } catch (IOException e) {
        mLogger.warning("Error accepting neighbor connection: " + e.getMessage());
      } catch (InterruptedException e) {
          throw new RuntimeException(e);
      }
    });
    connectionExecutor.submit(this::connectionNeighbour);
  }

  private void connectionNeighbour() {
    connectionExecutor.submit(() -> {
      int retryCount = 0;
      int maxRetries = 10;
      long retryDelay = 2000;

      while (retryCount < maxRetries && running.get()) {
        try {
          System.out.println("Attempting to connect to the next peer...");
          Socket nextPeerSocket = new Socket(nextPeerInfo.getAddress(), nextPeerInfo.getPort());
          System.out.println("Connected to peer: " + nextPeerSocket.getRemoteSocketAddress());
          mLogger.info("Successfully connected to next peer at port " + nextPeerInfo.getPort());

          ConnectionPeer conP2 = new ConnectionPeer(mPeerInfo, nextPeerInfo, nextPeerSocket, hasToken, mLogger, this);
          connectionPeer = conP2;
          neighbourSocket = nextPeerSocket;
          conP2.run();
          break;
        } catch (Exception e) {
          retryCount++;
          mLogger.warning("Error connecting to next peer (attempt " + retryCount + "): " + e.getMessage());

          if (retryCount >= maxRetries) {
            mLogger.severe("Max retries reached. Unable to connect to next peer on port " + nextPeerInfo.getPort());
            break;
          }

          try {
            mLogger.info("Retrying connection in " + retryDelay + "ms...");
            Thread.sleep(retryDelay);
            retryDelay *= 2;
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            mLogger.warning("Retry process interrupted.");
            break;
          }
        }
      }
    });
  }



  private void passTokenToNeighbour() {
    mLogger.info("Passing token to the next peer...");
    if (neighbourSocket != null && !neighbourSocket.isClosed() && connectionPeer.writer != null) {
      connectionPeer.writer.println("TOKEN_HOLDER");
      connectionPeer.writer.flush();
      mLogger.info("Token passed to peer on port " + neighbourSocket.getPort());
    } else {
      mLogger.warning("Failed to pass token: socket or writer not properly initialized");
    }

    if (!serverSocket.isClosed() && connectionPeerServer.writer != null) {
      connectionPeerServer.writer.println("UPDATE_TOKEN");
      connectionPeerServer.writer.flush();
      mLogger.info("Message 'UPDATE_TOKEN' sent to server");
    }

    hasToken = false;
  }

  private String sendOperationToServer(String operation) {
    try (Socket socket = new Socket("localhost", SERVER_PORT);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

      out.println(operation);
      String response = in.readLine();
      if (response != null) {
        System.out.println("Server received: " + response);
        mLogger.info("Server received: " + response);
        return response;
      }
    } catch (IOException e) {
      if (e instanceof ConnectException) {
        System.out.println("Peer " + mPeerInfo.getName() + " failed to connect to server.");
        try {
          requestQueue.put(operation);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      } else {
        mLogger.warning("Error communicating with server: " + e.getMessage());
      }
    }
    return null;
  }

  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: java Peer <myPeerPort> <nextPeerPort>");
      System.exit(1);
    }

    int myPeerPort = Integer.parseInt(args[0]);
    int nextPeerPort = Integer.parseInt(args[1]);
    String name = "m" + getNumberName() + nextPeerPort;
    final String HOST_PEER = "localhost";
    PeerInfo peerInfo = new PeerInfo(name, myPeerPort, HOST_PEER);
    PeerInfo nextPeerInfo = new PeerInfo(name, nextPeerPort, HOST_PEER);
    PeerM peer = new PeerM(peerInfo, nextPeerInfo);
    peer.startPeer();
  }
}
