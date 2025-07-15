package wxdgaming.webim.util;

import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.webim.bean.ChatRoom;

/**
 * 工具
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-14 20:25
 **/
public class Utils {

    public static RunResult buildSystemTip(ChatRoom chatRoom, String content) {
        RunResult runResult = RunResult.ok();
        runResult.fluentPut("cmd", "roomMsg");
        runResult.fluentPut("roomId", chatRoom.getRoomId());
        runResult.fluentPut("sender", "系统");
        runResult.fluentPut("time", MyClock.nowString());
        runResult.fluentPut("type", "system");
        runResult.fluentPut("content", content);
        return runResult;
    }
}
