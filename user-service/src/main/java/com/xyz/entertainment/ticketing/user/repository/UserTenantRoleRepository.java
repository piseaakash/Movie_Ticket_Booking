package com.xyz.entertainment.ticketing.user.repository;

import com.xyz.entertainment.ticketing.user.domain.UserTenantRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTenantRoleRepository extends JpaRepository<UserTenantRole, Long> {

    List<UserTenantRole> findByUserId(Long userId);

    Optional<UserTenantRole> findByUserIdAndTenantId(Long userId, Long tenantId);
}
