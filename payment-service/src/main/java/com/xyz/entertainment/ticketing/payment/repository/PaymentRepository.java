package com.xyz.entertainment.ticketing.payment.repository;

import com.xyz.entertainment.ticketing.payment.domain.Payment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserId(Long userId);
}
