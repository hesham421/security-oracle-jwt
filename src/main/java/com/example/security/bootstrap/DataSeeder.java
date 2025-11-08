package com.example.security.bootstrap;

import com.example.security.domain.Permission;
import com.example.security.domain.Role;
import com.example.security.domain.UserAccount;
import com.example.security.multitenancy.TenantContext;
import com.example.security.repo.PermissionRepository;
import com.example.security.repo.RoleRepository;
import com.example.security.repo.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final PermissionRepository perms;
    private final PasswordEncoder encoder;

    @Value("${app.default-tenant:default}")
    private String defaultTenant;

    @Override
    @Transactional
    public void run(String... args) {
        // اجعل الـtenant الحالي معروفًا أثناء البذر
        TenantContext.setTenantId(defaultTenant);
        try {
            // صلاحيات لكل Tenant
            Permission pView = perms.findByNameAndTenantId("PERM_USER_VIEW", defaultTenant)
                    .orElseGet(() -> perms.save(
                            Permission.builder()
                                    .tenantId(defaultTenant)
                                    .name("PERM_USER_VIEW")
                                    .build()
                    ));
            Permission pCreate = perms.findByNameAndTenantId("PERM_USER_CREATE", defaultTenant)
                    .orElseGet(() -> perms.save(
                            Permission.builder()
                                    .tenantId(defaultTenant)
                                    .name("PERM_USER_CREATE")
                                    .build()
                    ));

            // أدوار لكل Tenant
            Role roleAdmin = roles.findByNameAndTenantId("ROLE_ADMIN", defaultTenant)
                    .orElseGet(() -> {
                        Role r = Role.builder()
                                .tenantId(defaultTenant)
                                .name("ROLE_ADMIN")
                                .build();
                        r.setPermissions(Set.of(pView, pCreate));
                        return roles.save(r);
                    });

            Role roleUser = roles.findByNameAndTenantId("ROLE_USER", defaultTenant)
                    .orElseGet(() -> roles.save(
                            Role.builder()
                                    .tenantId(defaultTenant)
                                    .name("ROLE_USER")
                                    .build()
                    ));

            // مستخدمو dev لكل Tenant
            if (users.findByUsernameIgnoreCaseAndTenantId("admin", defaultTenant).isEmpty()) {
                UserAccount admin = UserAccount.builder()
                        .tenantId(defaultTenant)
                        .username("admin")
                        .password(encoder.encode("Admin@12345"))
                        .enabled(true)
                        .build();
                admin.setRoles(Set.of(roleAdmin));
                users.save(admin);
            }

            if (users.findByUsernameIgnoreCaseAndTenantId("user", defaultTenant).isEmpty()) {
                UserAccount user = UserAccount.builder()
                        .tenantId(defaultTenant)
                        .username("user")
                        .password(encoder.encode("User@12345"))
                        .enabled(true)
                        .build();
                user.setRoles(Set.of(roleUser));
                users.save(user);
            }
        } finally {
            TenantContext.clear();
        }
    }
}
