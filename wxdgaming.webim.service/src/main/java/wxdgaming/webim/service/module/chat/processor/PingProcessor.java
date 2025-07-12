package wxdgaming.webim.service.module.chat.processor;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.service.module.chat.AbstractProcessor;

/**
 * ping
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
@Slf4j
@Singleton
public class PingProcessor extends AbstractProcessor {

    @Override public String type() {
        return "ping";
    }

    @Override public boolean checkLoginEnd() {
        return false;
    }

    @Override public void process(SocketSession socketSession, ChatUser self, JSONObject jsonObject) {
    }

}
