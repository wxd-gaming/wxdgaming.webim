package wxdgaming.webim.service.module.chat.processor;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.ForwardMessage;
import wxdgaming.webim.bean.ServerMapping;
import wxdgaming.webim.service.module.chat.AbstractProcessor;

/**
 * 注册网关处理器
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
@Slf4j
@Singleton
public class RegisterGatewayProcessor extends AbstractProcessor {

    @Override public String type() {
        return "RegisterGateway";
    }

    @Override public void process(SocketSession socketSession, ForwardMessage.Gateway2RoomServer gateway2RoomServer) {
        ServerMapping javaObject = gateway2RoomServer.getMessage().toJavaObject(ServerMapping.class);
        dataService.getGatewayServerMappingMap().put(javaObject.getSid(), socketSession);
    }

}
