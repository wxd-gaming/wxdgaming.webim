package wxdgaming.webim.gateway.module.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.webim.bean.ServerMapping;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据中心
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-13 10:31
 **/
@Slf4j
@Getter
@Singleton
public class DataCenterService extends HoldRunApplication {

    final ConcurrentHashMap<Integer, ServerMapping> roomMappings = new ConcurrentHashMap<>();

    @Inject
    public DataCenterService() {

    }

}
