package com.chat.proxy.websocket;

import com.chat.proxy.contant.Role;
import com.chat.proxy.model.MessageRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/websocket/{userId}")
@Component
public class ChatSocket {
    //实例一个session，这个session是websocket的session
    private Session session;
    private String userId;

    //userId:会话
    private static ConcurrentHashMap<String, ChatSocket> webSocketMap = new ConcurrentHashMap<>();


    //userId:聊天记录
    private static ConcurrentHashMap<String, MessageRecord> messageRecord = new ConcurrentHashMap<>();

    //新增一个方法用于主动向客户端发送消息
    public static void sendMessage(String message, String userId) {
        ChatSocket webSocket = webSocketMap.get(userId);
        if (webSocket != null) {
            try {
                webSocket.session.getBasicRemote().sendText(message);
                System.out.println("【websocket消息】发送消息成功,用户=" + userId + ",消息内容" + message.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void responseMessage(String message, Session session) {
        try {
            session.getBasicRemote().sendText(message);
            System.out.println("【websocket消息】发送消息成功,消息内容" + message.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ConcurrentHashMap<String, ChatSocket> getWebSocketMap() {
        return webSocketMap;
    }

    public void setWebSocketMap(ConcurrentHashMap<String, ChatSocket> webSocketMap) {
        ChatSocket.webSocketMap = webSocketMap;
    }

    //前端请求时一个websocket时
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.session = session;
        this.userId = userId;
        webSocketMap.put(userId, this);

        recordInit(userId);

        responseMessage("CONNECT_SUCCESS", session);
        System.out.println("【websocket消息】有新的连接,连接id" + userId);
    }

    //前端关闭时一个websocket时
    @OnClose
    public void onClose() {
        webSocketMap.remove(this);
        System.out.println("【websocket消息】连接断开,总数:" + webSocketMap.size());
    }

    //前端向后端发送消息
    @OnMessage
    public void onMessage(String message, Session session) {
        if (!message.equals("ping")) {
            System.out.println("【websocket消息】收到客户端发来的消息:" + message);
        }
        String reMsg = "这是答复" + System.currentTimeMillis();
        record(userId, message, reMsg);

        /////test
        MessageRecord messageRecord1 = messageRecord.get(userId);
        System.out.println(messageRecord1);

        responseMessage(reMsg, session);
    }

    public void recordInit(String userId) {
        if (!Optional.ofNullable(messageRecord.get(userId)).isPresent()) {
            MessageRecord newRecord = new MessageRecord();
            newRecord.addChat(Role.SYSTEM, "You are a helpful assistant.");
            messageRecord.put(userId, newRecord);
        }
    }

    public void record(String userId, String requestMsg, String responseMsg) {
        MessageRecord record = Optional.of(messageRecord.get(userId)).get();
        if (StringUtils.hasLength(requestMsg)) {
            record.addChat(Role.USER, requestMsg);
        }
        if (StringUtils.hasLength(responseMsg)) {
            record.addChat(Role.ASSISTANT, responseMsg);
        }
    }
}