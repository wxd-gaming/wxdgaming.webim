package wxdgaming.webim.service.module.chat.processor;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.service.bean.ChatRoom;
import wxdgaming.webim.service.bean.ChatUser;
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

    @Override public void process(SocketSession socketSession, ChatUser self, JSONObject jsonObject) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomId(dataService.getAtomicLong().incrementAndGet());
        chatRoom.setTitle(jsonObject.getString("title"));
        chatRoom.setToken(jsonObject.getString("token"));
        chatRoom.setMaster(self.getName());
        chatRoom.setMaxUser(1000);
        chatRoom.addUser(self.getName());
        dataService.getRoomMap().put(chatRoom.getRoomId(), chatRoom);

        chatRoom.getSessionGroup().add(socketSession);

        chatService.sendRoomList(socketSession, self);
    }

}
