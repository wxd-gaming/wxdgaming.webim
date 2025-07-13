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
    ConcurrentHashMap<Integer, ServerMapping> roomServerMappingMap = new ConcurrentHashMap<>();

}
