package com.example.security.domain;

import com.example.security.multitenancy.TenantEntityListener;
import com.example.security.multitenancy.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "USERS",
       uniqueConstraints = {@UniqueConstraint(name="UK_USERS_TENANT_USERNAME", columnNames={"TENANT_ID","USERNAME"})},
       indexes = {@Index(name="IDX_USERS_TENANT", columnList="TENANT_ID")})
@EntityListeners(TenantEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAccount implements TenantScoped {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="TENANT_ID", length=64, nullable=false)
    private String tenantId;

    @Column(nullable=false, length=80)
    private String username;

    @Column(nullable=false, length=200)
    private String password;

    @Column(nullable=false)
    private boolean enabled = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "USER_ROLES",
      joinColumns = @JoinColumn(name="USER_ID"),
      inverseJoinColumns = @JoinColumn(name="ROLE_ID"))
    private Set<Role> roles = new HashSet<>();

    @Column(name="CREATED_AT", updatable=false)
    private Instant createdAt = Instant.now();
}
