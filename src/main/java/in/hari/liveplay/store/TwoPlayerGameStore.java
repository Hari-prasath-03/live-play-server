package in.hari.liveplay.store;

import in.hari.liveplay.model.TwoPlayerRoom;

import java.util.concurrent.ConcurrentHashMap;

public class TwoPlayerGameStore {
    private static final ConcurrentHashMap<String, TwoPlayerRoom> rooms = new ConcurrentHashMap<>();

    public static TwoPlayerRoom createRoom(String roomId) {
        TwoPlayerRoom room = new TwoPlayerRoom(roomId);
        rooms.put(roomId, room);
        return room;
    }

    public static TwoPlayerRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public static boolean isRoomExist(String roomId) {
        return rooms.containsKey(roomId);
    }

    public static void removeRoom(String roomId) {
        rooms.remove(roomId);
    }

}
