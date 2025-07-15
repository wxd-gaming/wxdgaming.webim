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
import wxdgaming.boot2.core.util.AssertUtil;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.boot2.starter.net.pojo.IWebSocketStringListener;
import wxdgaming.webim.AbstractProcessor;
import wxdgaming.webim.bean.ChatUser;
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
public class WebSocketStringListener extends HoldRunApplication implements IWebSocketStringListener {

    final ChatService chatService;
    final DataService dataService;

    HashMap<String, AbstractProcessor> processorMap = new HashMap<>();

    @Inject
    public WebSocketStringListener(ChatService chatService, DataService dataService) {
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
        ChatUser bindData = socketSession.bindData("user");
        log.debug("ws接受到消息: {}, {}, {}", socketSession, bindData, message);
        try {

            /*接受发过来的消息*/
            JSONObject jsonObject = FastJsonUtil.parseJSONObject(message);
            String cmd = jsonObject.getString("cmd");
            if (StringUtils.isBlank(cmd)) {
                Utils.fail(socketSession, ("命令错误"));
                return;
            }

            AbstractProcessor abstractProcessor = processorMap.get(cmd.toLowerCase());
            if (abstractProcessor == null) {
                Utils.fail(socketSession, ("命令错误"));
                return;
            }
            if (abstractProcessor.checkLoginEnd()) {
                if (bindData == null) {
                    Utils.fail(socketSession, ("尚未登录"));
                    return;
                }
            }
            abstractProcessor.process(socketSession, bindData, jsonObject);
        } catch (Exception e) {
            log.error("处理异常: {}, {}", socketSession, message, e);
            if (e instanceof AssertException assertException) {
                Utils.fail(socketSession, assertException.getMessage());
                return;
            }
            Utils.fail(socketSession, "服务器异常");
        }
    }


}
