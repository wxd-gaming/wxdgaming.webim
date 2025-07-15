package wxdgaming.webim.gateway.module.dirve.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.gateway.module.dirve.AbstractProcessor;
import wxdgaming.webim.gateway.module.service.Gateway2RoomServerSocketProxy;

import java.util.Map;

/**
 * 创建房间
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
@Slf4j
@Singleton
public class NewRoomProcessor extends AbstractProcessor {

    @Override public String type() {
        return "NewRoom";
    }

    @Override public void process(SocketSession socketSession, ChatUser self, JSONObject jsonObject) {

        Integer integer = gatewayService.getRoomServerRoomCountMap().entrySet().stream()
                .min((o1, o2) -> Integer.compare(o2.getValue().get(), o1.getValue().get()))
                .map(Map.Entry::getKey)
                .orElse(null);

        if (integer == null) {
            socketSession.write(RunResult.fail("服务不可用").toJSONString());
            return;
        }

        Gateway2RoomServerSocketProxy gateway2RoomServerSocketProxy = gatewayService.getRoomServerProxyMap().get(integer);
        if (gateway2RoomServerSocketProxy == null) {
            socketSession.write(RunResult.fail("服务不可用").toJSONString());
            return;
        }

        SocketSession roomServer = gateway2RoomServerSocketProxy.idle();
        if (roomServer == null) {
            socketSession.write(RunResult.fail("服务器繁忙").toJSONString());
            return;
        }

        send2RoomServer(roomServer, socketSession, self, jsonObject);
    }

    void send2RoomServer(SocketSession roomServer, SocketSession socketSession, ChatUser chatUser, JSONObject jsonObject) {
        ForwardMessage.Gateway2RoomServer forwardMessage = new ForwardMessage.Gateway2RoomServer();
        forwardMessage.setAccount(chatUser.getName());
        forwardMessage.setClientSessionId(socketSession.getUid());
        forwardMessage.setCmd("NewRoom");
        forwardMessage.setMessage(jsonObject);
        roomServer.write(forwardMessage.toJSONString());
    }

}
