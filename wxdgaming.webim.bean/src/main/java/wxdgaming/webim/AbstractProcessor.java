package wxdgaming.webim;

import com.alibaba.fastjson.JSONObject;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.bean.ChatUser;

/**
 * 处理器
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
public abstract class AbstractProcessor {


    public abstract String type();

    public boolean checkLoginEnd() {
        return true;
    }

    public abstract void process(SocketSession socketSession, ChatUser self, JSONObject jsonObject);

}
