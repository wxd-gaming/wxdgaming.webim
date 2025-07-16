package wxdgaming.webim.gateway.module.dirve.impl.app;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.ann.Value;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.token.JsonToken;
import wxdgaming.boot2.core.token.JsonTokenParse;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.gateway.module.dirve.AbstractApp2GatewayMessageProcessor;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.gateway.module.GatewayService;
import wxdgaming.webim.gateway.module.service.Gateway2RoomServerSocketProxy;

import java.util.Comparator;

/**
 * 登陆处理器
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-14 19:17
 **/
@Slf4j
@Singleton
public class LoginProcessor extends AbstractApp2GatewayMessageProcessor {

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

    @Override public boolean checkLoginEnd() {
        return false;
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

        SocketSession oldSession = gatewayService.getAccountSessionMappingMap().put(chatUser.getName(), socketSession);
        if (oldSession != null) {
            RunResult runResult = RunResult.ok();
            runResult.fluentPut("cmd", "logout");
            runResult.fluentPut("msg", "其它地方登录");
            oldSession
                    .writeAndFlush(runResult.toJSONString())
                    .addListener(ChannelFutureListener.CLOSE);
        }

        socketSession.getChannel().closeFuture().addListener(future -> {
            gatewayService.getAccountSessionMappingMap().remove(chatUser.getName());
            log.info("用户登录: {} 下线", chatUser.getName());
            logout2RoomServer(socketSession, chatUser, jsonObject);
        });

        login2RoomServer(socketSession, chatUser, jsonObject);
    }

    void logout2RoomServer(SocketSession socketSession, ChatUser chatUser, JSONObject jsonObject) {
        ForwardMessage.Gateway2RoomServer forwardMessage = new ForwardMessage.Gateway2RoomServer();
        forwardMessage.setAccount(chatUser.getName());
        forwardMessage.setClientSessionId(socketSession.getUid());
        forwardMessage.setCmd("logout");
        gatewayService.getRoomServerProxyMap().values().forEach(roomServer -> {
            SocketSession idle = roomServer.idle();
            if (idle != null) {
                idle.write(forwardMessage.toJSONString());
            } else {
                log.warn("{} 服务器繁忙", roomServer.getRoomServerId());
            }
        });
    }

    void login2RoomServer(SocketSession socketSession, ChatUser chatUser, JSONObject jsonObject) {
        ForwardMessage.Gateway2RoomServer forwardMessage = new ForwardMessage.Gateway2RoomServer();
        forwardMessage.setAccount(chatUser.getName());
        forwardMessage.setClientSessionId(socketSession.getUid());
        forwardMessage.setCmd("login");
        forwardMessage.setMessage(jsonObject);
        gatewayService.getRoomServerProxyMap().values().stream().sorted(Comparator.comparingInt(Gateway2RoomServerSocketProxy::getRoomServerId)).forEach(roomServer -> {
            SocketSession idle = roomServer.idle();
            if (idle != null) {
                idle.write(forwardMessage.toJSONString());
            } else {
                log.warn("{} 服务器繁忙", roomServer.getRoomServerId());
            }
        });
    }

}
