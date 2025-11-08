package com.example.security.domain;

import com.example.security.multitenancy.TenantContext;
import com.example.security.multitenancy.TenantEntityListener;
import com.example.security.multitenancy.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

// domain/RefreshToken.java
@Entity
@Table(name = "REFRESH_TOKENS", schema = "HR")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="JTI", nullable=false, unique=true, length=64)
    private String jti;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="USER_ID", nullable=false)
    private UserAccount user;

    @Column(name="TENANT_ID", nullable=false, length=64)
    private String tenantId;

    @CreationTimestamp
    @Column(name="CREATED_AT", nullable=false, updatable=false)
    private Instant createdAt;

    @Column(name="EXPIRES_AT", nullable=false)
    private Instant expiresAt;

    @Builder.Default
    @Column(name="REVOKED", nullable=false)
    private boolean revoked = false;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.tenantId == null)  this.tenantId  = TenantContext.getTenantId();
    }
}
