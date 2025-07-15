package wxdgaming.webim.service.module.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.scheduled.ann.Scheduled;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatRoom;
import wxdgaming.webim.service.module.data.DataService;
import wxdgaming.webim.util.Utils;

/**
 * 聊天服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-08 11:04
 **/
@Slf4j
@Singleton
public class ChatService extends HoldRunApplication {

    final DataService dataService;

    @Inject
    public ChatService(DataService dataService) {
        this.dataService = dataService;
    }

    @Scheduled("0 */5 * * * *")
    public void scheduled() {
        dataService.getRoomMap().values().stream()
                .filter(ChatRoom::isSystem)
                .filter(chatRoom -> !chatRoom.getUserMap().isEmpty())
                .forEach(chatRoom -> {
                    RunResult runResult = Utils.buildSystemTip(chatRoom, "请文明聊天，合法合规；请勿发布违法信息，否则将被封号！");
                    dataService.sendAllGateway(chatRoom, runResult);
                });
    }

}
