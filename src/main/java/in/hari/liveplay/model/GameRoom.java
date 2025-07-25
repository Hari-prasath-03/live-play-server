package in.hari.liveplay.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

public interface GameRoom {

    void addPlayer(String userId, WebSocketSession session);

    void handlePlayerMove(String userId, JsonNode move) throws IOException;

    boolean checkWinCondition(String userId);

    boolean checkDraw();

    void switchTurn();

    void broadcast(String message) throws IOException;

    default void displayBoard(String[][] board, int SIZE) {
        System.out.println();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                System.out.print(board[i][j] == null ? "   " : " " + board[i][j] + " ");
                if (j != board.length - 1) System.out.print("|");
            }
            if (i != board.length - 1) System.out.println("\n-----------");
        }
        System.out.println();
    }
}
