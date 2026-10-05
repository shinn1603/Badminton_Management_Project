package vn.yain.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Pure WebSocket and SockJS fallback endpoint
        registry.addEndpoint("/ws-court")
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws-court")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Clients subscribe to /topic/... to receive broadcasts
        registry.enableSimpleBroker("/topic", "/queue");
        // Messages sent to /app/... are routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
    }
}
