package com.sky.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务
 */
@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {

    /**
     * 存放会话对象。
     * 必须声明为 static：Tomcat 会为每个连接 new 一个本类实例，
     * 而 Spring 注入给 OrderServiceImpl 的是另一个单例，
     * 只有静态成员才能在两者之间共享同一份会话集合。
     */
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        sessionMap.put(sid, session);
        log.info("客户端：{} 建立连接，当前在线 {} 个", sid, sessionMap.size());
    }

    /**
     * 收到客户端消息后调用的方法
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("收到来自客户端：{} 的信息：{}", sid, message);
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        sessionMap.remove(sid);
        log.info("连接断开：{}，当前在线 {} 个", sid, sessionMap.size());
    }

    /**
     * 连接发生异常时调用的方法
     */
    @OnError
    public void onError(Session session, Throwable error, @PathParam("sid") String sid) {
        log.error("连接异常：{}", sid, error);
        sessionMap.remove(sid);
    }

    /**
     * 群发消息
     */
    public void sendToAllClient(String message) {
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            try {
                // 服务器向客户端发送消息
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                log.error("向客户端推送消息失败", e);
            }
        }
    }
}
