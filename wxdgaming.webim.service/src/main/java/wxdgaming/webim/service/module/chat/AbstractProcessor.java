package wxdgaming.webim.service.module.chat;

import com.google.inject.Inject;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatRoom;
import wxdgaming.webim.service.module.data.DataService;

import java.util.List;
import java.util.Map;

/**
 * 处理器
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
public abstract class AbstractProcessor {

    @Inject protected DataService dataService;
    @Inject protected ChatService chatService;

    public abstract String type();

    public abstract void process(SocketSession socketSession, ForwardMessage.Gateway2RoomServer gateway2RoomServer);

}
