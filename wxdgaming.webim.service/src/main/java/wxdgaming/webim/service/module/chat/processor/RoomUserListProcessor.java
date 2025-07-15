package wxdgaming.webim.service.module.chat.processor;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.AbstractProcessor;
import wxdgaming.webim.bean.ChatRoom;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.service.module.data.DataService;
import wxdgaming.webim.util.Utils;

import java.util.ArrayList;

/**
 * 获取当前房间用户
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
@Slf4j
@Singleton
public class RoomUserListProcessor extends AbstractProcessor {

    final DataService dataService;

    @Inject
    public RoomUserListProcessor(DataService dataService) {
        this.dataService = dataService;
    }

    @Override public String type() {
        return "roomUserList";
    }

    @Override public void process(SocketSession socketSession, ChatUser self, JSONObject jsonObject) {
        long roomId = jsonObject.getLongValue("roomId");
        ChatRoom chatRoom = dataService.getRoomMap().get(roomId);
        if (chatRoom == null) {
            Utils.fail(socketSession, "房间不存在");
            return;
        }
        boolean hasUser = chatRoom.hasUser(self.getName());
        if (!hasUser) {
            Utils.fail(socketSession, "尚未加入该房间");
            return;
        }
        RunResult ok = RunResult.ok();
        ok.fluentPut("cmd", "roomUserList");
        ok.fluentPut("roomId", roomId);
        ok.fluentPut("userList", new ArrayList<>(chatRoom.getUserMap()));
        socketSession.write(ok.toJSONString());
    }

}
