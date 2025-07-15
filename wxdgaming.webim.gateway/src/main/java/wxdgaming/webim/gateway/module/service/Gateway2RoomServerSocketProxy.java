package wxdgaming.webim.gateway.module.service;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import lombok.Getter;
import wxdgaming.boot2.starter.net.ChannelUtil;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.boot2.starter.net.client.SocketClient;
import wxdgaming.boot2.starter.net.client.SocketClientConfig;
import wxdgaming.boot2.starter.net.pojo.ProtoListenerFactory;
import wxdgaming.boot2.starter.net.server.http.HttpListenerFactory;

import java.util.function.Consumer;

/**
 * 网关连接到房间服务的连接
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-14 13:26
 */
@Getter
public class Gateway2RoomServerSocketProxy extends SocketClient {

    private final int roomServerId;

    public Gateway2RoomServerSocketProxy(SocketClientConfig config, int roomServerId) {
        super(config);
        this.roomServerId = roomServerId;
    }

    @Override public void start(ProtoListenerFactory protoListenerFactory, HttpListenerFactory httpListenerFactory) {
        super.start(protoListenerFactory, httpListenerFactory);
    }

    @Override public void init(ProtoListenerFactory protoListenerFactory, HttpListenerFactory httpListenerFactory) {
        super.init(protoListenerFactory, httpListenerFactory);
    }

    @Override protected void addChanelHandler(SocketChannel socketChannel, ChannelPipeline pipeline) {
        super.addChanelHandler(socketChannel, pipeline);
        ChannelUtil.attr(socketChannel, "inner-channel", true);
    }

    @Override public ChannelFuture connect(Consumer<SocketSession> consumer) {
        return super.connect((socketSession) -> {
            if (consumer != null) {
                consumer.accept(socketSession);
            }
        });
    }

    @Override public ChannelFuture connect(String inetHost, int inetPort, Consumer<SocketSession> consumer) {
        return super.connect(inetHost, inetPort, consumer);
    }

}
