package assign.p2p.neigbour;


import assign.p2p.network.ConnectionPeer;
import assign.p2p.network.PeerInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Neighbour {
    private final Map<String, Pair<PeerInfo, ConnectionPeer>> neighbours;
    private String currentPeer;
    private static final int MAX_PEERS = 6;

    public Neighbour() {
      this.neighbours = new HashMap<>();
      this.currentPeer = null;
    }

    public int sizeNeighbours() {
        return this.neighbours.size();
    }

    public Map<String,PeerInfo> getNeighbours() {
        Map<String,PeerInfo> peers = new HashMap<>();

        for (Map.Entry<String, Pair<PeerInfo, ConnectionPeer>> entry : neighbours.entrySet()) {
            peers.put(entry.getKey(), entry.getValue().getFirst());
        }

        return peers.isEmpty() ? null : peers;
    }

    public Pair<PeerInfo, ConnectionPeer> getPairNeighbourByKey(String key) {
        return neighbours.get(key);
    }


    public void saveAndUpdate(String name, int port,String host){
        if (neighbours.containsKey(String.valueOf(port))) {
            System.out.println("Neighbor already exists on port: " + port);
            return;
        }

        if (neighbours.size() >= MAX_PEERS) {
            System.out.println("Peer is too full");
            return;
        }

        PeerInfo peerInfo = new PeerInfo(name ,port, host);
        neighbours.put(String.valueOf(port),new Pair<>(peerInfo, null));
        System.out.println("Added neighbor: " + name + " at " + host + " Port: " + port);
    }


    public void updateConnection(int port, ConnectionPeer connectionPeer){
        String portKey = String.valueOf(port);
        Pair<PeerInfo, ConnectionPeer> pair = neighbours.get(portKey);
        if (pair == null) {
            System.out.println("Neighbor does not exist " + portKey);
            return;
        }
        neighbours.put(portKey, new Pair<>(pair.getFirst(), connectionPeer));
        System.out.println("Neighbor updated " + portKey + " to " + connectionPeer);
    }

    public ConnectionPeer getConnectionPeerByPort(int port){
        Pair<PeerInfo,ConnectionPeer> neighbourInfo = neighbours.get(String.valueOf(port));
        return neighbourInfo != null ? neighbourInfo.getSecond() : null;
    }

    public String getCurrentPeer() {
        if (neighbours.isEmpty()){
            System.out.println("Current peer not found  " + currentPeer);
            return null;
        }

        Set<String> keys = neighbours.keySet();
        Iterator<String> iter = keys.iterator();

        if (currentPeer == null && iter.hasNext()){
            String nextPeer = iter.next();
            if (nextPeer.equals(currentPeer) && iter.hasNext()){
                currentPeer = iter.next();
                return currentPeer;
            }
        }

        currentPeer = keys.iterator().next();
        return currentPeer;

    }

    public void remove(int port){
        String portKey = String.valueOf(port);
        if(neighbours.containsKey(portKey)){
            neighbours.remove(portKey);
            if(portKey.equals(currentPeer)){
                currentPeer = null;
            }
        }
    }


    public boolean isEmpty(){
        return neighbours.isEmpty();
    }

    public boolean removeEntries(long currentTime, long peerTimeout) {
        boolean removed = false;
        Iterator<Map.Entry<String, Pair<PeerInfo, ConnectionPeer>>> iter = neighbours.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Pair<PeerInfo, ConnectionPeer>> entry = iter.next();
            PeerInfo peerInfo = entry.getValue().getFirst();
            if ((currentTime - peerInfo.getLastUpdated()) > peerTimeout) {
                iter.remove();
                if (entry.getKey().equals(currentPeer)) {
                    currentPeer = null;
                }
                removed = true;
            }
        }
        return removed;
    }

    @Override
    public String toString() {
        if (neighbours.isEmpty()) {
            return "No neighbours available.";
        }

        StringBuilder builder = new StringBuilder("Neighbours List:\n");
        builder.append("Total neighbours: ").append(neighbours.size()).append("\n");

        for (Map.Entry<String, Pair<PeerInfo, ConnectionPeer>> entry : neighbours.entrySet()) {
            builder.append(entry.getValue().getFirst().toString()).append("\n");
        }
        return builder.toString();
    }

}
