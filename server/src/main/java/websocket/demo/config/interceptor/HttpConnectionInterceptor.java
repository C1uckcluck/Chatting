package websocket.demo.config.interceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

/**
 * 웹 소켓 연결시 최초에는 http 로 연결이 이뤄지는데,
 * 이 http 연결시 사용할 수 있는 인터셉터
 *
 */
@Slf4j
public class HttpConnectionInterceptor implements HandshakeInterceptor {
    /**
     *  핸드셰이크가 이뤄지기 전 실행된는 것
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        log.info("Http connection 시작!");
        log.info(String.valueOf(request.getHeaders()));
        // 쿠키 로깅
        if(request instanceof ServletServerHttpRequest servletRequest){
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            Cookie[] cookies = httpServletRequest.getCookies();
            if(cookies != null){
                for (Cookie cookie : cookies) {
                    System.out.println("Cookie + " + cookie.getName() + "value : " + cookie.getValue());
                }
            }
        }
        // 여기서 JWT 검증을 시도하면 될듯
        return true;
    }

    /**
     * 핸드쉐이크가 수행되고 나서 실행
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        log.info("http connection 종료!");
    }
}
