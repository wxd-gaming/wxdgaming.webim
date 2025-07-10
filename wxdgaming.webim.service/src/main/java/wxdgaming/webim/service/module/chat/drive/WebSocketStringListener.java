package wxdgaming.webim.service.module.chat.drive;

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
import wxdgaming.webim.service.bean.ChatUser;
import wxdgaming.webim.service.module.chat.AbstractProcessor;
import wxdgaming.webim.service.module.chat.ChatService;
import wxdgaming.webim.service.module.data.DataService;
import wxdgaming.webim.service.module.user.ChatUserService;

import java.util.HashMap;

/**
 * websocket string消息监听
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-08 15:47
 **/
@Slf4j
@Singleton
public class WebSocketStringListener extends HoldRunApplication implements IWebSocketStringListener {

    final ChatService chatService;
    final ChatUserService chatUserService;
    final DataService dataService;

    HashMap<String, AbstractProcessor> processorMap = new HashMap<>();

    @Inject
    public WebSocketStringListener(ChatService chatService, ChatUserService chatUserService, DataService dataService) {
        this.chatService = chatService;
        this.chatUserService = chatUserService;
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
        ChatUser bindData = socketSession.bindData("user");
        log.debug("ws接受到消息: {}, {}, {}", socketSession, bindData, message);
        try {

            /*接受发过来的消息*/
            JSONObject jsonObject = FastJsonUtil.parseJSONObject(message);
            String cmd = jsonObject.getString("cmd");
            if (StringUtils.isBlank(cmd)) {
                chatService.fail(socketSession, ("命令错误"));
                return;
            }

            AbstractProcessor abstractProcessor = processorMap.get(cmd.toLowerCase());
            if (abstractProcessor == null) {
                chatService.fail(socketSession, ("命令错误"));
                return;
            }
            if (abstractProcessor.checkLoginEnd()) {
                if (bindData == null) {
                    chatService.fail(socketSession, ("尚未登录"));
                    return;
                }
            }
            abstractProcessor.process(socketSession, bindData, jsonObject);
        } catch (Exception e) {
            log.error("处理异常: {}, {}", socketSession, message, e);
            chatService.fail(socketSession, "服务器异常");
        }
    }


}
