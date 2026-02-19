package com.xyz.entertainment.ticketing.theatre.repository;

import com.xyz.entertainment.ticketing.theatre.domain.Theatre;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    List<Theatre> findByTenantId(Long tenantId);
}

