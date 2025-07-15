package wxdgaming.webim.gateway.module.dirve.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.ann.Value;
import wxdgaming.boot2.core.token.JsonToken;
import wxdgaming.boot2.core.token.JsonTokenParse;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.AbstractProcessor;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.gateway.module.GatewayService;
import wxdgaming.webim.util.Utils;

/**
 * 登陆处理器
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-14 19:17
 **/
@Slf4j
@Singleton
public class LoginProcessor extends AbstractProcessor {

    @Value(path = "json.token.key", nestedPath = true)
    private String jsonTokenKey;

    final GatewayService gatewayService;

    @Inject
    public LoginProcessor(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @Override public String type() {
        return "login";
    }

    public ChatUser parseChatUser(String token) {
        JsonToken jsonToken = JsonTokenParse.parse(jsonTokenKey, token);
        String name = jsonToken.getString("name");
        String openId = jsonToken.getString("openId");
        return new ChatUser().setName(name).setOpenId(openId);
    }

    @Override public void process(SocketSession socketSession, ChatUser self, JSONObject jsonObject) {
        String token = jsonObject.getString("token");
        ChatUser chatUser = this.parseChatUser(token);
        socketSession.bindData("user", chatUser);

        log.info("用户登录: {} 上线", chatUser.getName());

        ForwardMessage.Gateway2RoomServer forwardMessage = new ForwardMessage.Gateway2RoomServer();
        forwardMessage.setAccount(chatUser.getName());
        forwardMessage.setClientSessionId(socketSession.getUid());
        forwardMessage.setMessage(jsonObject);
        gatewayService.getRoomServerMap().values().forEach(roomServer -> {
            SocketSession idle = roomServer.idle();
            if (idle != null) {
                idle.write(forwardMessage.toJSONString());
            } else {
                log.warn("{} 服务器繁忙", roomServer.getRoomServerId());
            }
        });

    }

}
