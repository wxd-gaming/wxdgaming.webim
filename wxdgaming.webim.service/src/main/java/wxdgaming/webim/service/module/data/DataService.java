package wxdgaming.webim.service.module.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.BootConfig;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.ann.Shutdown;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.batis.mapdb.HoldMap;
import wxdgaming.boot2.starter.batis.mapdb.MapDBDataHelper;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.boot2.starter.scheduled.ann.Scheduled;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatRoom;
import wxdgaming.webim.bean.ChatUser;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-08 11:00
 **/
@Slf4j
@Getter
@Singleton
public class DataService extends HoldRunApplication {

    private final AtomicLong atomicLong = new AtomicLong(1000);
    private final ConcurrentHashMap<Long, ChatRoom> roomMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, SocketSession> gatewayServerMappingMap = new java.util.concurrent.ConcurrentHashMap<>();
    final MapDBDataHelper mapDBDataHelper;
    final HoldMap roomMapCache;

    @Inject
    public DataService(MapDBDataHelper mapDBDataHelper) {
        this.mapDBDataHelper = mapDBDataHelper;
        roomMapCache = mapDBDataHelper.bMap("room-map");
        for (Object value : roomMapCache.values()) {
            ChatRoom chatRoom = (ChatRoom) value;
            roomMap.put(chatRoom.getRoomId(), chatRoom);
        }
        if (BootConfig.getIns().sid() == 1) {
            if (!roomMap.containsKey(1L)) {
                ChatRoom publicChatRoom = new ChatRoom().setSystem(true).setRoomId(1).setMaster("系统").setTitle("公共聊天室").setMaxUser(1000);
                roomMap.put(publicChatRoom.getRoomId(), publicChatRoom);
            }
        }
    }

    @Shutdown
    public void shutdown() {
        saveRoom();
        mapDBDataHelper.close();
    }

    @Scheduled("0 * * * * *")
    public void saveRoom() {

        roomMap.values().forEach(chatRoom -> {
            roomMapCache.put(String.valueOf(chatRoom.getRoomId()), chatRoom);
        });

    }

    public List<Map<String, Object>> roomListBean(String account) {
        return getRoomMap().values().stream()
                .filter(room -> room.hasUser(account))
                .sorted(Comparator.comparingLong(ChatRoom::getRoomId))
                .map(ChatRoom::toBean)
                .toList();
    }

    public void sendAllGateway(ChatRoom chatRoom, RunResult runResult) {
        ForwardMessage.RoomServer2Gateway roomServer2Gateway = chatRoom.buildRoomServer2Gateway(runResult);
        sendAllGateway(roomServer2Gateway);
    }
    public void sendAllGateway(ForwardMessage.RoomServer2Gateway roomServer2Gateway) {
        final String jsonString = roomServer2Gateway.toJSONString();
        getGatewayServerMappingMap().values().forEach(socketSession -> {
            socketSession.writeAndFlush(jsonString);
        });
    }

    public void sendRoomList(SocketSession socketSession, ForwardMessage.Gateway2RoomServer gateway2RoomServer) {
        List<Map<String, Object>> roomList = roomListBean(gateway2RoomServer.getAccount());
        RunResult runResult = RunResult.ok();
        runResult.fluentPut("cmd", "roomList");
        runResult.fluentPut("roomList", roomList);
        sendMessage2Gateway(socketSession, gateway2RoomServer, runResult);
    }

    public void sendMessage2Gateway(SocketSession socketSession, ForwardMessage.Gateway2RoomServer gateway2RoomServer, RunResult runResult) {
        ForwardMessage.RoomServer2Gateway roomServer2Gateway = new ForwardMessage.RoomServer2Gateway();
        roomServer2Gateway.setAccountList(List.of(gateway2RoomServer.getAccount()));
        roomServer2Gateway.setMessage(runResult);
        socketSession.writeAndFlush(roomServer2Gateway.toJSONString());
    }

}
