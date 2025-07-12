package wxdgaming.webim.bean;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wxdgaming.boot2.core.lang.ObjectBase;

import java.io.Serial;
import java.io.Serializable;

/**
 * 聊天用户
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-08 10:56
 **/
@Getter
@Setter
@Accessors(chain = true)
public class ChatUser extends ObjectBase implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private String name;
    private String token;
    private String openId;

}
