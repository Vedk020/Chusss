import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;
import java.util.Optional;

public class Main extends Application {

    private static final int TILE_SIZE = 80;
    private static final int WIDTH = 8;
    private static final int HEIGHT = 8;

    private Tile[][] board = new Tile[HEIGHT][WIDTH];
    private String[][] piecePositions = new String[HEIGHT][WIDTH];
    private int selectedRow = -1, selectedCol = -1;
    private String currentPlayer = "w";
    private boolean gameOver = false;

    private Text gameStatusText = new Text();
    private Text timerText = new Text("10:00");

    private NetworkConnection connection;
    private boolean isMyTurn = false;

    private String myName = "Player";
    private String opponentName = "Opponent";

    private int whiteTotalTime = 600;  // Total time for White in seconds (10 minutes)
    private int blackTotalTime = 600;  // Total time for Black in seconds (10 minutes)
    private Timeline timer;

    @Override
    public void start(Stage primaryStage) {
        boolean isServer = askIfServer();
        isMyTurn = isServer;

        TextInputDialog nameDialog = new TextInputDialog(isServer ? "White" : "Black");
        nameDialog.setTitle("Enter Your Name");
        nameDialog.setHeaderText("You're playing as " + (isServer ? "White" : "Black"));
        nameDialog.setContentText("Enter your name:");
        Optional<String> nameResult = nameDialog.showAndWait();
        myName = nameResult.orElse(isServer ? "White" : "Black");

        if (isServer) {
            connection = new Server(this::receiveMove);
        } else {
            TextInputDialog ipDialog = new TextInputDialog("localhost");
            ipDialog.setTitle("Enter Server IP");
            ipDialog.setHeaderText("Connect to Server");
            ipDialog.setContentText("Enter server IP address:");
            Optional<String> ipResult = ipDialog.showAndWait();
            String ip = ipResult.orElse("localhost");
            connection = new Client(ip, this::receiveMove);
        }

        try {
            connection.start();

            // ✅ Wait until the connection is fully ready
            while (!connection.isReady()) {
                Thread.sleep(50); // Wait for the socket and streams to be initialized
            }

            // ✅ Now it’s safe to send
            connection.send("NAME " + myName);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set up the timer and display
        timerText.setFont(Font.font(24));
        GridPane grid = new GridPane();
        grid.add(timerText, WIDTH, 0); // Display the timer at top-right of the board

        gameStatusText.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        gameStatusText.setFill(Color.DARKRED);
        initializePieces();

        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {
                Tile tile = new Tile(row, col);
                board[row][col] = tile;
                grid.add(tile.getStack(), col, row);
            }
        }

        grid.add(gameStatusText, 0, HEIGHT, WIDTH, 1);
        updateBoard();
        gameStatusText.setText(isMyTurn ? myName + "'s turn" : opponentName + "'s turn");

        primaryStage.setTitle("Online Chess Game");
        primaryStage.setScene(new Scene(grid));
        primaryStage.show();

        // Initialize and start the timer
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimer()));
        timer.setCycleCount(Timeline.INDEFINITE); // Loop the timer until stopped
        timer.playFromStart();
    }

    private void updateTimer() {
        if (currentPlayer.equals("w")) {
            whiteTotalTime--;
        } else {
            blackTotalTime--;
        }

        // Update the timer display
        timerText.setText((currentPlayer.equals("w") ? "White: " : "Black: ") +
                formatTime(currentPlayer.equals("w") ? whiteTotalTime : blackTotalTime));

        // If time runs out, end the turn
        if (whiteTotalTime == 0 || blackTotalTime == 0) {
            gameStatusText.setText((currentPlayer.equals("w") ? "White's" : "Black's") + " time is up!");
            switchTurn();
        }
    }

    private String formatTime(int timeInSeconds) {
        int minutes = timeInSeconds / 60;
        int seconds = timeInSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private boolean askIfServer() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Choose Role");
        alert.setHeaderText("Start as Server or Client?");
        alert.setContentText("Click OK for Server, Cancel for Client.");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void switchTurn() {
        // Switch turn
        currentPlayer = opponentColor();
        gameStatusText.setText((currentPlayer.equals("w") ? myName : opponentName) + "'s turn");

        // Restart the timer for the next player (continue the total time)
        timer.stop();
        timer.playFromStart();
    }

    private void receiveMove(String msg) {
        Platform.runLater(() -> {
            if (msg.startsWith("NAME ")) {
                opponentName = msg.substring(5);
                gameStatusText.setText(currentPlayer.equals("w") ? myName : opponentName + "'s turn");
                return;
            }

            String[] parts = msg.split(" ");
            int fromRow = Integer.parseInt(parts[0]);
            int fromCol = Integer.parseInt(parts[1]);
            int toRow = Integer.parseInt(parts[2]);
            int toCol = Integer.parseInt(parts[3]);

            movePiece(fromRow, fromCol, toRow, toCol);
            updateBoard();

            if (isCheckmate(currentPlayer)) {
                gameOver = true;
                gameStatusText.setText(opponentColor().equals("w") ? myName + " wins!" : opponentName + " wins!");
            } else if (isInCheck(currentPlayer)) {
                gameStatusText.setText((currentPlayer.equals("w") ? myName : opponentName) + " is in check!");
            } else {
                gameStatusText.setText((currentPlayer.equals("w") ? myName : opponentName) + "'s turn");
            }

            currentPlayer = opponentColor();
            isMyTurn = true;
        });
    }

    private void initializePieces() {
        String[] backRow = {"R", "N", "B", "Q", "K", "B", "N", "R"};
        for (int i = 0; i < 8; i++) {
            piecePositions[0][i] = "b" + backRow[i];
            piecePositions[1][i] = "bP";
            piecePositions[6][i] = "wP";
            piecePositions[7][i] = "w" + backRow[i];
        }
    }

    private void updateBoard() {
        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {
                board[row][col].updatePiece(piecePositions[row][col]);
                board[row][col].rect.setStrokeWidth(0);
            }
        }
    }

    private void movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        piecePositions[toRow][toCol] = piecePositions[fromRow][fromCol];
        piecePositions[fromRow][fromCol] = null;
    }

    private class Tile {
        private int row, col;
        private StackPane stack;
        private Rectangle rect;
        private Text text;

        Tile(int row, int col) {
            this.row = row;
            this.col = col;
            stack = new StackPane();
            rect = new Rectangle(TILE_SIZE, TILE_SIZE);
            rect.setFill((row + col) % 2 == 0 ? Color.rgb(240, 217, 181) : Color.rgb(181, 136, 99));
            stack.getChildren().add(rect);

            text = new Text();
            text.setFont(Font.font(32));
            stack.getChildren().add(text);

            stack.setOnMouseClicked(this::handleClick);
        }

        void updatePiece(String piece) {
            if (piece == null) {
                text.setText("");
            } else {
                switch (piece.charAt(1)) {
                    case 'K' -> text.setText("♔");
                    case 'Q' -> text.setText("♕");
                    case 'R' -> text.setText("♖");
                    case 'B' -> text.setText("♗");
                    case 'N' -> text.setText("♘");
                    case 'P' -> text.setText("♙");
                }
                text.setFill(piece.charAt(0) == 'w' ? Color.WHITE : Color.BLACK);
            }
        }

        void handleClick(MouseEvent event) {
            if (!isMyTurn || gameOver) return;

            if (selectedRow == -1 && selectedCol == -1) {
                if (piecePositions[row][col] != null && piecePositions[row][col].charAt(0) == currentPlayer.charAt(0)) {
                    selectedRow = row;
                    selectedCol = col;
                    rect.setStroke(Color.YELLOW);
                    rect.setStrokeWidth(4);
                    highlightValidMoves(row, col);
                }
            } else {
                if (isValidMove(selectedRow, selectedCol, row, col, currentPlayer.charAt(0))) {
                    boolean legal = !isInCheck(currentPlayer) || canEscapeCheck(selectedRow, selectedCol, row, col);
                    if (legal) {
                        movePiece(selectedRow, selectedCol, row, col);
                        updateBoard();
                        try {
                            connection.send(selectedRow + " " + selectedCol + " " + row + " " + col);
                            isMyTurn = false;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (isCheckmate(opponentColor())) {
                            gameOver = true;
                            gameStatusText.setText(myName + " wins!");
                        } else if (isInCheck(opponentColor())) {
                            gameStatusText.setText(opponentName + " is in check!");
                        } else {
                            gameStatusText.setText(opponentName + "'s turn");
                        }

                        currentPlayer = opponentColor();
                    } else {
                        gameStatusText.setText(myName + " is in check!");
                    }
                }
                board[selectedRow][selectedCol].rect.setStrokeWidth(0);
                selectedRow = selectedCol = -1;
            }
        }

        StackPane getStack() {
            return stack;
        }
    }

    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, char color) {
        String piece = piecePositions[fromRow][fromCol];
        if (piece == null) return false;
        char type = piece.charAt(1);
        if (piecePositions[toRow][toCol] != null && piecePositions[toRow][toCol].charAt(0) == color) return false;

        return switch (type) {
            case 'K' -> isValidKingMove(fromRow, fromCol, toRow, toCol);
            case 'Q' -> isValidQueenMove(fromRow, fromCol, toRow, toCol);
            case 'R' -> isValidRookMove(fromRow, fromCol, toRow, toCol);
            case 'B' -> isValidBishopMove(fromRow, fromCol, toRow, toCol);
            case 'N' -> isValidKnightMove(fromRow, fromCol, toRow, toCol);
            case 'P' -> isValidPawnMove(fromRow, fromCol, toRow, toCol, color);
            default -> false;
        };
    }

    private boolean isValidKingMove(int r1, int c1, int r2, int c2) {
        return Math.abs(r1 - r2) <= 1 && Math.abs(c1 - c2) <= 1;
    }

    private boolean isValidQueenMove(int r1, int c1, int r2, int c2) {
        return isValidRookMove(r1, c1, r2, c2) || isValidBishopMove(r1, c1, r2, c2);
    }

    private boolean isValidRookMove(int r1, int c1, int r2, int c2) {
        if (r1 != r2 && c1 != c2) return false;
        return !isPathBlocked(r1, c1, r2, c2);
    }

    private boolean isValidBishopMove(int r1, int c1, int r2, int c2) {
        if (Math.abs(r1 - r2) != Math.abs(c1 - c2)) return false;
        return !isPathBlocked(r1, c1, r2, c2);
    }

    private boolean isValidKnightMove(int r1, int c1, int r2, int c2) {
        return (Math.abs(r1 - r2) == 2 && Math.abs(c1 - c2) == 1) ||
                (Math.abs(r1 - r2) == 1 && Math.abs(c1 - c2) == 2);
    }

    private boolean isValidPawnMove(int r1, int c1, int r2, int c2, char color) {
        int dir = color == 'w' ? -1 : 1;
        if (c1 == c2 && piecePositions[r2][c2] == null) {
            if ((color == 'w' && r1 == 6 && r2 == 4) || (color == 'b' && r1 == 1 && r2 == 3)) return true;
            return r2 == r1 + dir;
        } else if (Math.abs(c1 - c2) == 1 && r2 == r1 + dir) {
            return piecePositions[r2][c2] != null && piecePositions[r2][c2].charAt(0) != color;
        }
        return false;
    }

    private boolean isPathBlocked(int r1, int c1, int r2, int c2) {
        int rowStep = Integer.signum(r2 - r1);
        int colStep = Integer.signum(c2 - c1);
        int row = r1 + rowStep;
        int col = c1 + colStep;
        while (row != r2 || col != c2) {
            if (piecePositions[row][col] != null) return true;
            row += rowStep;
            col += colStep;
        }
        return false;
    }

    private boolean isInCheck(String color) {
        int kingRow = -1, kingCol = -1;
        for (int r = 0; r < HEIGHT; r++) {
            for (int c = 0; c < WIDTH; c++) {
                if (piecePositions[r][c] != null && piecePositions[r][c].equals(color + "K")) {
                    kingRow = r;
                    kingCol = c;
                    break;
                }
            }
        }

        for (int r = 0; r < HEIGHT; r++) {
            for (int c = 0; c < WIDTH; c++) {
                if (piecePositions[r][c] != null && piecePositions[r][c].charAt(0) != color.charAt(0)) {
                    if (isValidMove(r, c, kingRow, kingCol, piecePositions[r][c].charAt(0))) return true;
                }
            }
        }
        return false;
    }

    private boolean canEscapeCheck(int r1, int c1, int r2, int c2) {
        char color = piecePositions[r1][c1].charAt(0);
        String[][] backup = copyBoard();
        movePiece(r1, c1, r2, c2);
        boolean safe = !isInCheck(String.valueOf(color));
        piecePositions = backup;
        return safe;
    }

    private boolean isCheckmate(String color) {
        if (!isInCheck(color)) return false;
        for (int r1 = 0; r1 < HEIGHT; r1++) {
            for (int c1 = 0; c1 < WIDTH; c1++) {
                if (piecePositions[r1][c1] != null && piecePositions[r1][c1].charAt(0) == color.charAt(0)) {
                    for (int r2 = 0; r2 < HEIGHT; r2++) {
                        for (int c2 = 0; c2 < WIDTH; c2++) {
                            if (isValidMove(r1, c1, r2, c2, color.charAt(0)) &&
                                    canEscapeCheck(r1, c1, r2, c2)) return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private String[][] copyBoard() {
        String[][] copy = new String[HEIGHT][WIDTH];
        for (int i = 0; i < HEIGHT; i++) {
            System.arraycopy(piecePositions[i], 0, copy[i], 0, WIDTH);
        }
        return copy;
    }

    private String opponentColor() {
        return currentPlayer.equals("w") ? "b" : "w";
    }

    private void highlightValidMoves(int row, int col) {
        String piece = piecePositions[row][col];
        if (piece == null) return;
        char color = piece.charAt(0);
        for (int r = 0; r < HEIGHT; r++) {
            for (int c = 0; c < WIDTH; c++) {
                if (isValidMove(row, col, r, c, color)) {
                    board[r][c].rect.setStroke(Color.GREEN);
                    board[r][c].rect.setStrokeWidth(3);
                } else {
                    board[r][c].rect.setStrokeWidth(0);
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}