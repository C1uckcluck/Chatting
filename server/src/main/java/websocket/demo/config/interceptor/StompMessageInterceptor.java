package websocket.demo.config.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * stomp message channel 로 보내기 전에 전처리할 수 있는 interceptor
 */

@Component
@Slf4j
public class StompMessageInterceptor implements ChannelInterceptor {

//     메세지가 채널에 보내기전에 실행됨
//     필요하다면 message 를 수정할 수 있고 null 반환시 채널로 메세지가 전달되지 않음
//     로그 찍어보면 Byte 값으로 넘어옴
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        log.info("메세지 값 {}",(Object) message.getPayload());
        return ChannelInterceptor.super.preSend(message, channel);
    }


    /**
     * MessageChaneel.send() 직후에 호출
     * sent는 메세지 전달 성공 유무
     */
    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        ChannelInterceptor.super.postSend(message, channel, sent);
    }

    /**
     *  finally 같은 역할 => 실패/성공 상관없이 호출됨
     *  하지만 presend가 null을 호출한 경우에는 호출되지않음
     */
    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        ChannelInterceptor.super.afterSendCompletion(message, channel, sent, ex);
    }

    @Override
    public boolean preReceive(MessageChannel channel) {
        return ChannelInterceptor.super.preReceive(channel);
    }

    @Override
    public Message<?> postReceive(Message<?> message, MessageChannel channel) {
        return ChannelInterceptor.super.postReceive(message, channel);
    }

    @Override
    public void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {
        ChannelInterceptor.super.afterReceiveCompletion(message, channel, ex);
    }
}
