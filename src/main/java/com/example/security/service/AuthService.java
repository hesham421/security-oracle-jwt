package com.example.security.service;

import com.example.security.common.LocalizedException;
import com.example.security.domain.RefreshToken;
import com.example.security.domain.UserAccount;
import com.example.security.multitenancy.TenantContext;
import com.example.security.common.CookieUtils;
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
    @Value("${security.jwt.cookie.samesite:Lax}")
    private String cookieSameSite;

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
            .orElseThrow(() -> new LocalizedException(org.springframework.http.HttpStatus.BAD_REQUEST, "USER_NOT_FOUND"));

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
            .orElseThrow(() -> new LocalizedException(org.springframework.http.HttpStatus.BAD_REQUEST, "REFRESH_REVOKED"));
            if (db.isRevoked() || db.getExpiresAt().isBefore(Instant.now())) {
                throw new LocalizedException(org.springframework.http.HttpStatus.BAD_REQUEST, "REFRESH_EXPIRED_OR_REVOKED");
            }

            // أوقف القديم ودوّر الجديد
            db.setRevoked(true);
            refreshTokenRepo.save(db);

        var userEntity = userAccountRepo
            .findByUsernameIgnoreCaseAndTenantId(username, tenant)
            .orElseThrow(() -> new LocalizedException(org.springframework.http.HttpStatus.BAD_REQUEST, "USER_NOT_FOUND"));

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
        // Use header-based Set-Cookie to include SameSite attribute (Cookie API lacks SameSite)
    CookieUtils.addRefreshCookieToResponse(response, token, (int) refreshExpSeconds, cookieDomain, cookiePath, cookieSecure, com.example.security.common.SameSite.fromString(cookieSameSite));
    }

    private void clearRefreshCookie(HttpServletResponse response) {
    CookieUtils.clearRefreshCookieInResponse(response, cookieDomain, cookiePath, cookieSecure, com.example.security.common.SameSite.fromString(cookieSameSite));
    }

    private String readRefreshCookie(HttpServletRequest request) {
    if (request.getCookies() == null) throw new LocalizedException(org.springframework.http.HttpStatus.BAD_REQUEST, "NO_REFRESH_COOKIE");
        for (Cookie c : request.getCookies()) {
            if ("refresh_token".equals(c.getName())) return c.getValue();
        }
    throw new LocalizedException(org.springframework.http.HttpStatus.BAD_REQUEST, "NO_REFRESH_COOKIE");
    }
}
