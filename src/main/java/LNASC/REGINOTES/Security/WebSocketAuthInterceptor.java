package LNASC.REGINOTES.Security;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import java.nio.channels.Channel;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserDetailsService userDetails;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())){
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")){
                String token = header.substring(7);

                String username = jwtService.extractUsername(token);
                if (username != null){
                    UserDetails user = userDetails.loadUserByUsername(username);
                    if (jwtService.isValid(token,user)){
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(user, null,user.getAuthorities());

                        accessor.setUser(auth);
                    }
                }
            }

        }
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }
}
