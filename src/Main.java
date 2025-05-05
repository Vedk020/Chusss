import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
import javafx.geometry.Pos;
import javafx.scene.control.Label;

public class Main extends Application {

    private static int TILE_SIZE = 80;
    private static final int WIDTH = 8;
    private static final int HEIGHT = 8;

    private Tile[][] board = new Tile[HEIGHT][WIDTH];
    private String[][] piecePositions = new String[HEIGHT][WIDTH];
    private int selectedRow = -1, selectedCol = -1;
    private String currentPlayer = "w";
    private boolean gameOver = false;
    private String gameResult = "";
    private int whiteScore = 0;
    private int blackScore = 0;
    private boolean isServer = false;

    private Text gameStatusText = new Text();
    private Text timerText = new Text("10:00");

    private NetworkConnection connection;
    private boolean isMyTurn = false;

    private String myName = "Player";
    private String opponentName = "Opponent";

    private int whiteTotalTime = 600;  // Total time for White in seconds (10 minutes)
    private int blackTotalTime = 600;  // Total time for Black in seconds (10 minutes)
    private Timeline timer;

    private Button resignButton;
    private VBox controlPanel;
    private HBox timerPanel;
    private Text whiteTimerText;
    private Text blackTimerText;

    private int whitePoints = 0;
    private int blackPoints = 0;
    private Text whitePointsText;
    private Text blackPointsText;

    private List<String> capturedByWhite = new ArrayList<>();
    private List<String> capturedByBlack = new ArrayList<>();
    private VBox capturedWhiteBox;
    private VBox capturedBlackBox;

