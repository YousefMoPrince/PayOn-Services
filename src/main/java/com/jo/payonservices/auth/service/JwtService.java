package com.jo.payonservices.auth.service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    @Value("${app.jwt.secret:jo782005am2005yassmoham26111977yassyassmohammoham}")
    private String SECRET_STRING;
    @Autowired
    private RefreshTokenService refreshTokenService;


    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));
    }

    public Map<String, String> generateTokens(String name, String userName) {
        String accessToken = Jwts.builder()
                .setSubject(userName)
                .claim("username", name)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 15))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = UUID.randomUUID().toString();

        refreshTokenService.saveRefreshToken(refreshToken, userName);

        return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}