package websocket.demo.config.handler;


import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

// 텍스트 기반의 WebSocket 메세지 처리를 수행하는 handler
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 웹 소켓 연결을 괸리하는 리스트
    private static final ConcurrentHashMap<String, WebSocketSession> clientSession = new ConcurrentHashMap<>();

    /**
     * Websocket 연결에 성공한 경우
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("afterConnectionEstablished :: " + session.getId());
        clientSession.put(session.getId(), session);
    }

    /**
     * 메세지가 전달된 경우 호출되어 메세지를 전송함
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("HandleTextMessage :: " + session);
        System.out.println("HandleTextMessage :: " + message.getPayload());

        clientSession.forEach((key, value) -> {
            System.out.println(":: key " + key + " value :: " + value);
            // 같은 아이디아 아니라면 메세지 전달
            if (!key.equals(session.getId())) {
                try {
                    value.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 웹소켓 연결 해제시 세션 제거
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        clientSession.remove(session.getId());
        System.out.println("afterConnectionClosed :: Session : " + session.getId() + ", CloseStatus : " + status);
    }
}
