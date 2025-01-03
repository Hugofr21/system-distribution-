package assign.ring.utils;

import java.util.Random;
import java.util.Scanner;

final public class Menu {

    public static  void helpMenu(){
        System.out.println("\n #### Menu #### ");
        System.out.println("Available commands:");
        System.out.println("REGISTER <port> <neighbourPort>");
        System.out.println(" token - Pass the token (if you have it): TOKEN_HOLDER <peerPort>");
        System.out.println("  add <num1> <num2> - Add two numbers");
        System.out.println("  sub <num1> <num2> - Subtract two numbers");
        System.out.println("  mul <num1> <num2> - Multiply two numbers");
        System.out.println("  div <num1> <num2> - Divide two numbers");
        System.out.println("  quit - Exit the program");
    }

    public static String handleOperation(Scanner scanner){
        System.out.println("\n #### Operation #### ");
        System.out.println("\n chosen operation: add ,sub, multi and div");
        String operation = scanner.nextLine().trim();
        System.out.println("\n Enter first number: ");
        String num1 = scanner.nextLine().trim();
        System.out.println("\n Enter second number: ");
        String num2 = scanner.nextLine().trim();

        return operation + " " + num1 + " " + num2;
    }

    // Poisson distribution for request generation with average frequency of 4 events per minute
    public static void generatePoissonDistribution(Random random) throws InterruptedException {
        double LAMBDA = 4.0;
        double nextArrival = -Math.log(1.0 - random.nextDouble()) / LAMBDA;
        long sleepTime = (long) (nextArrival * 20000);
        Thread.sleep(sleepTime);
    }

    // Generate a random operation and arguments
    public static String generateRandomOperation(Random random, String[] operationsCalculator) {
        String operation = operationsCalculator[random.nextInt(operationsCalculator.length)];
        int num1 = random.nextInt(10);
        int num2 = random.nextInt(10);

        if (operation.equals("div") && num2 == 0) {
            num2 = 1;
        }

        return operation + " " + num1 + " " + num2;
    }


}
