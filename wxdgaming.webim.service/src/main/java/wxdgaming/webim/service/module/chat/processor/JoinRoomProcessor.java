package wxdgaming.webim.service.module.chat.processor;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.service.bean.ChatRoom;
import wxdgaming.webim.service.bean.ChatUser;
import wxdgaming.webim.service.module.chat.AbstractProcessor;

import java.util.Objects;

/**
 * 加入房间
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
@Slf4j
@Singleton
public class JoinRoomProcessor extends AbstractProcessor {

    @Override public String type() {
        return "joinRoom";
    }

    @Override public void process(SocketSession socketSession, ChatUser self, JSONObject jsonObject) {
        long joinRoomId = jsonObject.getLongValue("joinRoomId");
        ChatRoom chatRoom = dataService.getRoomMap().get(joinRoomId);
        if (chatRoom == null) {
            chatService.fail(socketSession, "房间不存在");
            return;
        }
        if (StringUtils.isNotBlank(chatRoom.getToken())) {
            String joinToken = jsonObject.getString("joinToken");
            if (!Objects.equals(chatRoom.getToken(), joinToken)) {
                chatService.fail(socketSession, "密钥错误");
                return;
            }
        }

        chatRoom.addUser(self.getName());
        chatRoom.getSessionGroup().add(socketSession);
        chatService.systemTip(chatRoom, "%s 进入聊天室".formatted(self.getName()));

        chatService.sendRoomList(socketSession, self);
    }

}
