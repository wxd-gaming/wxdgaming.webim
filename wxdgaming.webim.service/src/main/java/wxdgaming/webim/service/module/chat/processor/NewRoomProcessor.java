package wxdgaming.webim.service.module.chat.processor;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import wxdgaming.boot2.core.util.AssertUtil;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatRoom;
import wxdgaming.webim.service.module.chat.AbstractProcessor;

/**
 * 创建房间
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
@Slf4j
@Singleton
public class NewRoomProcessor extends AbstractProcessor {


    @Override public String type() {
        return "NewRoom";
    }

    @Override public void process(SocketSession socketSession, ForwardMessage.Gateway2RoomServer gateway2RoomServer) {

        String title = gateway2RoomServer.getMessage().getString("title");
        AssertUtil.assertTrue(!StringUtils.isBlank(title) && StringUtils.length(title) >= 3 && StringUtils.length(title) <= 12, "名字长度 3 ~ 8");

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomId(dataService.newRoomId());
        chatRoom.setTitle(title);
        chatRoom.setToken(gateway2RoomServer.getMessage().getString("token"));
        chatRoom.setMaster(gateway2RoomServer.getAccount());
        chatRoom.setMaxUser(1000);
        chatRoom.addUser(gateway2RoomServer.getAccount());
        dataService.getRoomMap().put(chatRoom.getRoomId(), chatRoom);

        dataService.sendRoomList(socketSession, gateway2RoomServer);
    }

}
