package wxdgaming.webim.service.module.chat.processor;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.AbstractProcessor;
import wxdgaming.webim.bean.ChatRoom;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.service.module.chat.ChatService;
import wxdgaming.webim.service.module.data.DataService;
import wxdgaming.webim.util.Utils;

/**
 * 退出房间
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
@Slf4j
@Singleton
public class ExitRoomProcessor extends AbstractProcessor {

    final DataService dataService;
    final ChatService chatService;

    @Inject
    public ExitRoomProcessor(DataService dataService, ChatService chatService) {
        this.dataService = dataService;
        this.chatService = chatService;
    }

    @Override public String type() {
        return "exitRoom";
    }

    @Override public void process(SocketSession socketSession, ChatUser self, JSONObject jsonObject) {
        long roomId = jsonObject.getLongValue("roomId");
        ChatRoom chatRoom = dataService.getRoomMap().get(roomId);
        if (chatRoom == null) {
            Utils.fail(socketSession, "房间不存在");
            return;
        }
        if (chatRoom.getMaster().equals(self.getName())) {
            chatRoom.getSessionGroup().write(buildExitRoomMessage(chatRoom, self));
            dataService.getRoomMap().remove(roomId);
            dataService.getRoomMapCache().remove(String.valueOf(roomId));
        } else {
            chatRoom.removeUser(self.getName());
            chatRoom.getSessionGroup().remove(socketSession);
            Utils.systemTip(chatRoom, "%s 退出聊天室".formatted(self.getName()));
            socketSession.write(buildExitRoomMessage(chatRoom, self));
        }
    }

    String buildExitRoomMessage(ChatRoom chatRoom, ChatUser self) {
        RunResult ok = RunResult.ok();
        ok.fluentPut("cmd", "exitRoom");
        ok.fluentPut("roomId", chatRoom.getRoomId());
        return ok.toJSONString();
    }

}
