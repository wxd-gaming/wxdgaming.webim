package wxdgaming.webim.service.module.chat.processor;

import com.alibaba.fastjson.JSONObject;
import com.google.common.html.HtmlEscapers;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.bean.ChatRoom;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.service.module.chat.AbstractProcessor;

/**
 * 聊天消息处理
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
@Slf4j
@Singleton
public class RoomMessageProcessor extends AbstractProcessor {

    @Override public String type() {
        return "roomMsg";
    }

    @Override public void process(SocketSession socketSession, ChatUser self, JSONObject jsonObject) {
        long roomId = jsonObject.getLongValue("roomId");
        ChatRoom chatRoom = dataService.getRoomMap().get(roomId);
        if (chatRoom == null) {
            chatService.fail(socketSession, "房间不存在");
            return;
        }
        if (!chatRoom.hasUser(self.getName())) {
            chatService.fail(socketSession, "尚未加入该房间");
            return;
        }

        RunResult ok = RunResult.ok();
        ok.fluentPut("cmd", "roomMsg");
        ok.fluentPut("roomId", roomId);
        ok.fluentPut("sender", self.getName());
        ok.fluentPut("time", MyClock.nowString());
        ok.fluentPut("type", jsonObject.getString("type"));
        String content = jsonObject.getString("content");
        String escape = HtmlEscapers.htmlEscaper().escape(content);
        ok.fluentPut("content", escape);
        String jsonString = ok.toJSONString();
        chatRoom.addHistory(jsonString);
        chatRoom.getSessionGroup().write(jsonString);
    }

}
