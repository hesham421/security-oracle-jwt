package com.example.security.domain;

import com.example.security.multitenancy.TenantEntityListener;
import com.example.security.multitenancy.TenantScoped;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PERMISSIONS",
       uniqueConstraints = {@UniqueConstraint(name="UK_PERMS_TENANT_NAME", columnNames={"TENANT_ID","NAME"})},
       indexes = {@Index(name="IDX_PERMS_TENANT", columnList="TENANT_ID")})
@EntityListeners(TenantEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Permission implements TenantScoped {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="TENANT_ID", length=64, nullable=false)
    private String tenantId;

    @Column(nullable=false, length=150)
    private String name; // PERM_*
}
