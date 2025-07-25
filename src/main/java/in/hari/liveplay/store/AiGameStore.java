package in.hari.liveplay.store;

import in.hari.liveplay.model.AiPlayerRoom;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class AiGameStore {
    private static final ConcurrentHashMap<String, AiPlayerRoom> rooms = new ConcurrentHashMap<>();

    public static AiPlayerRoom createRoom(String roomId) {
        AiPlayerRoom aiPlayerRoom = new AiPlayerRoom(roomId);
        rooms.put(roomId, aiPlayerRoom);
        return aiPlayerRoom;
    }

    public static AiPlayerRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public static boolean isRoomExist(String roomId) {
        return rooms.containsKey(roomId);
    }

    public static void removeRoom(String roomId) {
        rooms.remove(roomId);
    }
}
