package wxdgaming.webim.service.module.chat.drive;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.ann.Init;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.lang.AssertException;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.AssertUtil;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.boot2.starter.net.pojo.IWebSocketStringListener;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.service.module.chat.AbstractProcessor;
import wxdgaming.webim.service.module.chat.ChatService;
import wxdgaming.webim.service.module.data.DataService;
import wxdgaming.webim.util.Utils;

import java.util.HashMap;

/**
 * websocket string消息监听
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-08 15:47
 **/
@Slf4j
@Singleton
public class WebSocketStringDrive extends HoldRunApplication implements IWebSocketStringListener {

    final ChatService chatService;
    final DataService dataService;

    HashMap<String, AbstractProcessor> processorMap = new HashMap<>();

    @Inject
    public WebSocketStringDrive(ChatService chatService, DataService dataService) {
        this.chatService = chatService;
        this.dataService = dataService;
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
        log.debug("ws接受到消息: {}, {}", socketSession, message);
        final ForwardMessage.Gateway2RoomServer gateway2RoomServer = FastJsonUtil.parse(message, ForwardMessage.Gateway2RoomServer.class);
        try {
            /*接受发过来的消息*/
            String cmd = gateway2RoomServer.getCmd();
            if (StringUtils.isBlank(cmd)) {
                dataService.sendMessage2Gateway(socketSession, gateway2RoomServer, RunResult.fail("命令错误"));
                return;
            }

            AbstractProcessor abstractProcessor = processorMap.get(cmd.toLowerCase());
            if (abstractProcessor == null) {
                dataService.sendMessage2Gateway(socketSession, gateway2RoomServer, RunResult.fail("命令错误"));
                return;
            }
            abstractProcessor.process(socketSession, gateway2RoomServer);
        } catch (Exception e) {
            log.error("处理异常: {}, {}", socketSession, message, e);
            if (e instanceof AssertException assertException) {
                dataService.sendMessage2Gateway(socketSession, gateway2RoomServer, RunResult.fail(assertException.getMessage()));
                return;
            }
            dataService.sendMessage2Gateway(socketSession, gateway2RoomServer, RunResult.fail("服务器异常"));
        }
    }


}
