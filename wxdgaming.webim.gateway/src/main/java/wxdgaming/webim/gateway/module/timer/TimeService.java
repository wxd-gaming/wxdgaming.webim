package wxdgaming.webim.gateway.module.timer;

import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.BootConfig;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.Throw;
import wxdgaming.boot2.core.ann.Init;
import wxdgaming.boot2.core.ann.Value;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.executor.ExecutorWith;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.util.Md5Util;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.boot2.starter.net.client.SocketClientConfig;
import wxdgaming.boot2.starter.net.httpclient.HttpBuilder;
import wxdgaming.boot2.starter.net.httpclient.PostText;
import wxdgaming.boot2.starter.net.httpclient.Response;
import wxdgaming.boot2.starter.net.pojo.ProtoListenerFactory;
import wxdgaming.boot2.starter.net.server.SocketServerConfig;
import wxdgaming.boot2.starter.net.server.http.HttpListenerFactory;
import wxdgaming.boot2.starter.scheduled.ann.Scheduled;
import wxdgaming.webim.bean.ServerMapping;
import wxdgaming.webim.gateway.module.data.DataCenterService;
import wxdgaming.webim.gateway.module.service.Gateway2RoomServerSocketClientImpl;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    final DataCenterService dataCenterService;
    final ConcurrentHashMap<Integer, Gateway2RoomServerSocketClientImpl> roomServerMap = new ConcurrentHashMap<>();

    final ProtoListenerFactory protoListenerFactory;
    final HttpListenerFactory httpListenerFactory;
    final SocketClientConfig socketForwardConfig;

    @Inject
    public TimeService(DataCenterService dataCenterService,
                       ProtoListenerFactory protoListenerFactory,
                       HttpListenerFactory httpListenerFactory) {

        this.socketForwardConfig = BootConfig.getIns().getNestedValue("socket.client-forward", SocketClientConfig.class);

        this.dataCenterService = dataCenterService;
        this.protoListenerFactory = protoListenerFactory;
        this.httpListenerFactory = httpListenerFactory;
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

        ServerMapping serverMapping = new ServerMapping();
        serverMapping.setSid(sid);
        serverMapping.setPort(port);
        serverMapping.setRoomIds(Set.of());
        String jsonString = FastJsonUtil.toJSONString(serverMapping, SerializerFeature.SortField, SerializerFeature.MapSortField);

        String authorization = Md5Util.md5DigestEncode(jsonString, innerAuthorizationKey);

        Response<PostText> request = HttpBuilder.postJson(loginServerUrl + "/inner/syncGatewayServer", jsonString)
                .header(HttpHeaderNames.AUTHORIZATION, authorization)
                .request();

        if (!request.isSuccess()) {
            log.error("访问登陆服务器失败{}", Throw.ofString(request.getException(), false));
            return;
        }

        RunResult runResult = request.bodyRunResult();

        if (runResult.isFail()) {
            log.error("添加房间服务器映射失败:{}", runResult.msg());
            return;
        }

        ArrayList<ServerMapping> roomServerList = runResult.getObject("roomServerList", new TypeReference<ArrayList<ServerMapping>>() {});

        log.info("添加房间服务器映射结果:{}", roomServerList);

        for (ServerMapping mapping : roomServerList) {
            ServerMapping roomServerMapping = dataCenterService.getRoomMappings().computeIfAbsent(mapping.getSid(), k -> mapping);
            roomServerMapping.setRoomIds(mapping.getRoomIds());
            roomServerMapping.setIp(mapping.getIp());
            roomServerMapping.setPort(mapping.getPort());
            checkGatewaySession(roomServerMapping);
        }

    }

    /** 网关主动连游戏服 */
    public void checkGatewaySession(ServerMapping roomServerMapping) {

        Gateway2RoomServerSocketClientImpl gatewaySocketClient = roomServerMap.computeIfAbsent(sid, l -> {
            SocketClientConfig socketClientConfig = (SocketClientConfig) socketForwardConfig.clone();
            socketClientConfig.setHost(roomServerMapping.getIp());
            socketClientConfig.setPort(roomServerMapping.getPort());
            socketClientConfig.setMaxConnectionCount(1);
            socketClientConfig.setEnabledReconnection(false);
            Gateway2RoomServerSocketClientImpl socketClient = new Gateway2RoomServerSocketClientImpl(socketClientConfig);
            socketClient.init(protoListenerFactory, httpListenerFactory);
            return socketClient;
        });

        gatewaySocketClient.checkSync(null);

        SocketSession socketSession = gatewaySocketClient.idle();
        if (socketSession != null) {
            if (socketSession.isOpen()) {
                log.info("{}", roomServerMapping);
                socketSession.write(roomServerMapping);
            }
        }

    }

}
