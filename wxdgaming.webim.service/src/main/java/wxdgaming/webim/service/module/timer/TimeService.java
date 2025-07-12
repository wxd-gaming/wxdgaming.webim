package wxdgaming.webim.service.module.timer;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.ann.Init;
import wxdgaming.boot2.core.ann.Value;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.executor.ExecutorWith;
import wxdgaming.boot2.core.util.Md5Util;
import wxdgaming.boot2.starter.net.httpclient.HttpBuilder;
import wxdgaming.boot2.starter.net.server.SocketServerConfig;
import wxdgaming.boot2.starter.scheduled.ann.Scheduled;
import wxdgaming.webim.bean.RoomServerMapping;
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
    @Scheduled(value = "*/10 * * *", async = true)
    @ExecutorWith(useVirtualThread = true)
    public void syncRoomServer2LoginServer() {

        RoomServerMapping roomServerMapping = new RoomServerMapping();
        roomServerMapping.setSid(sid);
        roomServerMapping.setPort(port);
        roomServerMapping.setRoomIds(new HashSet<>(this.dataService.getRoomMap().keySet()));
        String jsonString = FastJsonUtil.toJSONString(roomServerMapping, SerializerFeature.SortField, SerializerFeature.MapSortField);

        String authorization = Md5Util.md5DigestEncode(jsonString, innerAuthorizationKey);

        String bodyString = HttpBuilder.postJson(loginServerUrl + "/inner/syncRoomServer", jsonString)
                .header(HttpHeaderNames.AUTHORIZATION, authorization)
                .request()
                .bodyString();

        log.info("添加房间服务器映射结果:{}", bodyString);

    }

}
