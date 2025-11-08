package com.example.security.service;

import com.example.security.domain.Role;
import com.example.security.domain.UserAccount;
import com.example.security.dto.CreateUserRequest;
import com.example.security.multitenancy.TenantContext;
import com.example.security.repo.RoleRepository;
import com.example.security.repo.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserAccountRepository repo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;

    @Transactional
    public UserAccount createUser(CreateUserRequest req){
        String tenant = requireTenant();

        if (repo.existsByUsernameIgnoreCaseAndTenantId(req.username(), tenant)) {
            throw new IllegalArgumentException("Username already exists in tenant");
        }

        UserAccount u = UserAccount.builder()
                .tenantId(tenant)
                .username(req.username())
                .password(encoder.encode(req.password()))
                .enabled(true)
                .build();

        // ربط الدور الافتراضي داخل نفس الـtenant
        roleRepo.findByNameAndTenantId("ROLE_USER", tenant)
                .ifPresent(r -> u.setRoles(Set.of(r)));

        return repo.save(u);
    }

    @Transactional(readOnly = true)
    public List<UserAccount> listUsers(){
        return repo.findAllByTenantId(requireTenant());
    }

    private String requireTenant() {
        String t = TenantContext.getTenantId();
        if (t == null || t.isBlank()) {
            throw new IllegalStateException("Missing tenant in context");
        }
        return t;
    }
}
