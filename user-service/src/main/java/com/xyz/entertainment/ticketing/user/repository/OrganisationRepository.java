package com.xyz.entertainment.ticketing.user.repository;

import com.xyz.entertainment.ticketing.user.domain.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganisationRepository extends JpaRepository<Organisation, Long> {
}
