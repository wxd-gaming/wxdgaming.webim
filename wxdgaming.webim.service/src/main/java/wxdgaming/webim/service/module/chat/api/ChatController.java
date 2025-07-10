package wxdgaming.webim.service.module.chat.api;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.HoldRunApplication;
import wxdgaming.boot2.core.chatset.StringUtils;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.lang.RunResult;
import wxdgaming.boot2.core.timer.MyClock;
import wxdgaming.boot2.starter.net.SocketSession;
import wxdgaming.boot2.starter.net.pojo.IWebSocketStringListener;
import wxdgaming.boot2.starter.scheduled.ann.Scheduled;
import wxdgaming.webim.service.bean.ChatRoom;
import wxdgaming.webim.service.bean.ChatUser;
import wxdgaming.webim.service.module.chat.ChatService;
import wxdgaming.webim.service.module.data.DataService;
import wxdgaming.webim.service.module.user.ChatUserService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天接口
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-07-08 15:47
 **/
@Slf4j
@Singleton
public class ChatController extends HoldRunApplication implements IWebSocketStringListener {

    final ChatService chatService;
    final ChatUserService chatUserService;
    final DataService dataService;

    private final ConcurrentHashMap<String, SocketSession> onlineUserMap = new ConcurrentHashMap<>();

    @Inject
    public ChatController(ChatService chatService, ChatUserService chatUserService, DataService dataService) {
        this.chatService = chatService;
        this.chatUserService = chatUserService;
        this.dataService = dataService;
    }

    @Scheduled("*/5 * * * * *")
    public void scheduled() {
        ChatRoom publicChatRoom = dataService.getRoomMap().get(1L);
        systemTip(publicChatRoom, "请文明聊天，合法合规；请勿发布违法信息，否则将被封号！");
    }

    public void systemTip(ChatRoom chatRoom, String content) {
        RunResult ok = RunResult.ok();
        ok.fluentPut("cmd", "roomMsg");
        ok.fluentPut("roomId", chatRoom.getRoomId());
        ok.fluentPut("sender", "系统");
        ok.fluentPut("time", MyClock.nowString());
        ok.fluentPut("type", "system");
        ok.fluentPut("content", content);
        chatRoom.getSessionGroup().write(ok.toJSONString());
    }

    @Override public void onMessage(SocketSession socketSession, String message) {
        ChatUser bindData = socketSession.bindData("user");
        log.info("ws接受到消息: {}, {}, {}", socketSession, bindData, message);
        try {

            /*接受发过来的消息*/
            JSONObject jsonObject = FastJsonUtil.parseJSONObject(message);
            String cmd = jsonObject.getString("cmd");
            if (StringUtils.isBlank(cmd)) {
                fail(socketSession, ("命令错误"));
                return;
            }
            if ("ping".equals(cmd)) {
                return;
            } else if ("login".equals(cmd)) {

                String token = jsonObject.getString("token");
                ChatUser chatUser = this.chatUserService.parseChatUser(token);
                socketSession.bindData("user", chatUser);


                onlineUserMap.put(chatUser.getName(), socketSession);
                log.info("用户登录: {} 上线 进入公共聊天室", chatUser.getName());

                dataService.getRoomMap().values().stream()
                        .filter(room -> room.hasUser(chatUser.getName()))
                        .forEach(room -> {
                            room.getSessionGroup().add(socketSession);
                            systemTip(room, "%s 进入聊天室".formatted(chatUser.getName()));
                        });

                socketSession.getChannel().closeFuture().addListener(future -> {

                    onlineUserMap.remove(chatUser.getName());
                    log.info("用户登录: {} 下线 退出公共聊天室", chatUser.getName());

                    dataService.getRoomMap().values().stream()
                            .filter(room -> room.hasUser(chatUser.getName()))
                            .forEach(room -> {
                                systemTip(room, "%s 退出聊天室".formatted(chatUser.getName()));
                            });

                });
                {
                    /*临时*/
                    ChatRoom chatRoom = dataService.getRoomMap().get(2L);
                    chatRoom.getUserMap().add(chatUser.getName());
                    chatRoom.getSessionGroup().add(socketSession);
                }
            } else {
                if (bindData == null) {
                    fail(socketSession, ("尚未登录"));
                    return;
                }
                switch (cmd) {
                    case "roomMsg": {
                        long roomId = jsonObject.getLongValue("roomId");
                        ChatRoom chatRoom = dataService.getRoomMap().get(roomId);
                        if (chatRoom == null) {
                            fail(socketSession, "房间不存在");
                            return;
                        }
                        if (!chatRoom.getUserMap().contains(bindData.getName())) {
                            fail(socketSession, "尚未加入该房间");
                            return;
                        }
                        RunResult ok = RunResult.ok();
                        ok.fluentPut("cmd", "roomMsg");
                        ok.fluentPut("roomId", roomId);
                        ok.fluentPut("sender", bindData.getName());
                        ok.fluentPut("time", MyClock.nowString());
                        ok.fluentPut("type", jsonObject.getString("type"));
                        ok.fluentPut("content", jsonObject.getString("content"));
                        chatRoom.getSessionGroup().write(ok.toJSONString());
                    }
                    break;
                    case "newRoom": {
                        ChatRoom chatRoom = new ChatRoom();
                        chatRoom.setRoomId(dataService.getAtomicLong().incrementAndGet());
                        chatRoom.setTitle(jsonObject.getString("title"));
                        chatRoom.setToken(jsonObject.getString("token"));
                        chatRoom.setMaster(bindData.getName());
                        chatRoom.getUserMap().add(bindData.getName());
                        chatRoom.setMaxUser(1000);
                        dataService.getRoomMap().put(chatRoom.getRoomId(), chatRoom);

                        chatRoom.getSessionGroup().add(socketSession);

                        List<Map<String, String>> roomList = dataService.roomListBean(bindData);

                        RunResult ok = RunResult.ok();
                        ok.fluentPut("cmd", "newRoom");
                        ok.fluentPut("roomList", roomList);
                        socketSession.write(ok.toJSONString());
                    }
                    break;
                    case "private": {
                        String targetUserName = jsonObject.getString("targetUser");
                        String content = jsonObject.getString("content");
                        SocketSession targetUserSession = onlineUserMap.get(targetUserName);
                        if (targetUserSession == null) {
                            fail(socketSession, ("用户不在线"));
                            return;
                        }
                        RunResult ok = RunResult.ok();
                        ok.fluentPut("cmd", "private");
                        ok.fluentPut("send", bindData.getName());
                        ok.fluentPut("target", targetUserName);
                        ok.fluentPut("time", MyClock.nowString());
                        ok.fluentPut("content", content);
                        targetUserSession.write(ok.toJSONString());
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.error("处理异常: {}, {}", socketSession, message, e);
            fail(socketSession, "服务器异常");
        }
    }

    void fail(SocketSession socketSession, String msg) {
        socketSession.write(RunResult.fail(msg).toJSONString());
    }


}
