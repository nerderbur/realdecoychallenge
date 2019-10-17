package me.leonwright;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;

/**
 * Client -> Server
 * GUESS <integer>
 * AUTH ("username=<string>,password=<string>")
 * PLACE_QUEEN
 * QUIT
 *
 * Server -> Client
 * LOGIN
 * AUTH_VALID <boolean>
 * ROLE <string>
 *
 */

public class GameServer {
    public static void main(String[] args) {
        try (var listener = new ServerSocket(7621)) {
            System.out.println("The Find The Queen server is running...");
            var pool = Executors.newFixedThreadPool(200);
            while (true) {
                Game game = new Game();
                pool.execute(game.new Player(listener.accept()));
                pool.execute(game.new Player(listener.accept()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Game {
    HashMap<String, Player> players = new HashMap<String, Player>();
    Player currentPlayer;
    Integer currentRound = 0;
    Integer currentQueenSpot;
    AuthService authService = new AuthService();
    Player winner;

    private Player getWinner() {
        players.forEach((s, player) -> {
            if (winner == null) {
                winner = player;
            } else if (player.points > winner.points) {
                winner = player;
            }
        });
        return winner;
    }

    private boolean roundsDone() {
        return currentRound >= 5;
    }

    private synchronized void setQueenSpot(Integer spot, Player player) {
        if (player.isDealer()) {
            currentQueenSpot = spot;
        }
    }

    private synchronized void guess(Integer guess, Player player) {
        System.out.println(currentQueenSpot);
        if (!player.isDealer) {
            if (currentQueenSpot != null && guess.equals(currentQueenSpot)) {
                player.output.println("ROUND_WON");
                player.points++;
                player.opponent.output.println("ROUND_LOST");
            } else {
                player.output.println("ROUND_LOST");
                player.opponent.points++;
                player.opponent.output.println("ROUND_WON");
            }
            currentRound++;
        }
    }

    class Player implements Runnable {
        Player opponent;
        Socket socket;
        String username;
        Scanner input;
        PrintWriter output;
        Integer points = 0;
        boolean isDealer = false;

        public boolean isDealer() {
            return isDealer;
        }

        public void setDealer(boolean dealer) {
            isDealer = dealer;
        }

        public Player(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                setup();
                processCommands();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (opponent != null && opponent.output != null) {
                    opponent.output.println("OTHER_PLAYER_LEFT");
                    players.remove(opponent.username);
                }
                try {socket.close();} catch (IOException e) {}
            }
        }

        private void setup() throws IOException {
            input = new Scanner(socket.getInputStream());
            output = new PrintWriter(socket.getOutputStream(), true);

        }

        private void processCommands() {
            while (input.hasNextLine()) {
                var command = input.nextLine();
                System.out.println(command);
                if (command.startsWith("QUIT")) {
                    return;
                } else if (command.startsWith("GUESS")) {
                    processGuessCommand(command.substring(5));
                } else if (command.startsWith("AUTH")) {
                    processLoginCommand(command.substring(4));
                } else if (command.startsWith("PLACE_QUEEN")) {
                    processPlaceQueenCommand(command.substring(11));
                }
            }
            players.remove(this.username);
            System.out.println(players.size());

        }

        private  void processPlaceQueenCommand(String data) {
            data = data.trim();

            setQueenSpot(Integer.parseInt(data), this);

            this.opponent.output.println("QUEEN_PLACED");
        }

        private void processLoginCommand(String data) {
            data = data.trim();
            String username = data.split(",")[0];
            String password = data.split(",")[1];

            username = username.split("=")[1];
            password = password.split("=")[1];

            if (authService.login(username, password)) {
                // Checking if user is already logged in...
                if (players.containsKey(username)) {
                    output.println("AUTH_VALID " + "user_loggged_in");
                } else {
                    players.put(username, this);
                    output.println("AUTH_VALID " + "true");
                    System.out.println(players.size());
                    if (players.size() >= 2) {
                        // Selecting a random dealer...
                        Object[] playerKeys = players.keySet().toArray();
                        Object key = playerKeys[new Random().nextInt(playerKeys.length)];

                        // Assigning dealer
                        Player dealer = players.get(key.toString());
                        dealer.setDealer(true);
                        dealer.output.println("ROLE dealer");
                        this.username = username;
                        players.forEach((id, player) -> {
                            if (!player.equals(dealer)) {
                                player.output.println("ROLE spotter");
                                player.opponent = dealer;
                                dealer.opponent = player;
                            }
                        });
                    }
                }
            } else {
                output.println("AUTH_VALID " + "false");
            }
        }

        private void processGuessCommand(String guess) {
            guess = guess.trim();
            Integer guessInt = Integer.parseInt(guess);

            try {
                guess(guessInt, this);
                output.println("VALID_GUESS");
                opponent.output.println("OPPONENT_GUESSED " + guess);

                currentQueenSpot = null;

                System.out.println(currentRound);

                if (roundsDone()) {
                    getWinner().output.println("VICTORY");
                    getWinner().opponent.output.println("DEFEAT");
                } else {
                    players.forEach((id, player) -> {
                        if (player.isDealer) {
                            player.isDealer = false;
                            player.output.println("ROLE spotter");
                        } else {
                            player.isDealer = true;
                            player.output.println("ROLE dealer");
                        }
                    });
                }
            } catch (IllegalStateException e) {
                output.println("MESSAGE " + e.getMessage());
            }
        }
    }
}
