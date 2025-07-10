package wxdgaming.webim.service.module.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.starter.batis.mapdb.MapDBDataHelper;
import wxdgaming.webim.service.bean.ChatRoom;
import wxdgaming.webim.service.bean.ChatUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final Map<Long, ChatRoom> roomMap = new java.util.concurrent.ConcurrentHashMap<>();
    final MapDBDataHelper mapDBDataHelper;

    @Inject
    public DataService(MapDBDataHelper mapDBDataHelper) {
        this.mapDBDataHelper = mapDBDataHelper;
        ChatRoom publicChatRoom = new ChatRoom().setSystem(true).setRoomId(1).setMaster("系统").setTitle("公共聊天室").setMaxUser(1000);
        roomMap.put(publicChatRoom.getRoomId(), publicChatRoom);
        roomMap.put(2L, new ChatRoom().setSystem(true).setRoomId(2).setMaster("系统").setTitle("公共聊天室2").setMaxUser(1000));
    }

    public List<Map<String, String>> roomListBean(ChatUser chatUser) {
        List<Map<String, String>> roomList = new ArrayList<>();
        roomList.add(getRoomMap().get(1L).toBean());
        roomList.add(getRoomMap().get(2L).toBean());

        List<Map<String, String>> tmp = getRoomMap().values().stream()
                .filter(room -> room.getRoomId() > 1000)
                .filter(room -> room.getUserMap().contains(chatUser.getName()))
                .map(ChatRoom::toBean)
                .toList();

        roomList.addAll(tmp);
        return roomList;
    }

}
