package tokenRing.server;

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
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(":");
        sb.append(port);
        sb.append(":");
        sb.append(address);
        return sb.toString();
    }
}
