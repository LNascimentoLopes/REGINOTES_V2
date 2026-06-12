package LNASC.REGINOTES.Security;

import LNASC.REGINOTES.Models.RefreshToken;
import LNASC.REGINOTES.Models.User;
import LNASC.REGINOTES.Repositories.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.antlr.v4.runtime.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;


@Service
public class JwtService {

    @Value("${api.security.token.secret}")
    private String SECRET;
    @Value("${api.security.token.expiration:7200000}")
    private String EXPIRATION;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    public String extractUsername(String token){
       return getBody(token).getSubject();
    }
    public Date extractExpiration(String token){
        return getBody(token).getExpiration();
    }

    public String generateToken(UserDetails user){
       return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+Long.parseLong(EXPIRATION)))
                .signWith(getSigningKey())
                .compact();

    }
    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }
    public boolean isValid(String token,UserDetails user){
        return !isTokenExpired(token)
                && extractUsername(token)
                .equals(user.getUsername());
    }
    private Boolean isTokenExpired(String token){
        Date exp = extractExpiration(token);
        return exp.before(Date.from(Instant.now()));
    }

    private Claims getBody(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build().parseSignedClaims(token)
                .getPayload();
    }

    @Transactional
    public String generateRefreshToken(User user,String token){
        RefreshToken rToken = new RefreshToken();

        rToken.setTokenOwner(user);
        rToken.setToken(token);
        rToken.setExpiresAt(Instant.now().plusSeconds(60*60*24*7));

        refreshTokenRepository.save(rToken);

        return rToken.getId().toString();

    }

}
