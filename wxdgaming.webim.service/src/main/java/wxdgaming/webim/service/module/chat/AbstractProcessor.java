package wxdgaming.webim.service.module.chat;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.service.bean.ChatUser;
import wxdgaming.webim.service.module.data.DataService;
import wxdgaming.webim.service.module.user.ChatUserService;

/**
 * 处理器
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
public abstract class AbstractProcessor {

    @Inject protected ChatService chatService;
    @Inject protected ChatUserService chatUserService;
    @Inject protected DataService dataService;

    public abstract String type();

    public boolean checkLoginEnd() {
        return true;
    }

    public abstract void process(SocketSession socketSession, ChatUser self, JSONObject jsonObject);

}
