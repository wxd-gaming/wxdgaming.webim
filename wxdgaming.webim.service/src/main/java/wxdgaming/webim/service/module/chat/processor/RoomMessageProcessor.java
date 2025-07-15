package wxdgaming.webim.service.module.chat.processor;

import com.google.common.html.HtmlEscapers;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatRoom;
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

    @Override public void process(SocketSession socketSession, ForwardMessage.Gateway2RoomServer gateway2RoomServer) {
        long roomId = gateway2RoomServer.getMessage().getLongValue("roomId");
        ChatRoom chatRoom = dataService.getRoomMap().get(roomId);
        if (chatRoom == null) {
            dataService.sendMessage2Gateway(socketSession, gateway2RoomServer, RunResult.fail("房间不存在"));
            return;
        }

        if (!chatRoom.hasUser(gateway2RoomServer.getAccount())) {
            dataService.sendMessage2Gateway(socketSession, gateway2RoomServer, RunResult.fail("尚未加入该房间"));
            return;
        }

        RunResult runResult = RunResult.ok();
        runResult.fluentPut("cmd", "roomMsg");
        runResult.fluentPut("roomId", roomId);
        runResult.fluentPut("sender", gateway2RoomServer.getAccount());
        runResult.fluentPut("time", MyClock.nowString());
        runResult.fluentPut("type", gateway2RoomServer.getMessage().getString("type"));
        String content = gateway2RoomServer.getMessage().getString("content");
        String escape = HtmlEscapers.htmlEscaper().escape(content);
        runResult.fluentPut("content", escape);
        String jsonString = runResult.toJSONString();
        chatRoom.addHistory(jsonString);
        dataService.sendAllGateway(chatRoom, runResult);

    }

}
