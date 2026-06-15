package LNASC.REGINOTES.Security;

import LNASC.REGINOTES.Exceptions.NotFoundException;
import LNASC.REGINOTES.Models.RefreshToken;
import LNASC.REGINOTES.Repositories.RefreshTokenRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.weaver.patterns.IToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private HandlerExceptionResolver handlerExceptionResolver;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;



    @Override
    protected boolean shouldNotFilter(HttpServletRequest request){
        String path = request.getRequestURI();
        return path.equals("/auth/login") ||
                path.equals("/auth/register") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/auth/login") ||
                path.startsWith("/auth/register");
    }



    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

       try {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }

        String token = header.substring(7);

        if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtService.extractUsername(token);

        if (username != null){
            UserDetails user = userDetailsService.loadUserByUsername(username);
            if (jwtService.isValid(token,user)){
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null,user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            filterChain.doFilter(request,response);
        }
       }catch ( ExpiredJwtException ex){
           handlerExceptionResolver.resolveException(request,response,null,ex);
       }catch (JwtException ex){
           handlerExceptionResolver.resolveException(request,response,null,ex);
       }catch (NotFoundException e){
           handlerExceptionResolver.resolveException(request,response,null,e);
       }

    }
}
