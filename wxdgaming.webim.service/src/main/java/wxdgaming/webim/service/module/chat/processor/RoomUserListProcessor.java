package wxdgaming.webim.service.module.chat.processor;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatRoom;
import wxdgaming.webim.service.module.chat.AbstractProcessor;
import wxdgaming.webim.util.Utils;

import java.util.ArrayList;

/**
 * 获取当前房间用户
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
@Slf4j
@Singleton
public class RoomUserListProcessor extends AbstractProcessor {


    @Override public String type() {
        return "roomUserList";
    }

    @Override public void process(SocketSession socketSession, ForwardMessage.Gateway2RoomServer gateway2RoomServer) {
        long roomId = gateway2RoomServer.getMessage().getLongValue("roomId");
        ChatRoom chatRoom = dataService.getRoomMap().get(roomId);
        if (chatRoom == null) {
            dataService.sendMessage2Gateway(socketSession, gateway2RoomServer, RunResult.fail("房间不存在"));
            return;
        }
        boolean hasUser = chatRoom.hasUser(gateway2RoomServer.getAccount());
        if (!hasUser) {
            dataService.sendMessage2Gateway(socketSession, gateway2RoomServer, RunResult.fail("尚未加入该房间"));
            return;
        }
        RunResult runResult = RunResult.ok();
        runResult.fluentPut("cmd", "roomUserList");
        runResult.fluentPut("roomId", roomId);
        runResult.fluentPut("userList", new ArrayList<>(chatRoom.getUserMap()));
        dataService.sendMessage2Gateway(socketSession, gateway2RoomServer, runResult);
    }

}
