package wxdgaming.webim.gateway.module.dirve.impl.roomserver;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.gateway.module.dirve.AbstractRoomServerMessageProcessor;

/**
 * 创建房间
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
@Slf4j
@Singleton
public class AddNewRoomProcessor extends AbstractRoomServerMessageProcessor {

    @Override public String type() {
        return "addNewRoom";
    }

    @Override public void process(SocketSession socketSession, ChatUser self, JSONObject jsonObject) {
        String roomId = jsonObject.getString("roomId");
        int sid = jsonObject.getIntValue("sid");
        gatewayService.getRoomId4RoomServerMapping().put(roomId, sid);
        log.info("创建房间: {} -> {}", sid, roomId);
    }

}
