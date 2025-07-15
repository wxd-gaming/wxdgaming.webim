package wxdgaming.webim.service.module.chat.processor;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatRoom;
import wxdgaming.webim.service.module.chat.AbstractProcessor;
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

    @Override public String type() {
        return "exitRoom";
    }

    @Override public void process(SocketSession socketSession, ForwardMessage.Gateway2RoomServer gateway2RoomServer) {
        String roomId = gateway2RoomServer.getMessage().getString("roomId");
        ChatRoom chatRoom = dataService.getRoomMap().get(roomId);
        if (chatRoom == null) {
            dataService.sendMessage2Gateway(socketSession, gateway2RoomServer, RunResult.fail("房间不存在"));
            return;
        }
        if (chatRoom.getMaster().equals(gateway2RoomServer.getAccount())) {
            dataService.getRoomMap().remove(roomId);
            dataService.getRoomMapCache().remove(roomId);
        } else {
            chatRoom.removeUser(gateway2RoomServer.getAccount());
            RunResult runResult = Utils.buildSystemTip(chatRoom, "%s 退出聊天室".formatted(gateway2RoomServer.getAccount()));
            dataService.sendAllGateway(chatRoom, runResult);
            dataService.sendMessage2Gateway(socketSession, gateway2RoomServer, buildExitRoomMessage(chatRoom));
        }
    }

    RunResult buildExitRoomMessage(ChatRoom chatRoom) {
        RunResult runResult = RunResult.ok();
        runResult.fluentPut("cmd", "exitRoom");
        runResult.fluentPut("roomId", chatRoom.getRoomId());
        return runResult;
    }

}
