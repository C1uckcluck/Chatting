package websocket.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import websocket.demo.dto.ChatMessageDto;
import websocket.demo.dto.ChatMessageType;
import websocket.demo.dto.LoginDto;
import websocket.demo.dto.SignupDto;
import websocket.demo.dto.ApiResponse;
import websocket.demo.dto.ChatRoomDto;
import websocket.demo.dto.CreateChatRoomRequest;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketChatIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private WebSocketStompClient stompClient;

    private String jsessionId; // 로그인 후 획득한 세션 ID
    private String roomId; // 생성된 채팅방 ID

    @BeforeEach
    public void setup() {
        // 1. WebSocket 클라이언트 설정 (SockJS 사용)
        StandardWebSocketClient standardWebSocketClient = new StandardWebSocketClient();
        WebSocketTransport webSocketTransport = new WebSocketTransport(standardWebSocketClient);
        List<Transport> transports = Collections.singletonList(webSocketTransport);
        SockJsClient sockJsClient = new SockJsClient(transports);

        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // 2. 회원가입 및 로그인하여 세션 획득
        registerAndLogin();

        // 3. 채팅방 생성
        createChatRoom();
    }

    private void registerAndLogin() {
        // 회원가입
        SignupDto signupDto = new SignupDto("wsUser", "password", "wsNick");
        ResponseEntity<String> signupResponse = restTemplate.postForEntity("/auth/signup", signupDto, String.class);
        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 로그인
        LoginDto loginDto = new LoginDto("wsUser", "password");
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/login", loginDto, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // 세션 쿠키 추출
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.startsWith("JSESSIONID")) {
                    jsessionId = cookie.split(";")[0];
                    System.out.println("Session ID acquired: " + jsessionId);
                    break;
                }
            }
        }
    }

    private void createChatRoom() {
        // 채팅방 생성 (헤더에 쿠키 포함)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (jsessionId != null) {
            headers.add("Cookie", jsessionId);
        }
        HttpEntity<CreateChatRoomRequest> request =
                new HttpEntity<>(new CreateChatRoomRequest("Test Room", 10), headers);
        
        // ChatRoomController.createRoom은 @RequestBody String name을 받음
        ResponseEntity<ApiResponse<ChatRoomDto>> response = restTemplate.exchange(
                "/chat/rooms",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<ApiResponse<ChatRoomDto>>() {}
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        roomId = response.getBody().data().roomId();
        System.out.println("Created Room ID: " + roomId);
    }

    @Test
    @DisplayName("웹소켓 연결 후 메시지 전송 및 수신 테스트")
    public void verifyMessageTransmission() throws Exception {
        // given
        String wsUrl = "ws://localhost:" + port + "/ws-stomp";
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        if (jsessionId != null) {
            headers.add("Cookie", jsessionId); // 세션 쿠키 추가
        } else {
            System.out.println("Warning: No Session ID found.");
        }

        StompSessionHandlerAdapter sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                System.out.println("Connected to WebSocket");
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                System.out.println("Stomp Exception: " + exception.getMessage());
                exception.printStackTrace();
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                System.out.println("Transport Error: " + exception.getMessage());
            }
        };

        // when: 웹소켓 연결
        StompSession session = stompClient.connectAsync(wsUrl, headers, sessionHandler).get(5, TimeUnit.SECONDS);

        // 구독 (Subscribe)
        BlockingQueue<ChatMessageDto> blockingQueue = new LinkedBlockingQueue<>();
        String subscribeUrl = "/sub/" + roomId;
        session.subscribe(subscribeUrl, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                System.out.println("Message Received: " + payload);
                blockingQueue.offer((ChatMessageDto) payload);
            }
        });

        // 구독 시점의 ENTER 메시지를 소비해버릴 수 있으므로 잠시 대기하거나 ENTER 메시지를 무시하도록 필터링 필요할 수 있음.
        // SessionEventHandler에서 구독 시 ENTER 메시지를 보내므로, 큐에 ENTER 메시지가 먼저 들어올 것임.
        // 여기서는 TALK 메시지를 기다리므로 poll을 두 번 하거나 내용을 확인해야 함.

        // 발행 (Publish)
        ChatMessageDto messageToSend = new ChatMessageDto(
                null,
                ChatMessageType.TALK,
                null, // 서버에서 채워짐 (로그인 유저 닉네임)
                "Hello WebSocket",
                null,
                null,
                null
        );
        String publishUrl = "/pub/" + roomId;
        session.send(publishUrl, messageToSend);
        System.out.println("Message Sent");

        // then: 메시지 수신 대기 (ENTER 메시지가 먼저 올 수 있음)
        ChatMessageDto firstMessage = blockingQueue.poll(5, TimeUnit.SECONDS);
        assertThat(firstMessage).isNotNull();
        System.out.println("First Message: " + firstMessage.content());

        ChatMessageDto targetMessage = firstMessage;
        if (firstMessage.type() == ChatMessageType.ENTER) {
            // ENTER 메시지라면 다음 메시지를 기다림 (TALK)
            targetMessage = blockingQueue.poll(5, TimeUnit.SECONDS);
            assertThat(targetMessage).isNotNull();
            System.out.println("Second Message: " + targetMessage.content());
        }

        assertThat(targetMessage.content()).isEqualTo("Hello WebSocket");
        assertThat(targetMessage.sender()).isEqualTo("wsNick"); // 로그인한 유저 닉네임 확인
    }
}
