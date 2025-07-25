package in.hari.liveplay.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class AiPlayerRoom implements GameRoom {
    public final String roomId;
    public String userId;
    private final Map<String, WebSocketSession> player = new ConcurrentHashMap<>(1);

    private static final int SIZE = 3;
    private final String[][] gameBoard = new String[SIZE][SIZE];
    private String currentTurn;
    private boolean isGameOver;

    public AiPlayerRoom(String roomId) {
        this.roomId = roomId;
        this.currentTurn = "X";
        this.isGameOver = false;
    }

    public void addPlayer(String userId, WebSocketSession session) {
        this.userId = userId;
        this.player.put(userId, session);
    }

    public void replacePlayer(WebSocketSession session) {
        this.player.put(userId, session);
    }

    @Override
    public synchronized void handlePlayerMove(String userId, JsonNode move) throws IOException {
        if (isGameOver) return;

        if (!userId.equals(this.userId) || !"X".equals(currentTurn)) return;

        int row = move.get("row").asInt();
        int col = move.get("col").asInt();
        System.out.println("row: " + row + " col: " + col);

        if (gameBoard[row][col] != null) return;

        gameBoard[row][col] = "X";

        if (checkWinCondition("X")) {
            isGameOver = true;
            broadcast("You winn...!");
            return;
        }

        if (checkDraw()) {
            isGameOver = true;
            broadcast("Game is a draw!");
            return;
        }

        displayBoard(gameBoard, SIZE);
        switchTurn();
        aiMakeMove();
        displayBoard(gameBoard, SIZE);
    }

    private void aiMakeMove() throws IOException {
        int[] bestMove = findBestMove();
        if (bestMove[0] != -1) {
            gameBoard[bestMove[0]][bestMove[1]] = "O";
            broadcastMove(bestMove[0], bestMove[1]);

            if (checkWinCondition("O")) {
                isGameOver = true;
                broadcast("AI wins!");
            } else if (checkDraw()) {
                isGameOver = true;
                broadcast("Game is a draw!");
            } else {
                switchTurn();
            }
        }
    }

    @Override
    public boolean checkWinCondition(String userId) {
        for (int i = 0; i < 3; i++) {
            if (userId.equals(gameBoard[i][0]) && userId.equals(gameBoard[i][1]) && userId.equals(gameBoard[i][2])) return true;
            if (userId.equals(gameBoard[0][i]) && userId.equals(gameBoard[1][i]) && userId.equals(gameBoard[2][i])) return true;
        }
        return userId.equals(gameBoard[0][0]) && userId.equals(gameBoard[1][1]) && userId.equals(gameBoard[2][2])
                || userId.equals(gameBoard[0][2]) && userId.equals(gameBoard[1][1]) && userId.equals(gameBoard[2][0]);
    }

    @Override
    public boolean checkDraw() {
        for (String[] row : gameBoard)
            for (String cell : row)
                if (cell == null) return false;
        return true;
    }


    @Override
    public void switchTurn() {
        this.currentTurn = "X".equals(currentTurn) ? "O" : "X";
    }

    private int[] findBestMove() {
        int bestScore = Integer.MIN_VALUE;
        int[] move = {-1, -1};

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (gameBoard[i][j] == null) {
                    gameBoard[i][j] = "O";
                    int score = minimax(0, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    gameBoard[i][j] = null;
                    if (score > bestScore) {
                        bestScore = score;
                        move[0] = i;
                        move[1] = j;
                    }
                }
            }
        }
        return move;
    }

    private int minimax(int depth, boolean isMaximizing, int alpha, int beta) {
        if (checkWinCondition("O")) return 10 - depth;
        if (checkWinCondition("X")) return depth - 10;
        if (checkDraw()) return 0;

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    if (gameBoard[i][j] == null) {
                        gameBoard[i][j] = "O";
                        int eval = minimax(depth + 1, false, alpha, beta);
                        gameBoard[i][j] = null;
                        maxEval = Math.max(maxEval, eval);
                        alpha = Math.max(alpha, eval);
                        if (beta <= alpha) return maxEval;
                    }
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    if (gameBoard[i][j] == null) {
                        gameBoard[i][j] = "X";
                        int eval = minimax(depth + 1, true, alpha, beta);
                        gameBoard[i][j] = null;
                        minEval = Math.min(minEval, eval);
                        beta = Math.min(beta, eval);
                        if (beta <= alpha) return minEval;
                    }
                }
            }
            return minEval;
        }
    }

    @Override
    public void broadcast(String message) throws IOException {
        for (WebSocketSession session : player.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    public void broadcastMove(int row, int col) throws IOException {
        String json = String.format("{\"row\":%d,\"col\":%d }", row, col);
        for (WebSocketSession session : player.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        }
    }

}
