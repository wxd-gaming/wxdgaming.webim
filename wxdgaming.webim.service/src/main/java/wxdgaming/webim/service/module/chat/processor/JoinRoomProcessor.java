package wxdgaming.webim.service.module.chat.processor;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatRoom;
import wxdgaming.webim.service.module.chat.AbstractProcessor;
import wxdgaming.webim.util.Utils;

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

    @Override public void process(SocketSession socketSession, ForwardMessage.Gateway2RoomServer gateway2RoomServer) {
        long joinRoomId = gateway2RoomServer.getMessage().getLongValue("joinRoomId");
        ChatRoom chatRoom = dataService.getRoomMap().get(joinRoomId);
        if (chatRoom == null) {
            dataService.sendMessage2Gateway(socketSession, gateway2RoomServer, RunResult.fail("房间不存在"));
            return;
        }
        if (StringUtils.isNotBlank(chatRoom.getToken())) {
            String joinToken = gateway2RoomServer.getMessage().getString("joinToken");
            if (!Objects.equals(chatRoom.getToken(), joinToken)) {
                dataService.sendMessage2Gateway(socketSession, gateway2RoomServer, RunResult.fail("密钥错误"));
                return;
            }
        }

        chatRoom.addUser(gateway2RoomServer.getAccount());
        RunResult runResult = Utils.buildSystemTip(chatRoom, "%s 进入聊天室".formatted(gateway2RoomServer.getAccount()));
        dataService.sendAllGateway(chatRoom, runResult);
        dataService.sendRoomList(socketSession, gateway2RoomServer);
    }

}
