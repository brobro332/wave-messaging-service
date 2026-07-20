package xyz.datt.wave.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지 내보내기 (Sub): /sub 경로로 구독 중인 유저들에게 전달
        registry.enableSimpleBroker("/sub");

        // 메시지 들어오기 (Pub): /pub 경로로 들어오는 메시지를 핸들러로 라우팅
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS 지원 엔드포인트
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Raw WebSocket 지원 엔드포인트 (SockJS 미사용 클라이언트용)
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*");
    }
}
