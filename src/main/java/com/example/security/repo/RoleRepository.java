package com.example.security.repo;

import com.example.security.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByNameAndTenantId(String name, String tenantId);

    Optional<Role> findByIdAndTenantId(Long id, String tenantId);

    List<Role> findAllByTenantId(String tenantId);

    long deleteByIdAndTenantId(Long id, String tenantId);
}
