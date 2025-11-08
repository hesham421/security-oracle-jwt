package com.example.security.repo;

import com.example.security.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsernameIgnoreCaseAndTenantId(String username, String tenantId);

    boolean existsByUsernameIgnoreCaseAndTenantId(String username, String tenantId);

    List<UserAccount> findAllByTenantId(String tenantId);

    long deleteByIdAndTenantId(Long id, String tenantId);
}
