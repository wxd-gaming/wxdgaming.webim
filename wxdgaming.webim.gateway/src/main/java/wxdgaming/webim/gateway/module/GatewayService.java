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
import wxdgaming.boot2.starter.net.httpclient5.HttpResponse;
import wxdgaming.boot2.starter.net.httpclient5.HttpRequestPost;
import wxdgaming.boot2.starter.net.pojo.ProtoListenerFactory;
import wxdgaming.boot2.starter.net.server.SocketServerConfig;
import wxdgaming.boot2.starter.net.server.http.HttpListenerFactory;
import wxdgaming.boot2.starter.scheduled.ann.Scheduled;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ServerMapping;
import wxdgaming.webim.gateway.module.data.DataCenterService;
import wxdgaming.webim.gateway.module.service.Gateway2RoomServerSocketProxy;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    /**
     * 房间服务器映射
     * <p>key: 房间服务的进程id
     * <p>value: 房间服务代理
     */
    final ConcurrentHashMap<Integer, Gateway2RoomServerSocketProxy> roomServerProxyMap = new ConcurrentHashMap<>();
    /**
     * 房间服务器映射
     * <p>key: 房间id
     * <p>value: 房间服务器id
     */
    final ConcurrentHashMap<String, Integer> roomId4RoomServerMapping = new ConcurrentHashMap<>();
    /**
     * 房间服务的房间 数量
     * <p>key: 房间服务器id
     * <p>value: 房间数量
     */
    final ConcurrentHashMap<Integer, AtomicInteger> roomServerRoomCountMap = new ConcurrentHashMap<>();
    /**
     * 账号session映射
     * <p>key: 账号
     * <p>value: session
     */
    final ConcurrentHashMap<String, SocketSession> accountSessionMappingMap = new ConcurrentHashMap<>();

    final ProtoListenerFactory protoListenerFactory;
    final HttpListenerFactory httpListenerFactory;
    final SocketClientConfig socketForwardConfig;
    /** 连接成功同步用户到房间服务器 */
    final ConcurrentSkipListSet<Long> sendLoginUserSet = new ConcurrentSkipListSet<>();
    private final AtomicBoolean initEnd = new AtomicBoolean();

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
    @Scheduled(value = "*/5 * * *", async = true)
    @ExecutorWith(useVirtualThread = true)
    public void syncRoomServer2LoginServer() {

        ServerMapping gatewayServerMapping = new ServerMapping();
        gatewayServerMapping.setSid(sid);
        gatewayServerMapping.setPort(port);
        gatewayServerMapping.setRoomIds(Set.of());
        String jsonString = FastJsonUtil.toJSONString(gatewayServerMapping, SerializerFeature.SortField, SerializerFeature.MapSortField);

        String authorization = Md5Util.md5DigestEncode(jsonString, innerAuthorizationKey);

        HttpResponse execute = HttpRequestPost.ofJson(loginServerUrl + "/inner/syncGatewayServer", jsonString)
                .addHeader(HttpHeaderNames.AUTHORIZATION.toString(), authorization)
                .execute();

        if (!execute.isSuccess()) {
            log.error("访问登陆服务器失败{}", Throw.ofString(execute.getException(), false));
            return;
        }

        RunResult runResult = execute.bodyRunResult();

        if (runResult.isFail()) {
            log.error("添加房间服务器映射失败:{}", runResult.msg());
            return;
        }

        ArrayList<ServerMapping> roomServerList = runResult.getObject("roomServerList", new TypeReference<ArrayList<ServerMapping>>() {});

        log.debug("添加房间服务器映射结果:{}", roomServerList);

        ForwardMessage.Gateway2RoomServer gateway2RoomServer = new ForwardMessage.Gateway2RoomServer();
        gateway2RoomServer.setCmd("RegisterGateway");
        gateway2RoomServer.setMessage(gatewayServerMapping.toJSONObject());

        for (ServerMapping mapping : roomServerList) {
            ServerMapping roomServerMapping = dataCenterService.getRoomMappings().computeIfAbsent(mapping.getSid(), k -> mapping);
            roomServerMapping.setRoomIds(mapping.getRoomIds());
            roomServerMapping.setIp(mapping.getIp());
            roomServerMapping.setPort(mapping.getPort());
            checkGatewaySession(gateway2RoomServer.toJSONString(), roomServerMapping);
            AtomicInteger atomicInteger = new AtomicInteger();
            for (String roomId : roomServerMapping.getRoomIds()) {
                roomId4RoomServerMapping.put(roomId, roomServerMapping.getSid());
                atomicInteger.incrementAndGet();
            }
            roomServerRoomCountMap.put(roomServerMapping.getSid(), atomicInteger);
        }
        if (!initEnd.get() && !roomServerProxyMap.isEmpty()) {
            long count = roomServerProxyMap.values().stream().filter(proxy -> proxy.idle() != null).count();
            if (roomServerProxyMap.size() >= count) {
                initEnd.set(true);
                log.info("房间连接成功，游戏服数量：{}", roomServerProxyMap.size());
            }
        }
    }

    /** 网关主动连游戏服 */
    public void checkGatewaySession(String gatewayServerMapping, ServerMapping roomServerMapping) {

        Gateway2RoomServerSocketProxy gatewaySocketClient = roomServerProxyMap.computeIfAbsent(roomServerMapping.getSid(), l -> {
            SocketClientConfig socketClientConfig = (SocketClientConfig) socketForwardConfig.clone();
            socketClientConfig.setHost(roomServerMapping.getIp());
            socketClientConfig.setPort(roomServerMapping.getPort());
            socketClientConfig.setMaxConnectionCount(1);
            socketClientConfig.setEnabledReconnection(false);
            Gateway2RoomServerSocketProxy socketClient = new Gateway2RoomServerSocketProxy(socketClientConfig, roomServerMapping.getSid());
            socketClient.init(protoListenerFactory, httpListenerFactory);
            return socketClient;
        });

        gatewaySocketClient.checkSync(null);

        SocketSession socketSession = gatewaySocketClient.idle();
        if (socketSession != null) {
            if (socketSession.isOpen()) {
                log.debug("{}", roomServerMapping);
                socketSession.writeAndFlush(gatewayServerMapping);
                if (sendLoginUserSet.add(socketSession.getUid())) {
                    for (Map.Entry<String, SocketSession> entry : accountSessionMappingMap.entrySet()) {
                        String string = buildLogin2RoomServerMessage(entry.getKey());
                        socketSession.writeAndFlush(string);
                    }
                }
            }
        }
    }

    public String buildLogin2RoomServerMessage(String account) {
        ForwardMessage.Gateway2RoomServer forwardMessage = new ForwardMessage.Gateway2RoomServer();
        forwardMessage.setAccount(account);
        forwardMessage.setCmd("login");
        return forwardMessage.toJSONString();
    }

}
