package wxdgaming.webim.bean;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wxdgaming.boot2.core.lang.ObjectBase;
import wxdgaming.boot2.starter.net.SocketSession;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 房间服务映射
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-12 21:57
 **/
@Getter
@Setter
@Accessors(chain = true)
public class RoomServerMapping extends ObjectBase implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private int sid;
    private String ip;
    private int port;
    private Set<Long> roomIds;
    @JSONField(serialize = false, deserialize = false)
    private transient SocketSession socketSession;

}
