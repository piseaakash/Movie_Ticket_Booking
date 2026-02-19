package com.xyz.entertainment.ticketing.theatre.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Theatre onboarded by a partner tenant. The tenant itself lives in user-service;
 * here we only store its id (tenantId) and rely on the JWT claim for scoping.
 */
@Entity
@Table(name = "theatres")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Theatre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void applyUpdate(String name, String addressLine1, String city, String state, String postalCode) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (addressLine1 != null) {
            this.addressLine1 = addressLine1.trim();
        }
        if (city != null) {
            this.city = city.trim();
        }
        if (state != null) {
            this.state = state.trim();
        }
        if (postalCode != null) {
            this.postalCode = postalCode.trim();
        }
        this.updatedAt = Instant.now();
    }
}

