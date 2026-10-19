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
        // Pure WebSocket and SockJS fallback endpoint with restricted origins
        String[] allowedOrigins = new String[]{
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://*.utesport.vn",
            "https://*.onrender.com",
            "https://*.railway.app",
            "https://*.up.railway.app",
            "*"
        };
        registry.addEndpoint("/ws-court")
                .setAllowedOriginPatterns(allowedOrigins);
        registry.addEndpoint("/ws-court")
                .setAllowedOriginPatterns(allowedOrigins)
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
