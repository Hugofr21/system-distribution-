package antiEntropy.network;

import tokenRing.server.PeerInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Neighbour {
    private final Map<String, PeerInfo> neighbours;
    private String currentPeer;

    public Neighbour() {
      this.neighbours = new HashMap<String,PeerInfo>();
      this.currentPeer = null;
    }

    public Map<String,PeerInfo> getNeighbours() {
        if(!neighbours.isEmpty())
        {
            return neighbours;
        }
        System.out.println("Neighbours is empty");
        return null;
    }

    public void update(String name, int port,String host){
        PeerInfo peerInfo = new PeerInfo(name ,port, host);
        neighbours.put(name, peerInfo);
        System.out.println("Added neighbor: " + name + " at " + host + ":" + port);
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

    public void remove(String host){
        if(neighbours.containsKey(host)){
            neighbours.remove(host);
            if(host.equals(currentPeer)){
                currentPeer = null;
            }
        }
    }


}
