package wxdgaming.webim.gateway.module.dirve;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.gateway.module.GatewayService;

/**
 * 房间服务器传递过来的消息处理
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
public abstract class AbstractRoomServerMessageProcessor {

    @Inject protected GatewayService gatewayService;

    public abstract String type();

    public boolean checkLoginEnd() {
        return true;
    }

    public abstract void process(SocketSession socketSession, ChatUser self, JSONObject jsonObject);

}
