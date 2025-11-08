package com.example.security.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessExpSeconds;
    private final long refreshExpSeconds;
    private final String tenantClaim;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-exp-seconds}") long accessExpSeconds,
            @Value("${security.jwt.refresh-exp-seconds}") long refreshExpSeconds,
            @Value("${security.jwt.tenant-claim:tenant}") String tenantClaim
    ){
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpSeconds = accessExpSeconds;
        this.refreshExpSeconds = refreshExpSeconds;
        this.tenantClaim = tenantClaim;
    }

    public String generateAccess(String username, String tenantId, List<String> authorities){
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .addClaims(Map.of(tenantClaim, tenantId, "authorities", authorities))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(accessExpSeconds)))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefresh(String username, String tenantId, String jti){
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .setId(jti)
                .claim(tenantClaim, tenantId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(refreshExpSeconds)))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token){
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
    }

    public String extractUsername(String token){ return parse(token).getBody().getSubject(); }
    public String extractTenant(String token){
        Object v = parse(token).getBody().get(tenantClaim);
        return v == null ? null : v.toString();
    }
    public String extractJti(String token){ return parse(token).getBody().getId(); }
}
