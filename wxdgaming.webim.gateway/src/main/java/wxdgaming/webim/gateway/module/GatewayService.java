package wxdgaming.webim.gateway.module;

import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.Getter;
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
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ServerMapping;
import wxdgaming.webim.gateway.module.data.DataCenterService;
import wxdgaming.webim.gateway.module.service.Gateway2RoomServerSocketClientImpl;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-12 21:53
 **/
@Slf4j
@Getter
@Singleton
public class GatewayService extends HoldRunApplication {

    int sid;
    int port;
    @Value(path = "loginServerUrl")
    String loginServerUrl;
    @Value(path = "innerAuthorizationKey")
    private String innerAuthorizationKey;
    final DataCenterService dataCenterService;
    final ConcurrentHashMap<Integer, Gateway2RoomServerSocketClientImpl> roomServerMap = new ConcurrentHashMap<>();
    /**
     * 房间服务器映射
     * <p>key: 房间id
     * <p>value: 房间服务器id
     */
    final ConcurrentHashMap<Long, Integer> roomId4RoomServerMapping = new ConcurrentHashMap<>();
    /**
     * 账号session映射
     * <p>key: 账号
     * <p>value: session
     */
    final ConcurrentHashMap<String, SocketSession> accountSessionMappingMap = new ConcurrentHashMap<>();

    final ProtoListenerFactory protoListenerFactory;
    final HttpListenerFactory httpListenerFactory;
    final SocketClientConfig socketForwardConfig;

    @Inject
    public GatewayService(DataCenterService dataCenterService,
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

        ServerMapping gatewayServerMapping = new ServerMapping();
        gatewayServerMapping.setSid(sid);
        gatewayServerMapping.setPort(port);
        gatewayServerMapping.setRoomIds(Set.of());
        String jsonString = FastJsonUtil.toJSONString(gatewayServerMapping, SerializerFeature.SortField, SerializerFeature.MapSortField);

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

        ForwardMessage.Gateway2RoomServer gateway2RoomServer = new ForwardMessage.Gateway2RoomServer();
        gateway2RoomServer.setCmd("RegisterGateway");
        gateway2RoomServer.setMessage(gatewayServerMapping.toJSONObject());

        for (ServerMapping mapping : roomServerList) {
            ServerMapping roomServerMapping = dataCenterService.getRoomMappings().computeIfAbsent(mapping.getSid(), k -> mapping);
            roomServerMapping.setRoomIds(mapping.getRoomIds());
            roomServerMapping.setIp(mapping.getIp());
            roomServerMapping.setPort(mapping.getPort());
            checkGatewaySession(gateway2RoomServer.toJSONString(), roomServerMapping);
            for (Long roomId : roomServerMapping.getRoomIds()) {
                roomId4RoomServerMapping.put(roomId, roomServerMapping.getSid());
            }
        }

    }

    /** 网关主动连游戏服 */
    public void checkGatewaySession(String gatewayServerMapping, ServerMapping roomServerMapping) {

        Gateway2RoomServerSocketClientImpl gatewaySocketClient = roomServerMap.computeIfAbsent(sid, l -> {
            SocketClientConfig socketClientConfig = (SocketClientConfig) socketForwardConfig.clone();
            socketClientConfig.setHost(roomServerMapping.getIp());
            socketClientConfig.setPort(roomServerMapping.getPort());
            socketClientConfig.setMaxConnectionCount(1);
            socketClientConfig.setEnabledReconnection(false);
            Gateway2RoomServerSocketClientImpl socketClient = new Gateway2RoomServerSocketClientImpl(socketClientConfig, roomServerMapping.getSid());
            socketClient.init(protoListenerFactory, httpListenerFactory);
            return socketClient;
        });

        gatewaySocketClient.checkSync(null);

        SocketSession socketSession = gatewaySocketClient.idle();
        if (socketSession != null) {
            if (socketSession.isOpen()) {
                log.info("{}", roomServerMapping);

                socketSession.writeAndFlush(gatewayServerMapping);
            }
        }

    }
}
