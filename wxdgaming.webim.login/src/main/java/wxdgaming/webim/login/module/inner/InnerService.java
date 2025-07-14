package wxdgaming.webim.login.module.inner;

import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.webim.bean.ServerMapping;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 内部服务
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-12 22:04
 **/
@Slf4j
@Getter
@Singleton
public class InnerService extends HoldRunApplication {

    ConcurrentHashMap<Integer, ServerMapping> gatewayServerMappingMap = new ConcurrentHashMap<>();
    /**
     * 房间服务器映射
     * <p>key: 进程id
     * <p>value: 房间服务器映射
     */
    ConcurrentHashMap<Integer, ServerMapping> room4ServerMappingMap = new ConcurrentHashMap<>();

}
