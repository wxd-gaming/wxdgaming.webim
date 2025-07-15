package wxdgaming.webim.bean;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wxdgaming.boot2.core.lang.ObjectBase;

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
public class ServerMapping extends ObjectBase implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private int sid;
    private String ip;
    private int port;
    private Set<String> roomIds;

}
