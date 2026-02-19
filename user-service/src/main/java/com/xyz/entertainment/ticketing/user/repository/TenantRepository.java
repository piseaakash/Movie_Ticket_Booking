package com.xyz.entertainment.ticketing.user.repository;

import com.xyz.entertainment.ticketing.user.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
}
