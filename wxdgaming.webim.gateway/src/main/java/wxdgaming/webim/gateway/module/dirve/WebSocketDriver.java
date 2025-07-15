package wxdgaming.webim.gateway.module.dirve;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.ann.Init;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.util.AssertUtil;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.boot2.starter.net.pojo.IWebSocketStringListener;
import wxdgaming.webim.AbstractProcessor;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.gateway.module.GatewayService;
import wxdgaming.webim.gateway.module.service.Gateway2RoomServerSocketClientImpl;
import wxdgaming.webim.util.Utils;

import java.util.HashMap;

/**
 * 处理websocket请求 字符串匹配
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-14 13:23
 */
@Slf4j
@Singleton
public class WebSocketDriver extends HoldRunApplication implements IWebSocketStringListener {

    HashMap<String, AbstractProcessor> processorMap = new HashMap<>();

    final GatewayService gatewayService;

    @Inject
    public WebSocketDriver(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @Init
    public void init() {
        HashMap<String, AbstractProcessor> tmpProcessorMap = new HashMap<>();
        runApplication.classWithSuper(AbstractProcessor.class)
                .forEach(processor -> {
                    AbstractProcessor old = tmpProcessorMap.put(processor.type().toLowerCase(), processor);
                    AssertUtil.assertTrue(old == null, "重复注册处理器：{}", processor.type());
                });
        processorMap = tmpProcessorMap;
    }

    @Override public void onMessage(SocketSession socketSession, String message) {
        ChatUser bindData = socketSession.bindData("user");
        log.debug("ws接受到消息: {}, {}, {}", socketSession, bindData, message);
        try {
            if (socketSession.getType() == SocketSession.Type.client) {
                /*接受发过来的消息*/
                JSONObject jsonObject = FastJsonUtil.parseJSONObject(message);
                String cmd = jsonObject.getString("cmd");
                if (StringUtils.isBlank(cmd)) {
                    Utils.fail(socketSession, ("命令错误"));
                    return;
                }

                AbstractProcessor abstractProcessor = processorMap.get(cmd.toLowerCase());
                if (abstractProcessor != null) {
                    if (abstractProcessor.checkLoginEnd()) {
                        if (bindData == null) {
                            Utils.fail(socketSession, ("尚未登录"));
                            return;
                        }
                    }
                    abstractProcessor.process(socketSession, bindData, jsonObject);
                } else {
                    /*说明需要转发*/
                    long roomId = jsonObject.getLongValue("roomId");
                    Integer room2ServerId = gatewayService.getRoomId4RoomServerMapping().get(roomId);
                    if (room2ServerId == null) {
                        Utils.fail(socketSession, ("房间不存在"));
                        return;
                    }
                    Gateway2RoomServerSocketClientImpl gateway2RoomServerSocketClient = gatewayService.getRoomServerMap().get(room2ServerId);
                    if (gateway2RoomServerSocketClient == null) {
                        Utils.fail(socketSession, "房间不可用");
                        return;
                    }
                    SocketSession idle = gateway2RoomServerSocketClient.idle();
                    if (idle != null) {
                        ForwardMessage.Gateway2RoomServer forwardMessage = new ForwardMessage.Gateway2RoomServer();
                        forwardMessage.setClientSessionId(socketSession.getUid());
                        forwardMessage.setAccount(bindData.getName());
                        forwardMessage.setMessage(jsonObject);
                        idle.writeAndFlush(forwardMessage.toJSONString());
                    } else {
                        Utils.fail(socketSession, "服务器繁忙");
                    }
                }
            } else {
                ForwardMessage.RoomServer2Gateway forwardMessage = FastJsonUtil.parse(message, ForwardMessage.RoomServer2Gateway.class);

            }
        } catch (Exception e) {
            log.error("ws处理消息异常: {}", e.getMessage());
            Utils.fail(socketSession, "服务器异常");
        }
    }

}
