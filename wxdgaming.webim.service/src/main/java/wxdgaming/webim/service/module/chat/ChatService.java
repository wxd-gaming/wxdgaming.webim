package wxdgaming.webim.service.module.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.boot2.starter.scheduled.ann.Scheduled;
import wxdgaming.webim.service.bean.ChatRoom;
import wxdgaming.webim.service.bean.ChatUser;
import wxdgaming.webim.service.module.data.DataService;
import wxdgaming.webim.service.module.user.ChatUserService;

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

    final ChatUserService chatUserService;
    final DataService dataService;

    @Inject
    public ChatService(ChatUserService chatUserService, DataService dataService) {
        this.chatUserService = chatUserService;
        this.dataService = dataService;
    }

    @Scheduled("0 */5 * * * *")
    public void scheduled() {
        dataService.getRoomMap().values().stream()
                .filter(ChatRoom::isSystem)
                .filter(chatRoom -> chatRoom.getSessionGroup().size() > 0)
                .forEach(chatRoom -> systemTip(chatRoom, "请文明聊天，合法合规；请勿发布违法信息，否则将被封号！"));
    }

    public void systemTip(ChatRoom chatRoom, String content) {
        RunResult ok = RunResult.ok();
        ok.fluentPut("cmd", "roomMsg");
        ok.fluentPut("roomId", chatRoom.getRoomId());
        ok.fluentPut("sender", "系统");
        ok.fluentPut("time", MyClock.nowString());
        ok.fluentPut("type", "system");
        ok.fluentPut("content", content);
        chatRoom.getSessionGroup().write(ok.toJSONString());
    }

    public void sendRoomList(SocketSession socketSession, ChatUser chatUser) {
        List<Map<String, Object>> roomList = dataService.roomListBean(chatUser);
        RunResult ok = RunResult.ok();
        ok.fluentPut("cmd", "roomList");
        ok.fluentPut("roomList", roomList);
        socketSession.write(ok.toJSONString());
    }

    public void fail(SocketSession socketSession, String msg) {
        socketSession.write(RunResult.fail(msg).toJSONString());
    }

}
