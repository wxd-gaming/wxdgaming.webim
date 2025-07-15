package wxdgaming.webim.service.module.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.boot2.starter.scheduled.ann.Scheduled;
import wxdgaming.webim.bean.ChatRoom;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.service.module.data.DataService;
import wxdgaming.webim.util.Utils;

import java.util.List;
import java.util.Map;

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
                .filter(chatRoom -> chatRoom.getSessionGroup().size() > 0)
                .forEach(chatRoom -> Utils.systemTip(chatRoom, "请文明聊天，合法合规；请勿发布违法信息，否则将被封号！"));
    }


    public void sendRoomList(SocketSession socketSession, ChatUser chatUser) {
        List<Map<String, Object>> roomList = dataService.roomListBean(chatUser);
        RunResult ok = RunResult.ok();
        ok.fluentPut("cmd", "roomList");
        ok.fluentPut("roomList", roomList);
        socketSession.write(ok.toJSONString());
    }

}
