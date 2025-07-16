package wxdgaming.webim;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import wxdgaming.boot2.core.lang.ObjectBase;

import java.util.List;

/**
 * 转发消息
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-14 20:40
 **/
public interface ForwardMessage {

    @Getter
    @Setter
    public static class Gateway2RoomServer extends ObjectBase {
        private String account;
        private String cmd;
        private JSONObject message;
    }

    @Getter
    @Setter
    public static class RoomServer2Gateway extends ObjectBase {
        private String cmd;
        private JSONObject message;
        private long clientSessionId;
        private List<String> accountList;
    }

}
