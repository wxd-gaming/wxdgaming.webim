package wxdgaming.webim.service.module.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.BootConfig;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.ann.Shutdown;
import wxdgaming.boot2.starter.batis.mapdb.HoldMap;
import wxdgaming.boot2.starter.batis.mapdb.MapDBDataHelper;
import wxdgaming.boot2.starter.scheduled.ann.Scheduled;
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

    public List<Map<String, Object>> roomListBean(ChatUser chatUser) {
        return getRoomMap().values().stream()
                .filter(room -> room.hasUser(chatUser.getName()))
                .sorted(Comparator.comparingLong(ChatRoom::getRoomId))
                .map(ChatRoom::toBean)
                .toList();
    }

}
