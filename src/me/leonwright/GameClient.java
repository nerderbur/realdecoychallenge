package me.leonwright;

import java.awt.Font;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Scanner;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class GameClient {
    private Socket socket;
    private Scanner in;
    private PrintWriter out;
    private Scanner scanner;

    public GameClient(String serverAddress) throws Exception {
        socket = new Socket(serverAddress, 7621);
        in = new Scanner(socket.getInputStream());
        out = new PrintWriter(socket.getOutputStream(), true);
        scanner = new Scanner(System.in);
    }

    public void play(GameClient client) throws Exception {
        System.out.println("Login Successful! Waiting for opponent to join...");

        try {
            while (in.hasNextLine()) {
                var response = in.nextLine();
                System.out.println(response);

                if (response.startsWith("ROLE")) {
                    Utils.clearConsole();
                    System.out.println("Find the Queen is starting...");
                    if (response.substring(4).trim().equals("dealer")) {
                        dealerFlow();
                    } else if (response.substring(4).trim().equals("spotter")) {
                        spotterFlow(false);
                    }
                } else if (response.startsWith("VICTORY")) {
                    System.out.println("Victory! You have won this battle!");
                } else if (response.startsWith("DEFEAT")) {
                    System.out.println("Defeat! Oh no, better luck next time :(");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the login flow
     */
    private void loginFlow() {
        System.out.println("You must log in before you can play!\n");

        System.out.println("Username:");
        String username = scanner.nextLine();

        System.out.println("Password:");
        String password = scanner.nextLine();

        out.println("AUTH username="+username+",password="+password);

        var response = in.nextLine();
        if (response.startsWith("AUTH_VALID")) {
            var authStatus = response.substring(10).trim();

            // If login fails for any reason it is retried.
            if (authStatus.equals("user_loggged_in")){
                Utils.clearConsole();
                System.out.println("Error! User already logged in. Please try again.");
                this.loginFlow();
            } else if (authStatus.equals("false")) {
                Utils.clearConsole();
                System.out.println("Error! Invalid login details. Please try again.");
                this.loginFlow();
            }
        }

    }

    /**
     * Run the flow for the dealer role
     */
    private void dealerFlow() {
        System.out.println("Where would you like to place the Queen? (Choose from 1-3)");

        if (scanner.hasNextInt()) {
            int response = scanner.nextInt();

            if (response > 3) {
                dealerFlow();
                return;
            }
            out.println("PLACE_QUEEN " + response);
        } else {
            System.out.println("Sorry, couldn't understand you!");
            scanner.nextLine();
            dealerFlow();
            return;
        }

        System.out.println("Waiting on opponent to move...");
    }

    /**
     * Run the flow for the spotter role
     * @param retry - Set to true if this flow failed originally due to validation errors.
     */
    private void spotterFlow(boolean retry) {
        System.out.println("Waiting on opponent to move...");

        if (retry) {
            queryForQueenSpot();
        } else {
            if (in.nextLine().startsWith("QUEEN_PLACED")) {
                queryForQueenSpot();
            }
        }
    }

    private void queryForQueenSpot() {
        System.out.println("Where do you think the Queen is? (Choose from 1-3)");
        if (scanner.hasNextInt()) {
            int response = scanner.nextInt();

            if (response > 3) {
                spotterFlow(true);
                return;
            }

            out.println("GUESS " + response);
        } else {
            System.out.println("Sorry, couldn't understand you!");
            scanner.nextLine();
            spotterFlow(true);
        }
    }

    public void init() {
        try {
            loginFlow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Pass the server IP as the sole command line argument");
            return;
        }
        GameClient client = new GameClient(args[0]);
        client.init();
        client.play(client);
    }
}