    @Override
    public void start(Stage primaryStage) {
        boolean isServer = askIfServer();
        this.isServer = isServer;
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

            // ✅ Now it's safe to send
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

        // Create control panel with buttons
        controlPanel = new VBox(10);
        controlPanel.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0;");

        resignButton = new Button("Resign");
        resignButton.setStyle("-fx-background-color: linear-gradient(to right, #f44336, #ff6a00); -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, #b0b0b0, 5, 0.2, 0, 2);");
        resignButton.setPrefWidth(120);
        resignButton.setOnAction(e -> handleResign());
        controlPanel.getChildren().clear();
        controlPanel.getChildren().addAll(resignButton);

        // Captured pieces display
        capturedWhiteBox = new VBox(5);
        capturedBlackBox = new VBox(5);
        capturedWhiteBox.setStyle("-fx-padding: 10; -fx-background-color: #fff; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, #b0b0b0, 5, 0.1, 0, 1);");
        capturedBlackBox.setStyle("-fx-padding: 10; -fx-background-color: #fff; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, #b0b0b0, 5, 0.1, 0, 1);");
        Label capturedWhiteLabel = new Label("White captured:");
        Label capturedBlackLabel = new Label("Black captured:");
        capturedWhiteLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        capturedBlackLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        capturedWhiteBox.getChildren().add(capturedWhiteLabel);
        capturedBlackBox.getChildren().add(capturedBlackLabel);
        VBox capturedPanel = new VBox(20, capturedWhiteBox, capturedBlackBox);
        capturedPanel.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0; -fx-border-radius: 10; -fx-background-radius: 10;");
        controlPanel.getChildren().add(capturedPanel);

        // Create timer panel
        timerPanel = new HBox(20);
        timerPanel.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0;");

        whiteTimerText = new Text("White: 10:00");
        blackTimerText = new Text("Black: 10:00");

        whiteTimerText.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        blackTimerText.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        timerPanel.getChildren().addAll(whiteTimerText, blackTimerText);

        // Create points panel
        HBox pointsPanel = new HBox(40);
        pointsPanel.setStyle("-fx-padding: 10; -fx-background-color: #e0e7ef; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, #b0b0b0, 10, 0.2, 0, 2);");
        pointsPanel.setAlignment(Pos.CENTER);
        whitePointsText = new Text("White Points: 0");
        blackPointsText = new Text("Black Points: 0");
        whitePointsText.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        blackPointsText.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        pointsPanel.getChildren().addAll(whitePointsText, blackPointsText);

        // Create main layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.setCenter(grid);
        mainLayout.setRight(controlPanel);
        VBox topPanel = new VBox(timerPanel, pointsPanel);
        topPanel.setStyle("-fx-background-color: linear-gradient(to right, #e0e7ef, #f8fafc);");
        mainLayout.setTop(topPanel);
        mainLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #e0e7ef, #f8fafc);");

        // Responsive resizing
        StackPane root = new StackPane(mainLayout);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Online Chess Game");
        primaryStage.show();

        // Listen for window size changes to resize tiles
        root.widthProperty().addListener((obs, oldVal, newVal) -> resizeBoard(grid, root));
        root.heightProperty().addListener((obs, oldVal, newVal) -> resizeBoard(grid, root));

        // Initialize and start the timer
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimer()));
        timer.setCycleCount(Timeline.INDEFINITE); // Loop the timer until stopped
        timer.playFromStart();
    }

    private void resizeBoard(GridPane grid, StackPane root) {
        double size = Math.min(root.getWidth(), root.getHeight() - 100) / WIDTH;
        TILE_SIZE = (int) Math.max(size, 20); // Minimum size
        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {
                board[row][col].resizeTile();
            }
        }
    }

    private void updateTimer() {
        if (!gameOver) {  // Only update timer if game is not over
            if (currentPlayer.equals("w")) {
                whiteTotalTime--;
            } else {
                blackTotalTime--;
            }

            // Update the timer display
            timerText.setText((currentPlayer.equals("w") ? "White: " : "Black: ") +
                    formatTime(currentPlayer.equals("w") ? whiteTotalTime : blackTotalTime));

            // If time runs out, end the game
            if (whiteTotalTime == 0 || blackTotalTime == 0) {
                gameOver = true;
                gameResult = (currentPlayer.equals("w") ? "Black" : "White") + " wins on time!";
                if (currentPlayer.equals("w")) {
                    blackScore += 1;
                } else {
                    whiteScore += 1;
                }
                timer.stop();
                showWinPage();
            }
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
            } else if (msg.equals("RESIGN")) {
                gameOver = true;
                gameResult = opponentName + " wins by resignation!";
                if (currentPlayer.equals("w")) {
                    blackScore += 1;
                } else {
                    whiteScore += 1;
                }
                timer.stop();
                showWinPage();
                return;
            } else if (msg.startsWith("CHECKMATE:")) {
                gameOver = true;
                String winner = msg.substring(10);
                if (winner.equals(myName)) {
                    gameResult = myName + " wins!";
                    whiteScore += currentPlayer.equals("w") ? 1 : 0;
                    blackScore += currentPlayer.equals("b") ? 1 : 0;
                } else {
                    gameResult = opponentName + " wins!";
                    whiteScore += currentPlayer.equals("b") ? 1 : 0;
                    blackScore += currentPlayer.equals("w") ? 1 : 0;
                }
                timer.stop();
                showWinPage();
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
                String winner = opponentColor().equals("w") ? myName : opponentName;
                gameResult = winner + " wins!";
                if (currentPlayer.equals("w")) {
                    blackScore += 1;
                } else {
                    whiteScore += 1;
                }
                timer.stop();
                showWinPage();
                // Notify opponent
                try {
                    connection.send("CHECKMATE:" + winner);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
        String captured = piecePositions[toRow][toCol];
        piecePositions[toRow][toCol] = piecePositions[fromRow][fromCol];
        piecePositions[fromRow][fromCol] = null;
        // Update points and captured pieces if a piece is captured
        if (captured != null) {
            if (captured.charAt(0) == 'w') {
                blackPoints += getPieceValue(captured);
                capturedByBlack.add(captured);
            } else {
                whitePoints += getPieceValue(captured);
                capturedByWhite.add(captured);
            }
            updatePointsDisplay();
            updateCapturedDisplay();
        }
    }

    private void updateCapturedDisplay() {
        capturedWhiteBox.getChildren().removeIf(node -> node instanceof Text);
        capturedBlackBox.getChildren().removeIf(node -> node instanceof Text);
        if (!capturedByWhite.isEmpty()) {
            Text t = new Text(capturedListToSymbols(capturedByWhite));
            t.setFont(Font.font("Arial", 22));
            t.setFill(Color.BLACK);
            capturedWhiteBox.getChildren().add(t);
        }
        if (!capturedByBlack.isEmpty()) {
            Text t = new Text(capturedListToSymbols(capturedByBlack));
            t.setFont(Font.font("Arial", 22));
            t.setFill(Color.BLACK);
            capturedBlackBox.getChildren().add(t);
        }
    }

    private String capturedListToSymbols(List<String> captured) {
        StringBuilder sb = new StringBuilder();
        for (String piece : captured) {
            switch (piece.charAt(1)) {
                case 'K': sb.append("♔"); break;
                case 'Q': sb.append("♕"); break;
                case 'R': sb.append("♖"); break;
                case 'B': sb.append("♗"); break;
                case 'N': sb.append("♘"); break;
                case 'P': sb.append("♙"); break;
            }
        }
        return sb.toString();
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
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(0.5);
            stack.getChildren().add(rect);

            text = new Text();
            text.setFont(Font.font("Arial", FontWeight.BOLD, 32));
            stack.getChildren().add(text);

            stack.setOnMouseClicked(this::handleClick);
            stack.setOnMouseEntered(e -> {
                if (!gameOver && piecePositions[row][col] != null) {
                    rect.setStroke(Color.YELLOW);
                    rect.setStrokeWidth(2);
                }
            });
            stack.setOnMouseExited(e -> {
                if (selectedRow != row || selectedCol != col) {
                    rect.setStroke(Color.BLACK);
                    rect.setStrokeWidth(0.5);
                }
            });
        }

        void resizeTile() {
            rect.setWidth(TILE_SIZE);
            rect.setHeight(TILE_SIZE);
            text.setFont(Font.font("Arial", FontWeight.BOLD, TILE_SIZE * 0.4));
        }

        void updatePiece(String piece) {
            if (piece == null) {
                text.setText("");
            } else {
                switch (piece.charAt(1)) {
                    case 'K': text.setText("♔"); break;
                    case 'Q': text.setText("♕"); break;
                    case 'R': text.setText("♖"); break;
                    case 'B': text.setText("♗"); break;
                    case 'N': text.setText("♘"); break;
                    case 'P': text.setText("♙"); break;
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
                            String winner = currentPlayer.equals("w") ? myName : opponentName;
                            gameResult = winner + " wins!";
                            if (currentPlayer.equals("w")) {
                                blackScore += 1;
                            } else {
                                whiteScore += 1;
                            }
                            timer.stop();
                            showWinPage();
                            // Notify opponent
                            try {
                                connection.send("CHECKMATE:" + winner);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
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

        boolean result = false;
        switch (type) {
            case 'K': result = isValidKingMove(fromRow, fromCol, toRow, toCol); break;
            case 'Q': result = isValidQueenMove(fromRow, fromCol, toRow, toCol); break;
            case 'R': result = isValidRookMove(fromRow, fromCol, toRow, toCol); break;
            case 'B': result = isValidBishopMove(fromRow, fromCol, toRow, toCol); break;
            case 'N': result = isValidKnightMove(fromRow, fromCol, toRow, toCol); break;
            case 'P': result = isValidPawnMove(fromRow, fromCol, toRow, toCol, color); break;
            default: result = false;
        }
        return result;
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

    private void updateTimerDisplay() {
        whiteTimerText.setText("White: " + formatTime(whiteTotalTime));
        blackTimerText.setText("Black: " + formatTime(blackTotalTime));
        // Highlight active player's timer and points
        if (currentPlayer.equals("w")) {
            whiteTimerText.setFill(Color.RED);
            blackTimerText.setFill(Color.BLACK);
            whitePointsText.setFill(Color.RED);
            blackPointsText.setFill(Color.BLACK);
        } else {
            whiteTimerText.setFill(Color.BLACK);
            blackTimerText.setFill(Color.RED);
            whitePointsText.setFill(Color.BLACK);
            blackPointsText.setFill(Color.RED);
        }
    }

    private void handleResign() {
        if (!gameOver) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Resign Game");
            alert.setHeaderText("Confirm Resignation");
            alert.setContentText("Are you sure you want to resign?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    connection.send("RESIGN");
                    gameOver = true;
                    gameResult = opponentName + " wins by resignation!";
                    if (currentPlayer.equals("w")) {
                        blackScore += 1;
                    } else {
                        whiteScore += 1;
                    }
                    timer.stop();
                    showWinPage();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void showWinPage() {
        // Create a new stage for the win page
        Stage winStage = new Stage();
        winStage.setTitle("Game Over");

        // Create the main layout
        VBox mainLayout = new VBox(20);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle("-fx-padding: 20; -fx-background-color: #f0f0f0;");

        // Game result text
        Text resultText = new Text(gameResult);
        resultText.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        resultText.setFill(Color.DARKRED);

        // Scorecard
        VBox scorecard = new VBox(10);
        scorecard.setAlignment(Pos.CENTER);
        scorecard.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-border-radius: 10;");

        Text whiteScoreText = new Text("White (" + myName + "): " + whiteScore);
        Text blackScoreText = new Text("Black (" + opponentName + "): " + blackScore);
        whiteScoreText.setFont(Font.font("Arial", 16));
        blackScoreText.setFont(Font.font("Arial", 16));

        scorecard.getChildren().addAll(whiteScoreText, blackScoreText);

        // Restart button
        Button restartButton = new Button("New Game");
        restartButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16px;");
        restartButton.setPrefWidth(200);
        restartButton.setOnAction(e -> {
            winStage.close();
            resetGame();
        });

        mainLayout.getChildren().addAll(resultText, scorecard, restartButton);

        // Show the win page
        winStage.setScene(new Scene(mainLayout, 400, 300));
        winStage.show();
    }

    private void resetGame() {
        // Reset game state
        gameOver = false;
        gameResult = "";
        whiteTotalTime = 600;
        blackTotalTime = 600;
        currentPlayer = "w";
        isMyTurn = isServer;
        whitePoints = 0;
        blackPoints = 0;
        capturedByWhite.clear();
        capturedByBlack.clear();
        updatePointsDisplay();
        updateCapturedDisplay();
        
        // Reset board
        for (int i = 0; i < HEIGHT; i++) {
            Arrays.fill(piecePositions[i], null);
        }
        initializePieces();
        updateBoard();
        
        // Reset UI
        gameStatusText.setText(isMyTurn ? myName + "'s turn" : opponentName + "'s turn");
        updateTimerDisplay();
        
        // Enable buttons
        resignButton.setDisable(false);
        
        // Restart timer
        timer.stop();
        timer.playFromStart();
    }

    private void updatePointsDisplay() {
        whitePointsText.setText("White Points: " + whitePoints);
        blackPointsText.setText("Black Points: " + blackPoints);
    }

    private int getPieceValue(String piece) {
        if (piece == null) return 0;
        switch (piece.charAt(1)) {
            case 'P': return 1;
            case 'N':
            case 'B': return 3;
            case 'R': return 5;
            case 'Q': return 9;
            default: return 0;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}