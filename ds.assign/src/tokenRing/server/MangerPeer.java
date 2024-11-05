package tokenRing.server;

import java.util.ArrayList;
import java.util.List;

public class MangerPeer {
    private List<PeerInfo> peers;
    private int countPeers = 0;
    private PeerInfo currentPeerInfoToToken;

    public MangerPeer() {
        this.peers = new ArrayList<>();
    }

    public void addPeerNetwork (String host, int port) {
        countPeers++;
        String name = "m" + countPeers;
        PeerInfo newPeer = new PeerInfo(name, port, host);
        peers.add(newPeer);
        System.out.println("Added peer " + host + ":" + port + " to " + name);
    }

    public void removePeerNetwork (String host, int port) {
        peers.removeIf(peer -> peer.getAddress().equals(host) && peer.getPort() == port);
        System.out.println("Removed peer " + host + ":" + port);
    }

    public List<PeerInfo> getPeers() {
        return peers;
    }

    public PeerInfo receivedTokenPeer(){
        if (peers.isEmpty()) {
            System.out.println("No peers");
            return null;
        }

        int currentIndex = peers.indexOf(currentPeerInfoToToken);

        int nextIndex = (currentIndex + 1) % peers.size();
        PeerInfo next = peers.get(nextIndex);

        currentPeerInfoToToken(next);
        return next;
    }

    public void currentPeerInfoToToken(PeerInfo next) {
        this.currentPeerInfoToToken = next;
        System.out.println("Current peer info to token: " + next.getAddress() + ":" + next.getPort());
    }


}
