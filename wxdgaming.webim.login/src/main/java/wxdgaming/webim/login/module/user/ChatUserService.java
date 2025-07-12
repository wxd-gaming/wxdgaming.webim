package wxdgaming.webim.login.module.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.ann.Value;
import wxdgaming.boot2.core.token.JsonToken;
import wxdgaming.boot2.core.token.JsonTokenBuilder;
import wxdgaming.boot2.core.token.JsonTokenParse;
import wxdgaming.boot2.core.util.AssertUtil;
import wxdgaming.boot2.starter.batis.mapdb.HoldMap;
import wxdgaming.boot2.starter.batis.mapdb.MapDBDataHelper;
import wxdgaming.webim.bean.ChatUser;

import java.util.concurrent.TimeUnit;

/**
 * 服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-12 21:16
 **/
@Slf4j
@Getter
@Singleton
public class ChatUserService extends HoldRunApplication {

    @Value(path = "json.token.key", nestedPath = true)
    private String jsonTokenKey;
    @Value(path = "openIdKey")
    private String openIdKey;

    final MapDBDataHelper mapDBDataHelper;
    final HoldMap chatUserDbService;

    @Inject
    public ChatUserService(MapDBDataHelper mapDBDataHelper) {
        this.mapDBDataHelper = mapDBDataHelper;
        chatUserDbService = mapDBDataHelper.bMap("chat-user");
    }

    public String token(ChatUser chatUser) {
        JsonTokenBuilder ss = JsonTokenBuilder.of(jsonTokenKey, TimeUnit.DAYS, 7);
        ss.put("name", chatUser.getName());
        ss.put("openId", chatUser.getOpenId());
        return ss.compact();
    }

    public ChatUser parseChatUser(String token) {
        JsonToken jsonToken = JsonTokenParse.parse(jsonTokenKey, token);
        String name = jsonToken.getString("name");
        ChatUser chatUser = chatUser(name);
        AssertUtil.assertNull(chatUser, "用户不存在");
        return chatUser;
    }

    /** 查询用户 */
    public ChatUser chatUser(String name) {
        return chatUserDbService.get(name.toLowerCase());
    }

    /** 添加用户 */
    public void saveChatUser(ChatUser chatUser) {
        chatUserDbService.put(chatUser.getName().toLowerCase(), chatUser);
    }

}
