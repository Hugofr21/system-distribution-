package utils;

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

    public static void showMenu(boolean hasToken){
        System.out.println("\n #### Menu #### ");
        if (hasToken) {
            System.out.println("\n This peer has the token");
            System.out.print("1. Pass the token to the next peer.");
            System.out.println("2. Send operation to the server(add, sub, mul and div)");
        } else {
            System.out.println("\n This peer is waiting for the next peer.");
        }
        System.out.println("\n Enter your command:");

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

}
