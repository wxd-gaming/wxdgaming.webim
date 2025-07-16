package wxdgaming.webim.gateway.module.dirve;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.ann.Init;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.AssertUtil;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.boot2.starter.net.server.IServerWebSocketStringListener;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.gateway.module.GatewayService;
import wxdgaming.webim.gateway.module.service.Gateway2RoomServerSocketProxy;

import java.util.HashMap;

/**
 * 客户端 到 网关的 字符串匹配
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-14 13:23
 */
@Slf4j
@Singleton
public class App2GatewayServerWebSocketDriver extends HoldRunApplication implements IServerWebSocketStringListener {

    HashMap<String, AbstractApp2GatewayMessageProcessor> processorMap = new HashMap<>();

    final GatewayService gatewayService;

    @Inject
    public App2GatewayServerWebSocketDriver(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @Init
    public void init() {
        HashMap<String, AbstractApp2GatewayMessageProcessor> tmpProcessorMap = new HashMap<>();
        runApplication.classWithSuper(AbstractApp2GatewayMessageProcessor.class)
                .forEach(processor -> {
                    AbstractApp2GatewayMessageProcessor old = tmpProcessorMap.put(processor.type().toLowerCase(), processor);
                    AssertUtil.assertTrue(old == null, "重复注册处理器：{}", processor.type());
                });
        processorMap = tmpProcessorMap;
    }

    @Override public void onMessage(SocketSession socketSession, String message) {

        if (!gatewayService.getInitEnd().get()) {
            socketSession.close("等待服务器初始化完成");
            return;
        }

        ChatUser bindData = socketSession.bindData("user");
        log.debug("ws接受到消息: {}, {}, {}", socketSession, bindData, message);
        try {
            /*接受发过来的消息*/
            JSONObject jsonObject = FastJsonUtil.parseJSONObject(message);
            String cmd = jsonObject.getString("cmd");
            if (StringUtils.isBlank(cmd)) {
                socketSession.write(RunResult.fail("命令错误").toJSONString());
                return;
            }

            AbstractApp2GatewayMessageProcessor abstractProcessor = processorMap.get(cmd.toLowerCase());
            if (abstractProcessor != null) {
                if (abstractProcessor.checkLoginEnd()) {
                    if (bindData == null) {
                        socketSession.write(RunResult.fail("尚未登录").toJSONString());
                        return;
                    }
                }
                abstractProcessor.process(socketSession, bindData, jsonObject);
            } else {
                /*说明需要转发*/
                String roomId = jsonObject.getString("roomId");
                if (StringUtils.isBlank(roomId)) {
                    socketSession.write(RunResult.fail("房间ID异常").toJSONString());
                    return;
                }
                Integer room2ServerId = gatewayService.getRoomId4RoomServerMapping().get(roomId);
                if (room2ServerId == null) {
                    socketSession.write(RunResult.fail("房间不存在").toJSONString());
                    return;
                }
                Gateway2RoomServerSocketProxy gateway2RoomServerSocketClient = gatewayService.getRoomServerProxyMap().get(room2ServerId);
                if (gateway2RoomServerSocketClient == null) {
                    socketSession.write(RunResult.fail("房间不可用").toJSONString());
                    return;
                }
                SocketSession idle = gateway2RoomServerSocketClient.idle();
                if (idle != null) {
                    ForwardMessage.Gateway2RoomServer forwardMessage = new ForwardMessage.Gateway2RoomServer();
                    forwardMessage.setAccount(bindData.getName());
                    forwardMessage.setCmd(cmd);
                    forwardMessage.setMessage(jsonObject);
                    idle.writeAndFlush(forwardMessage.toJSONString());
                } else {
                    socketSession.write(RunResult.fail("服务器繁忙").toJSONString());
                }
            }
        } catch (Exception e) {
            log.error("ws处理消息异常: {} {}", socketSession, message, e);
            socketSession.write(RunResult.fail("服务器异常").toJSONString());
        }
    }

}
