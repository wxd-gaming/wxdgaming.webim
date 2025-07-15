package wxdgaming.webim.service.module.chat.processor;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import wxdgaming.boot2.core.util.AssertUtil;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.webim.AbstractProcessor;
import wxdgaming.webim.bean.ChatRoom;
import wxdgaming.webim.bean.ChatUser;
import wxdgaming.webim.service.module.chat.ChatService;
import wxdgaming.webim.service.module.data.DataService;

/**
 * 创建房间
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-10 11:29
 **/
@Slf4j
@Singleton
public class NewRoomProcessor extends AbstractProcessor {

    final DataService dataService;
    final ChatService chatService;

    @Inject
    public NewRoomProcessor(DataService dataService, ChatService chatService) {
        this.dataService = dataService;
        this.chatService = chatService;
    }

    @Override public String type() {
        return "NewRoom";
    }

    @Override public void process(SocketSession socketSession, ChatUser self, JSONObject jsonObject) {

        String title = jsonObject.getString("title");
        AssertUtil.assertTrue(!StringUtils.isBlank(title) && StringUtils.length(title) >= 3 && StringUtils.length(title) <= 12, "名字长度 3 ~ 8");

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomId(dataService.getAtomicLong().incrementAndGet());
        chatRoom.setTitle(title);
        chatRoom.setToken(jsonObject.getString("token"));
        chatRoom.setMaster(self.getName());
        chatRoom.setMaxUser(1000);
        chatRoom.addUser(self.getName());
        dataService.getRoomMap().put(chatRoom.getRoomId(), chatRoom);

        chatRoom.getSessionGroup().add(socketSession);

        chatService.sendRoomList(socketSession, self);
    }

}
