package com.example.security.service;

import com.example.security.common.LocalizedException;
import com.example.security.domain.Role;
import com.example.security.domain.UserAccount;
import com.example.security.dto.CreateUserRequest;
import com.example.security.dto.UserDto;
import com.example.security.mapper.UserMapper;
import com.example.security.common.TenantHelper;
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
    public UserDto createUser(CreateUserRequest req){
        String tenant = TenantHelper.requireTenant();

        if (repo.existsByUsernameIgnoreCaseAndTenantId(req.username(), tenant)) {
            throw new LocalizedException(org.springframework.http.HttpStatus.BAD_REQUEST, "USERNAME_ALREADY_EXISTS", req.username());
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

    UserAccount saved = repo.save(u);
    // map to DTO while transaction/session is open so lazy collections can be initialized
    return UserMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public java.util.List<com.example.security.dto.UserDto> listUsers(){
        var users = repo.findAllByTenantId(TenantHelper.requireTenant());
        // map to DTOs while session is open to avoid LazyInitializationException
        return users.stream().map(com.example.security.mapper.UserMapper::toDto).toList();
    }

    // tenant checking delegated to TenantHelper
}
