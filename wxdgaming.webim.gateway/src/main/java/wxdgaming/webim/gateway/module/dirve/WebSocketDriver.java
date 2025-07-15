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
import wxdgaming.boot2.starter.net.pojo.IWebSocketStringListener;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.gateway.module.GatewayService;
import wxdgaming.webim.gateway.module.service.Gateway2RoomServerSocketProxy;

import java.util.HashMap;
import java.util.List;

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
            if (socketSession.getType() == SocketSession.Type.server) {
                /*接受发过来的消息*/
                JSONObject jsonObject = FastJsonUtil.parseJSONObject(message);
                String cmd = jsonObject.getString("cmd");
                if (StringUtils.isBlank(cmd)) {
                    socketSession.write(RunResult.fail("命令错误").toJSONString());
                    return;
                }

                AbstractProcessor abstractProcessor = processorMap.get(cmd.toLowerCase());
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
                        forwardMessage.setClientSessionId(socketSession.getUid());
                        forwardMessage.setAccount(bindData.getName());
                        forwardMessage.setCmd(cmd);
                        forwardMessage.setMessage(jsonObject);
                        idle.writeAndFlush(forwardMessage.toJSONString());
                    } else {
                        socketSession.write(RunResult.fail("服务器繁忙").toJSONString());
                    }
                }
            } else {
                ForwardMessage.RoomServer2Gateway forwardMessage = FastJsonUtil.parse(message, ForwardMessage.RoomServer2Gateway.class);
                String cmd = forwardMessage.getCmd();
                if (StringUtils.isNotBlank(cmd)) {
                    AbstractProcessor abstractProcessor = processorMap.get(cmd.toLowerCase());
                    abstractProcessor.process(socketSession, null, forwardMessage.getMessage());
                    return;
                }
                String jsonString = forwardMessage.getMessage().toJSONString();
                List<String> accountList = forwardMessage.getAccountList();
                for (String account : accountList) {
                    try {
                        SocketSession targetSession = gatewayService.getAccountSessionMappingMap().get(account);
                        if (targetSession != null) {
                            targetSession.write(jsonString);
                        }
                    } catch (Exception e) {
                        log.error("同步消息: {}, {}", account, jsonString, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("ws处理消息异常: {} {}", socketSession, message, e);
            socketSession.write(RunResult.fail("服务器异常").toJSONString());
        }
    }

}
