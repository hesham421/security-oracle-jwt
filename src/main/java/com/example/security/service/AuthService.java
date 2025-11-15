package com.example.security.service;

import com.example.security.domain.RefreshToken;
import com.example.security.domain.UserAccount;
import com.example.security.multitenancy.TenantContext;
import com.example.security.repo.RefreshTokenRepository;
import com.example.security.repo.UserAccountRepository;
import com.example.security.security.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwt;
    private final RefreshTokenRepository refreshTokenRepo;
    private final UserAccountRepository userAccountRepo;

    @Value("${security.jwt.access-exp-seconds:900}")
    private long accessExpSeconds;
    @Value("${security.jwt.refresh-exp-seconds:604800}")
    private long refreshExpSeconds;

    @Value("${security.jwt.cookie.domain:localhost}")
    private String cookieDomain;
    @Value("${security.jwt.cookie.path:/}")
    private String cookiePath;
    @Value("${security.jwt.cookie.secure:false}")   // اجعلها true في الإنتاج
    private boolean cookieSecure;

    @Value("${app.default-tenant:default}")
    private String defaultTenant;
    @Value("${app.tenant.header:X-Tenant-ID}")
    private String tenantHeader;

    public record Tokens(String access, long accessExpSeconds) {}

    @Transactional
    public Tokens login(String username, String password,
                        HttpServletRequest request, HttpServletResponse response) {
        String tenant = resolveTenant(request);
        TenantContext.setTenantId(tenant);
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            UserDetails principal = (UserDetails) auth.getPrincipal();
            List<String> authorities = principal.getAuthorities()
                    .stream().map(GrantedAuthority::getAuthority).toList();

            // ابني الـ JWTs
            String jti = UUID.randomUUID().toString();
            String access = jwt.generateAccess(principal.getUsername(), tenant, authorities);
            String refresh = jwt.generateRefresh(principal.getUsername(), tenant, jti);

            // اربط التوكن بكـيان المستخدم
            UserAccount userEntity = userAccountRepo
                    .findByUsernameIgnoreCaseAndTenantId(principal.getUsername(), tenant)
                    .orElseThrow(() -> new IllegalStateException("User entity not found"));

            refreshTokenRepo.save(RefreshToken.builder()
                    .jti(jti)
                    .user(userEntity)
                    .tenantId(tenant)
                    .expiresAt(Instant.now().plusSeconds(refreshExpSeconds))
                    .revoked(false)
                    .build());

            attachRefreshCookie(response, refresh);
            return new Tokens(access, accessExpSeconds);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public Tokens refresh(HttpServletRequest request, HttpServletResponse response) {
        String raw = readRefreshCookie(request);
        var claims = jwt.parse(raw).getBody();
        String username = claims.getSubject();
        String tenant = String.valueOf(claims.get("tenant"));
        String jti = claims.getId();

        TenantContext.setTenantId(tenant);
        try {
            var db = refreshTokenRepo.findByJtiAndTenantId(jti, tenant)
                    .orElseThrow(() -> new IllegalArgumentException("Refresh revoked"));
            if (db.isRevoked() || db.getExpiresAt().isBefore(Instant.now())) {
                throw new IllegalArgumentException("Refresh expired or revoked");
            }

            // أوقف القديم ودوّر الجديد
            db.setRevoked(true);
            refreshTokenRepo.save(db);

            var userEntity = userAccountRepo
                    .findByUsernameIgnoreCaseAndTenantId(username, tenant)
                    .orElseThrow(() -> new IllegalStateException("User entity not found"));

            var user = userDetailsService.loadUserByUsername(username);
            var authorities = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

            String newJti = UUID.randomUUID().toString();
            String access = jwt.generateAccess(username, tenant, authorities);
            String newRefresh = jwt.generateRefresh(username, tenant, newJti);

            refreshTokenRepo.save(RefreshToken.builder()
                    .jti(newJti)
                    .user(userEntity)
                    .tenantId(tenant)
                    .expiresAt(Instant.now().plusSeconds(refreshExpSeconds))
                    .revoked(false)
                    .build());

            attachRefreshCookie(response, newRefresh);
            return new Tokens(access, accessExpSeconds);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // Let exceptions propagate so the global `ApiErrors` handler can map them to structured responses.
        String raw = readRefreshCookie(request);
        var claims = jwt.parse(raw).getBody();
        String tenant = String.valueOf(claims.get("tenant"));
        String jti = claims.getId();

        try {
            TenantContext.setTenantId(tenant);
            refreshTokenRepo.findByJtiAndTenantId(jti, tenant)
                    .ifPresent(rt -> { rt.setRevoked(true); refreshTokenRepo.save(rt); });
        } finally {
            TenantContext.clear();
            clearRefreshCookie(response);
        }
    }

    private String resolveTenant(HttpServletRequest request) {
        String t = request.getHeader(tenantHeader);
        return (t == null || t.isBlank()) ? defaultTenant : t;
    }

    private void attachRefreshCookie(HttpServletResponse response, String token) {
        Cookie c = new Cookie("refresh_token", token);
        c.setHttpOnly(true);
        c.setSecure(cookieSecure);
        c.setDomain(cookieDomain);
        c.setPath(cookiePath);
        c.setMaxAge((int) refreshExpSeconds);
        response.addCookie(c);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie c = new Cookie("refresh_token", "");
        c.setHttpOnly(true);
        c.setSecure(cookieSecure);
        c.setDomain(cookieDomain);
        c.setPath(cookiePath);
        c.setMaxAge(0);
        response.addCookie(c);
    }

    private String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) throw new IllegalArgumentException("No refresh cookie");
        for (Cookie c : request.getCookies()) {
            if ("refresh_token".equals(c.getName())) return c.getValue();
        }
        throw new IllegalArgumentException("No refresh cookie");
    }
}
