package wxdgaming.webim.gateway.module.dirve;

import com.google.inject.Singleton;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.boot2.starter.net.pojo.IWebSocketStringListener;

/**
 * 处理websocket请求 字符串匹配
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-14 13:23
 */
@Singleton
public class WebSocketDriver implements IWebSocketStringListener {

    @Override public void onMessage(SocketSession socketSession, String message) {

    }

}
