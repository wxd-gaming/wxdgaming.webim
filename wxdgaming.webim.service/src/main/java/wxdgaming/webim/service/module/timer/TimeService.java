package wxdgaming.webim.service.module.timer;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.Throw;
import wxdgaming.boot2.core.ann.Init;
import wxdgaming.boot2.core.ann.Value;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.executor.ExecutorWith;
import wxdgaming.boot2.core.util.Md5Util;
import wxdgaming.boot2.starter.net.httpclient5.HttpContent;
import wxdgaming.boot2.starter.net.httpclient5.PostRequest;
import wxdgaming.boot2.starter.net.server.SocketServerConfig;
import wxdgaming.boot2.starter.scheduled.ann.Scheduled;
import wxdgaming.webim.bean.ServerMapping;
import wxdgaming.webim.service.module.data.DataService;

import java.util.HashSet;

/**
 * 定时服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-12 22:14
 **/
@Slf4j
@Singleton
public class TimeService extends HoldRunApplication {

    int sid;
    int port;
    @Value(path = "loginServerUrl")
    String loginServerUrl;
    @Value(path = "innerAuthorizationKey")
    private String innerAuthorizationKey;

    final DataService dataService;

    @Inject
    public TimeService(DataService dataService) {
        this.dataService = dataService;
    }


    @Init
    public void init(@Value(path = "sid") int sid,
                     @Named("socket.server.config") SocketServerConfig serverConfig) {

        this.sid = sid;
        this.port = serverConfig.getPort();

    }

    /** 定时器同步信息到登录服务器 */
    @Scheduled(value = "*/5 * * *", async = true)
    @ExecutorWith(useVirtualThread = true)
    public void syncRoomServer2LoginServer() {

        ServerMapping serverMapping = new ServerMapping();
        serverMapping.setSid(sid);
        serverMapping.setPort(port);
        serverMapping.setRoomIds(new HashSet<>(this.dataService.getRoomMap().keySet()));
        String jsonString = FastJsonUtil.toJSONString(serverMapping, SerializerFeature.SortField, SerializerFeature.MapSortField);

        String authorization = Md5Util.md5DigestEncode(jsonString, innerAuthorizationKey);

        HttpContent execute = PostRequest.ofJson(loginServerUrl + "/inner/syncRoomServer", jsonString)
                .addHeader(HttpHeaderNames.AUTHORIZATION.toString(), authorization)
                .execute();

        if (!execute.isSuccess()) {
            log.error("访问登陆服务器失败{}", Throw.ofString(execute.getException(), false));
            return;
        }

        String bodyString = execute.bodyString();

        log.info("添加房间服务器映射结果:{}", bodyString);

    }

}
