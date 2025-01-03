package assign.tom.utils;

import java.util.Random;

final public class Utils {
    private static final Random rand  = new Random();
    private final static double LAMBDA = 0.1;


    public static double getPoissonDistributionInterval(){
       return  -Math.log(1.0 - rand.nextDouble() / LAMBDA);
    }

    public static double getPoissonDistributionIntervalMillis(){
        return  (long)(getPoissonDistributionInterval() * 1000);
    }

    public static void helpMenu(){
        System.out.println("Enter the peer's name to connect (or 'quit' to close): ");
        System.out.println("\nMenu:");
        System.out.println("1. Connect to a peer");
        System.out.println("2. Multicast chat");
        System.out.println("3. Show list of words");
        System.out.println("4. Show list of neighbors");
        System.out.println("5. Quit");
        System.out.print("Enter your choice: ");
    }

}
