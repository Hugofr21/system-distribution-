package assign.ring.server;

public class PeerInfo {
    private String name;
    private int port;
    private String address;

    public PeerInfo(String name, int port, String address) {
        this.name = name;
        this.port = port;
        this.address = address;
    }
    public String getName() {
        return name;
    }
    public int getPort() {
        return port;
    }
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name +
                ":" +
                port +
                ":" +
                address;
    }
}
