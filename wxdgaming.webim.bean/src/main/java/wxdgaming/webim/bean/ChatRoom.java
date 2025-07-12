package wxdgaming.webim.bean;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wxdgaming.boot2.core.collection.MapOf;
import wxdgaming.boot2.core.lang.ObjectBase;
import wxdgaming.boot2.starter.net.SessionGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * 房间
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-08 10:58
 **/
@Getter
@Setter
@Accessors(chain = true)
public class ChatRoom extends ObjectBase implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private long roomId;
    private boolean system = false;
    /** 归属的角色 */
    private String master;
    /** 房间标题 */
    private String title;
    /** 进入房间的密钥 */
    private String token;
    private int maxUser;
    /** 最大历史记录 */
    private int maxHistory = 1000;

    private final List<String> historyList = new ArrayList<>();
    private final Set<String> userMap = new ConcurrentSkipListSet<>();

    private transient SessionGroup sessionGroup = new SessionGroup();

    public void addUser(String user) {
        userMap.add(user);
    }

    public boolean hasUser(String user) {
        return userMap.contains(user) || system;
    }

    public void removeUser(String user) {
        userMap.remove(user);
    }

    public void addHistory(String message) {
        synchronized (historyList) {
            historyList.add(message);
            if (historyList.size() > maxHistory) {
                historyList.removeFirst();
            }
        }
    }

    public Map<String, Object> toBean() {
        HashMap<String, Object> objectObjectHashMap = MapOf.newHashMap();
        objectObjectHashMap.put("roomId", String.valueOf(roomId));
        objectObjectHashMap.put("systemRoom", system ? 1 : 0);
        objectObjectHashMap.put("master", master);
        objectObjectHashMap.put("title", title);
        objectObjectHashMap.put("maxUser", String.valueOf(maxUser));
        objectObjectHashMap.put("innerUser", String.valueOf(userMap.size()));
        return objectObjectHashMap;
    }

    public List<String> roomHistory() {
        synchronized (historyList) {
            return new ArrayList<>(historyList);
        }
    }

    public SessionGroup getSessionGroup() {
        if (sessionGroup == null)
            sessionGroup = new SessionGroup();
        return sessionGroup;
    }
}
