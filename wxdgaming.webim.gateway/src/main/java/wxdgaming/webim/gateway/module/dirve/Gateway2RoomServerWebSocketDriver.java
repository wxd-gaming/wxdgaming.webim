package wxdgaming.webim.gateway.module.dirve;

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
import wxdgaming.boot2.starter.net.client.IClientWebSocketStringListener;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.gateway.module.GatewayService;

import java.util.HashMap;
import java.util.List;

/**
 * 网关和房间服务 字符串匹配
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-14 13:23
 */
@Slf4j
@Singleton
public class Gateway2RoomServerWebSocketDriver extends HoldRunApplication implements IClientWebSocketStringListener {

    HashMap<String, AbstractRoomServerMessageProcessor> processorMap = new HashMap<>();

    final GatewayService gatewayService;

    @Inject
    public Gateway2RoomServerWebSocketDriver(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @Init
    public void init() {
        HashMap<String, AbstractRoomServerMessageProcessor> tmpProcessorMap = new HashMap<>();
        runApplication.classWithSuper(AbstractRoomServerMessageProcessor.class)
                .forEach(processor -> {
                    AbstractRoomServerMessageProcessor old = tmpProcessorMap.put(processor.type().toLowerCase(), processor);
                    AssertUtil.assertTrue(old == null, "重复注册处理器：{}", processor.type());
                });
        processorMap = tmpProcessorMap;
    }

    @Override public void onMessage(SocketSession socketSession, String message) {
        ChatUser bindData = socketSession.bindData("user");
        log.debug("ws接受到消息: {}, {}, {}", socketSession, bindData, message);
        try {
            ForwardMessage.RoomServer2Gateway forwardMessage = FastJsonUtil.parse(message, ForwardMessage.RoomServer2Gateway.class);
            String cmd = forwardMessage.getCmd();
            if (StringUtils.isNotBlank(cmd)) {
                AbstractRoomServerMessageProcessor abstractProcessor = processorMap.get(cmd.toLowerCase());
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
        } catch (Exception e) {
            log.error("ws处理消息异常: {} {}", socketSession, message, e);
            socketSession.write(RunResult.fail("服务器异常").toJSONString());
        }
    }

}
