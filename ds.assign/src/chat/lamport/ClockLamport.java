package chat.lamport;

public class ClockLamport {
    private int time;

    public ClockLamport(int time) {
        this.time = time;
    }
    public int getTime() {
        return time;
    }

    public synchronized void setTime(int recTime) {
        this.time = Math.max(time, recTime) + 1;
    }

    public synchronized void inc(){
        this.time++;
    }
}
