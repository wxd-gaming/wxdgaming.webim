package wxdgaming.webim.login.module.user.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.ann.Param;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.format.HexId;
import wxdgaming.boot2.core.io.Objects;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.AssertUtil;
import wxdgaming.boot2.core.util.Md5Util;
import wxdgaming.boot2.core.util.RandomUtils;
import wxdgaming.boot2.core.util.SingletonLockUtil;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.bean.ServerMapping;
import wxdgaming.webim.login.module.inner.InnerService;
import wxdgaming.webim.login.module.user.ChatUserService;

import java.util.Collection;

/**
 * 注册接口
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-08 11:04
 **/
@Slf4j
@Singleton
@RequestMapping(path = "/api/chat/user")
public class ChatUserController {

    static final HexId hexId = new HexId(1);

    final InnerService innerService;
    final ChatUserService chatUserService;

    @Inject
    public ChatUserController(ChatUserService chatUserService, InnerService innerService) {
        this.chatUserService = chatUserService;
        this.innerService = innerService;
    }

    @HttpRequest
    public RunResult register(HttpContext httpContext, @Param(path = "name") String name, @Param(path = "password") String token) {

        AssertUtil.assertTrue(!StringUtils.isBlank(name) && StringUtils.length(name) >= 3 && StringUtils.length(name) <= 12, "用户名长度3 ~ 12");
        AssertUtil.assertTrue(!StringUtils.isBlank(token) && StringUtils.length(token) >= 3 && StringUtils.length(name) <= 64, "密码长度不能小于3");

        AssertUtil.assertTrue(!"系统".equals(name) && !"system".equalsIgnoreCase(name) && !"root".equalsIgnoreCase(name), "不符合规定");

        AssertUtil.assertTrue(wxdgaming.boot2.core.chatset.StringUtils.checkMatches(name, wxdgaming.boot2.core.chatset.StringUtils.PATTERN_ACCOUNT), "用户名不能包含特殊字符");

        SingletonLockUtil.lock(name);
        try {
            ChatUser chatUser = chatUserService.chatUser(name);
            if (chatUser != null) {
                return RunResult.fail("用户已存在");
            }
            chatUser = new ChatUser();
            chatUser.setName(name);
            chatUser.setToken(token);
            chatUserService.saveChatUser(chatUser);
            return login(httpContext, name, token);
        } finally {
            SingletonLockUtil.unlock(name);
        }
    }

    @HttpRequest
    public RunResult checkToken(HttpContext httpContext, @Param(path = "token") String token) {
        try {
            ChatUser chatUser = chatUserService.parseChatUser(token);
            return loginSuccess(chatUser);
        } catch (Exception e) {
            return RunResult.fail("token无效");
        }
    }

    @HttpRequest
    public RunResult login(HttpContext httpContext, @Param(path = "name") String name, @Param(path = "password") String token) {
        SingletonLockUtil.lock(name);
        try {
            ChatUser chatUser = chatUserService.chatUser(name.toLowerCase());
            if (chatUser == null) {
                return register(httpContext, name, token);
            }

            if (!Objects.equals(token, chatUser.getToken())) {
                return RunResult.fail("密码错误");
            }

            return loginSuccess(chatUser);
        } finally {
            SingletonLockUtil.unlock(name);
        }
    }

    RunResult loginSuccess(ChatUser chatUser) {
        String token = chatUserService.token(chatUser);

        if (StringUtils.isBlank(chatUser.getOpenId())) {
            chatUser.setOpenId(Md5Util.md5DigestEncode(chatUser.getName(), chatUserService.getOpenIdKey()));
            chatUserService.saveChatUser(chatUser);
        }

        Collection<ServerMapping> values = innerService.getGatewayServerMappingMap().values();
        ServerMapping serverMapping = RandomUtils.randomItem(values);
        String ip = "";
        int port = 0;
        if (serverMapping != null) {
            ip = serverMapping.getIp();
            port = serverMapping.getPort();
        }

        return RunResult.ok()
                .fluentPut("name", chatUser.getName())
                .fluentPut("openId", chatUser.getOpenId())
                .fluentPut("ip", ip)
                .fluentPut("port", port)
                .fluentPut("token", token);

    }


}
