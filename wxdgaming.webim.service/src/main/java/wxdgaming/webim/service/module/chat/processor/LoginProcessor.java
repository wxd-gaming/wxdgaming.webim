package wxdgaming.webim.service.module.chat.processor;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.ann.Value;
import wxdgaming.boot2.core.token.JsonToken;
import wxdgaming.boot2.core.token.JsonTokenParse;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.service.module.chat.AbstractProcessor;

/**
 * 登录处理
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
@Slf4j
@Singleton
public class LoginProcessor extends AbstractProcessor {

    @Value(path = "json.token.key", nestedPath = true)
    private String jsonTokenKey;
    @Value(path = "openIdKey")
    private String openIdKey;

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

        log.info("用户登录: {} 上线 进入公共聊天室", chatUser.getName());

        chatService.sendRoomList(socketSession, chatUser);

        dataService.getRoomMap().values().stream()
                .filter(room -> room.hasUser(chatUser.getName()))
                .forEach(room -> {
                    chatService.systemTip(room, "%s 上线".formatted(chatUser.getName()));
                    if (room.isSystem()) {
                        room.getUserMap().add(chatUser.getName());
                    }
                    room.getSessionGroup().add(socketSession);
                });

        socketSession.getChannel().closeFuture().addListener(future -> {

            log.info("用户登录: {} 下线 退出公共聊天室", chatUser.getName());

            dataService.getRoomMap().values().stream()
                    .filter(room -> room.hasUser(chatUser.getName()))
                    .forEach(room -> {
                        if (room.isSystem()) {
                            room.getUserMap().remove(chatUser.getName());
                        }
                        chatService.systemTip(room, "%s 下线".formatted(chatUser.getName()));
                    });

        });

    }

}
