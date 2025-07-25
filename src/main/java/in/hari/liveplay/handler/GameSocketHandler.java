package in.hari.liveplay.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.hari.liveplay.model.AiPlayerRoom;
import in.hari.liveplay.model.TwoPlayerRoom;
import in.hari.liveplay.store.AiGameStore;
import in.hari.liveplay.store.TwoPlayerGameStore;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Slf4j
@Component
public class GameSocketHandler extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("New connection: {}", session.getId());
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(payload);

        String type = jsonNode.get("type").asText();
        String roomId = jsonNode.get("roomId").asText();
        String userId = jsonNode.get("userId").asText();

        switch (type) {
            case "create":
                if (TwoPlayerGameStore.isRoomExist(roomId)) {
                    session.sendMessage(new TextMessage("Room already exists!"));
                    return;
                }
                TwoPlayerRoom room = TwoPlayerGameStore.createRoom(roomId);
                room.addPlayer(userId, session);
                session.sendMessage(new TextMessage("Room created! with "+ roomId));
                break;

            case "join":
                TwoPlayerRoom joinRoom = TwoPlayerGameStore.getRoom(roomId);
                if (joinRoom == null) {
                    session.sendMessage(new TextMessage("Room does not exist!"));
                    return;
                }
                if (joinRoom.hasPlayer(userId)) {
                    joinRoom.rejoinRoom(userId, session);
                    session.sendMessage(new TextMessage("Room joined!"));
                    return;
                }
                if (joinRoom.isFull()) {
                    session.sendMessage(new TextMessage("Room full!"));
                    return;
                }
                System.out.println("roomId : " + roomId + " userId : " + userId);
                joinRoom.addPlayer(userId, session);
                session.sendMessage(new TextMessage("Room joined!"));

                for (WebSocketSession player : joinRoom.getAllSessions()) {
                    if (!player.equals(session) && player.isOpen()) {
                        player.sendMessage(new TextMessage("Opponent joined!"));
                    }
                }
                break;

            case "reconnect":
                TwoPlayerRoom rejoinRoom = TwoPlayerGameStore.getRoom(roomId);
                if (rejoinRoom == null) {
                    session.sendMessage(new TextMessage("Room does not exist!"));
                    return;
                }
                if (!rejoinRoom.hasPlayer(userId)) {
                    session.sendMessage(new TextMessage("Unable to rejoin room!"));
                    return;
                }
                rejoinRoom.rejoinRoom(userId, session);
                session.sendMessage(new TextMessage("Room rejoined!"));
                break;

            case "move":
                TwoPlayerRoom currentRoom = TwoPlayerGameStore.getRoom(roomId);
                if  (currentRoom == null) {
                    session.sendMessage(new TextMessage("Room does not exist!"));
                    return;
                }
                currentRoom.handlePlayerMove(userId, jsonNode.get("payload"));
                break;

            case "ai-create":
                if (!AiGameStore.isRoomExist(roomId)) {
                    AiPlayerRoom aiPlayerRoom = AiGameStore.createRoom(roomId);
                    aiPlayerRoom.addPlayer(userId, session);
                    session.sendMessage(new TextMessage("Room created with 🤖!"));
                } else {
                    AiPlayerRoom aiPlayerRoom = AiGameStore.getRoom(roomId);
                    if (aiPlayerRoom.userId.equals(userId)) {
                        aiPlayerRoom.replacePlayer(session);
                        session.sendMessage(new TextMessage("Reconnected!"));
                    }
                }
                break;

            case "ai-move":
                AiPlayerRoom aiRoom = AiGameStore.getRoom(roomId);
                aiRoom.handlePlayerMove(userId, jsonNode.get("payload"));
                break;

            case "clear-2-player-room":
                TwoPlayerRoom roomToClear = TwoPlayerGameStore.getRoom(roomId);
                if (roomToClear == null) {
                    System.out.println("Room already cleared!");
                    return;
                }
                TwoPlayerGameStore.removeRoom(roomId);
                System.out.println(roomId + " Cleared successfully.\n");
                break;

            case "clear-ai-room":
                AiPlayerRoom aiPlayerRoom = AiGameStore.getRoom(roomId);
                if (aiPlayerRoom == null) {
                    session.sendMessage(new TextMessage("Room does not exist!"));
                    return;
                }
                AiGameStore.removeRoom(roomId);
                System.out.println(roomId + " Cleared successfully.\n");
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Connection closed: {}", session.getId());
    }
}
