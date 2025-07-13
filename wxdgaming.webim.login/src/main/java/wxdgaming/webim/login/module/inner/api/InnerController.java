package wxdgaming.webim.login.module.inner.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.ann.Body;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.starter.net.ChannelUtil;
import wxdgaming.boot2.starter.net.ann.HttpRequest;
import wxdgaming.boot2.starter.net.ann.RequestMapping;
import wxdgaming.boot2.starter.net.server.http.HttpContext;
import wxdgaming.webim.bean.ServerMapping;
import wxdgaming.webim.login.module.inner.InnerService;

import java.util.ArrayList;

/**
 * 内部转发
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-12 22:05
 **/
@Slf4j
@Singleton
@RequestMapping(path = "inner")
public class InnerController extends HoldRunApplication {

    final InnerService innerService;

    @Inject
    public InnerController(InnerService innerService) {
        this.innerService = innerService;
    }

    @HttpRequest()
    public RunResult syncRoomServer(HttpContext httpContext, @Body ServerMapping serverMapping) {
        String ip = ChannelUtil.getIP(httpContext.getCtx().channel());
        serverMapping.setIp(ip);
        log.info("syncRoomServer: {}", serverMapping);
        innerService.getRoomServerMappingMap().put(serverMapping.getSid(), serverMapping);
        return RunResult.OK;
    }

    @HttpRequest()
    public RunResult syncGatewayServer(HttpContext httpContext, @Body ServerMapping serverMapping) {
        String ip = ChannelUtil.getIP(httpContext.getCtx().channel());
        serverMapping.setIp(ip);
        log.info("syncGatewayServer: {}", serverMapping);
        innerService.getGatewayServerMappingMap().put(serverMapping.getSid(), serverMapping);
        return RunResult.ok().fluentPut("roomServerList", new ArrayList<>(innerService.getRoomServerMappingMap().values()));
    }

}
