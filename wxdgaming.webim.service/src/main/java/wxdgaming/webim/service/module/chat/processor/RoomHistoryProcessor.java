package wxdgaming.webim.service.module.chat.processor;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.service.bean.ChatRoom;
import wxdgaming.webim.service.bean.ChatUser;
import wxdgaming.webim.service.module.chat.AbstractProcessor;

import java.util.ArrayList;

/**
 * 获取历史消息
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
@Slf4j
@Singleton
public class RoomHistoryProcessor extends AbstractProcessor {

    @Override public String type() {
        return "pullHistoryMessage";
    }

    @Override public void process(SocketSession socketSession, ChatUser self, JSONObject jsonObject) {
        long roomId = jsonObject.getLongValue("roomId");
        ChatRoom chatRoom = dataService.getRoomMap().get(roomId);
        if (chatRoom == null) {
            chatService.fail(socketSession, "房间不存在");
            return;
        }
        boolean hasUser = chatRoom.hasUser(self.getName());
        if (!hasUser) {
            chatService.fail(socketSession, "尚未加入该房间");
            return;
        }
        RunResult ok = RunResult.ok();
        ok.fluentPut("cmd", "pullHistoryMessage");
        ok.fluentPut("roomId", roomId);
        ok.fluentPut("userList", new ArrayList<>(chatRoom.getUserMap()));
        ok.fluentPut("history", chatRoom.roomHistory());
        socketSession.write(ok.toJSONString());
    }

}
