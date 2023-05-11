package com.chat.proxy.websocket;

import com.chat.proxy.contant.Role;
import com.chat.proxy.model.Chat;
import com.chat.proxy.model.MessageRecord;
import com.chat.proxy.model.OpenApiRequest;
import com.chat.proxy.model.OpenApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

        MessageRecord recordHistory = storeUserRecord(userId, message);
        Chat reChat = postOpenApiChat(recordHistory);
        storeUserRecord(userId, reChat.getContent());

        responseMessage(reChat.getContent(), session);
    }

    public void recordInit(String userId) {
        if (!Optional.ofNullable(messageRecord.get(userId)).isPresent()) {
            MessageRecord newRecord = new MessageRecord();
            newRecord.addChat(Role.SYSTEM, "You are a helpful assistant.");
            messageRecord.put(userId, newRecord);
        }
    }

    public MessageRecord storeUserRecord(String userId, String Msg) {
        MessageRecord record = Optional.of(messageRecord.get(userId)).get();
        if (StringUtils.hasLength(Msg)) {
            record.addChat(Role.USER, Msg);
        }
        return record;
    }
    public MessageRecord storeAssistantRecord(String userId, String Msg) {
        MessageRecord record = Optional.of(messageRecord.get(userId)).get();
        if (StringUtils.hasLength(Msg)) {
            record.addChat(Role.ASSISTANT, Msg);
        }
        return record;
    }

    private Chat postOpenApiChat(MessageRecord record) {

        //设置代理
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 58591));

        //配置HTTP超时时间
        SimpleClientHttpRequestFactory httpRequestFactory = new SimpleClientHttpRequestFactory();
        httpRequestFactory.setConnectTimeout(600000);
        httpRequestFactory.setReadTimeout(600000);
//        httpRequestFactory.setProxy(proxy);

        RestTemplate restTemplate = new RestTemplate(httpRequestFactory);

        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        String url = "https://api.openai.com/v1/chat/completions";
        //设置请求头参数
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/json");
        httpHeaders.add("Authorization", "Bearer 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");

        //设置body参数
        OpenApiRequest req = new OpenApiRequest();
        req.setMessages(record.getChatList());
        req.setModel("gpt-3.5-turbo");
        ObjectMapper mapper = new ObjectMapper();
        String reqContent = "";
        try {
            reqContent = mapper.writeValueAsString(req);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        HttpEntity<String> httpEntity = new HttpEntity<>(reqContent, httpHeaders);

        OpenApiResponse resp = new OpenApiResponse();
        try {
            resp = restTemplate.postForObject(url, httpEntity, OpenApiResponse.class);
        } catch (Exception e) {
            System.out.println(e);
        }
        return resp.getChoices().get(0).getMessage();
    }
}