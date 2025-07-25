package in.hari.liveplay.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class TwoPlayerRoom implements GameRoom {

    private final String roomId;
    private final Map<String, WebSocketSession> players = new ConcurrentHashMap<>(2);
    private final Map<String, Character> playerSymbols = new HashMap<>();

    private static final int SIZE = 3;
    private String[][] gameBoard = new String[SIZE][SIZE];
    private String currentTurn;
    private boolean isGameOver;

    public TwoPlayerRoom(String roomId) {
        this.roomId = roomId;
    }

    @Override
    public void addPlayer(String userId, WebSocketSession session) {
        players.put(userId, session);
        if (players.size() == 1) {
            playerSymbols.put(userId, 'X');
            currentTurn = userId;
        } else if (players.size() == 2) {
            for (String existingUserId : players.keySet()) {
                if (!existingUserId.equals(userId)) {
                    playerSymbols.put(userId, 'O');
                    break;
                }
            }
        }
    }

    public void rejoinRoom(String userId, WebSocketSession session) {
        players.put(userId, session);
    }

    public boolean hasPlayer(String userId) {
        return players.containsKey(userId);
    }

    public boolean isFull() {
        return players.size() == 2;
    }

    public Collection<WebSocketSession> getAllSessions() {
        return players.values();
    }

    private String getSymbolForUser(String userId) {
        Character symbol = playerSymbols.get(userId);
        return symbol != null ? symbol.toString() : "?";
    }

    @Override
    public synchronized void handlePlayerMove(String userId, JsonNode move) throws IOException {
        int row = move.get("row").asInt();
        int col = move.get("col").asInt();

        WebSocketSession session = players.get(userId);

        if (isGameOver) {
            session.sendMessage(new TextMessage("Game is already over."));
            return;
        }

        if (gameBoard[row][col] != null) {
            session.sendMessage(new TextMessage("Invalid move, cell already taken."));
            return;
        }

        String currPlayer = getSymbolForUser(userId);
        gameBoard[row][col] = currPlayer;

        boolean hasWon = checkWinCondition(userId);
        boolean isDraw = checkDraw();

        if (hasWon || isDraw) {
            setGameBoard(new String[SIZE][SIZE]);
            displayBoard(gameBoard, SIZE);
        }

        broadcastMove(row, col, currPlayer);
        displayBoard(gameBoard, SIZE);

        if (hasWon) {
            broadcast("Player " + currPlayer + " won!");
        } else if (isDraw) {
            broadcast("Match draw!");
        } else {
            switchTurn();
        }

    }

    @Override
    public boolean checkWinCondition(String userId) {
        String symbol = getSymbolForUser(userId);
        for (int i = 0; i < 3; i++) {
            if (symbol.equals(gameBoard[i][0]) && symbol.equals(gameBoard[i][1]) && symbol.equals(gameBoard[i][2])) return true;
            if (symbol.equals(gameBoard[0][i]) && symbol.equals(gameBoard[1][i]) && symbol.equals(gameBoard[2][i])) return true;
        }
        return (symbol.equals(gameBoard[0][0]) && symbol.equals(gameBoard[1][1]) && symbol.equals(gameBoard[2][2])) ||
                (symbol.equals(gameBoard[0][2]) && symbol.equals(gameBoard[1][1]) && symbol.equals(gameBoard[2][0]));
    }

    @Override
    public boolean checkDraw() {
        for (String[] row : gameBoard) {
            for (String cell : row) {
                if (cell == null) return false;
            }
        }
        return true;
    }

    @Override
    public void switchTurn() {
        for (String userId : players.keySet()) {
            if (!userId.equals(currentTurn)) {
                currentTurn = userId;
                break;
            }
        }
    }

    @Override
    public void broadcast(String message) throws IOException {
        for (WebSocketSession session : players.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    public void broadcastMove(int row, int col, String currPlayer) throws IOException {
        String json = String.format("{\"row\":%d,\"col\":%d,\"player\":\"%s\"}", row, col, currPlayer);
        for (WebSocketSession session : players.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        }
    }
}
