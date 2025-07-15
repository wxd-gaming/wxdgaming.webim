package wxdgaming.webim.service.module.chat.processor;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.service.module.chat.AbstractProcessor;
import wxdgaming.webim.util.Utils;

/**
 * 登录处理
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
@Slf4j
@Singleton
public class LogoutProcessor extends AbstractProcessor {

    @Override public String type() {
        return "logout";
    }


    @Override public void process(SocketSession socketSession, ForwardMessage.Gateway2RoomServer gateway2RoomServer) {
        log.info("用户登录: {} 下线 退出公共聊天室", gateway2RoomServer.getAccount());

        dataService.getRoomMap().values().stream()
                .filter(room -> room.hasUser(gateway2RoomServer.getAccount()))
                .forEach(room -> {
                    if (room.isSystem()) {
                        room.getUserMap().remove(gateway2RoomServer.getAccount());
                    }
                    RunResult runResult = Utils.buildSystemTip(room, "%s 下线".formatted(gateway2RoomServer.getAccount()));
                    dataService.sendAllGateway(room, runResult);
                });


    }

}
