package assign.ring.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MangerPeer {
    private final List<PeerInfo> lPeers;
    private final Map<PeerInfo, PeerInfo> mapPeerPairInfo;
    private PeerInfo currentPeerInfoToToken;

    public MangerPeer() {
        this.mapPeerPairInfo = new HashMap<PeerInfo, PeerInfo>();
        this.lPeers = new ArrayList<>();
        this.currentPeerInfoToToken = null;
    }

    public Map<PeerInfo,PeerInfo> getPairPeers() {
        return this.mapPeerPairInfo;
    }

    public void addPeerNetwork (PeerInfo currentPeerInfo, PeerInfo neigbourPeerInfo) {
        lPeers.add(currentPeerInfo);
        System.out.println("Added peer " + currentPeerInfo.getAddress() + ":" + currentPeerInfo.getPort() + " to " + currentPeerInfo.getName());
        mapPeerPairInfo.put(currentPeerInfo, neigbourPeerInfo);
        printPeerPais();
    }

    public void removePeerNetwork (String host, int port) {
        lPeers.removeIf(peer -> peer.getAddress().equals(host) && peer.getPort() == port);
        System.out.println("Removed peer " + host + ":" + port);
    }

    public List<PeerInfo> getPeers() {
        return lPeers;
    }

    public PeerInfo receivedTokenPeer(){
        if (lPeers.isEmpty()) {
            System.out.println("No peers");
            return null;
        }

        if (currentPeerInfoToToken == null) {
            currentPeerInfoToToken = lPeers.getFirst();
            System.out.println("Peer " + currentPeerInfoToToken.getAddress() + ":" + currentPeerInfoToToken.getName());
        }
        PeerInfo nextPeer = mapPeerPairInfo.get(currentPeerInfoToToken);

        if (nextPeer == null) {
            System.out.println("No peers");
            return null;
        }

        currentPeerInfoToToken = nextPeer;
        System.out.println("Peer " + currentPeerInfoToToken.getAddress() + ":" + currentPeerInfoToToken.getName());
        return nextPeer;
    }

    public void currentPeerInfoToToken(PeerInfo next) {
        this.currentPeerInfoToToken = next;
        System.out.println("Current peer info to token: " + next.getAddress() + ":" + next.getPort());
    }

    public PeerInfo getCurrentPeerInfoToToken() {
        return currentPeerInfoToToken;
    }

    private void printPeerPais() {
        System.out.println("Peers:");
        for (Map.Entry<PeerInfo, PeerInfo> entry : mapPeerPairInfo.entrySet()) {
            PeerInfo peer = entry.getKey();
            PeerInfo peerInfo = entry.getValue();
            System.out.println("\t" + "Key: " + peer.getName() + " : " + peer.getAddress() + " : " + peer.getPort());
            System.out.println("\t" + "Value" + peerInfo.getName() + " : " + peerInfo.getAddress() + ":" + peerInfo.getPort());
        }

    }

    public PeerInfo getPeerByPort(int port) {
        return lPeers.stream()
                .filter(p -> p.getPort() == port)
                .findFirst()
                .orElse(null);
    }



}
