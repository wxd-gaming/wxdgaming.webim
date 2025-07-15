package wxdgaming.webim.util;

import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.bean.ChatRoom;

/**
 * 工具
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-14 20:25
 **/
public class Utils {

    public static void systemTip(ChatRoom chatRoom, String content) {
        RunResult ok = RunResult.ok();
        ok.fluentPut("cmd", "roomMsg");
        ok.fluentPut("roomId", chatRoom.getRoomId());
        ok.fluentPut("sender", "系统");
        ok.fluentPut("time", MyClock.nowString());
        ok.fluentPut("type", "system");
        ok.fluentPut("content", content);
        chatRoom.getSessionGroup().write(ok.toJSONString());
    }

    public static void fail(SocketSession socketSession, String msg) {
        socketSession.write(RunResult.fail(msg).toJSONString());
    }

}
