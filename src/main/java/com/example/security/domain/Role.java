package com.example.security.domain;

import com.example.security.multitenancy.TenantEntityListener;
import com.example.security.multitenancy.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ROLES",
       uniqueConstraints = {@UniqueConstraint(name="UK_ROLES_TENANT_NAME", columnNames={"TENANT_ID","NAME"})},
       indexes = {@Index(name="IDX_ROLES_TENANT", columnList="TENANT_ID")})
@EntityListeners(TenantEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role implements TenantScoped {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="TENANT_ID", length=64, nullable=false)
    private String tenantId;

    @Column(nullable=false, length=60)
    private String name; // ROLE_*

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "ROLE_PERMISSIONS",
      joinColumns = @JoinColumn(name="ROLE_ID"),
      inverseJoinColumns = @JoinColumn(name="PERM_ID"))
    private Set<Permission> permissions = new HashSet<>();
}
