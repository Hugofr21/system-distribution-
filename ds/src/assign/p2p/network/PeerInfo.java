package assign.p2p.network;

import java.util.Objects;

public class PeerInfo {
    private String name;
    private int port;
    private String address;
    private long lastUpdated;

    public PeerInfo(String name, int port, String address) {
        this.name = name;
        this.port = port;
        this.address = address;
        this.lastUpdated = System.currentTimeMillis();
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

    public long getLastUpdated() {
        return lastUpdated;
    }
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PeerInfo peerInfo = (PeerInfo) obj;
        return port == peerInfo.port && Objects.equals(name, peerInfo.name) && Objects.equals(address, peerInfo.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, port, address);
    }

    @Override
    public String toString() {
        return "PeerInfo{ name='" + name + "', port=" + port + ", ipAddress='" + address + "'LastUpdated='" + lastUpdated + "' }";
    }
}
