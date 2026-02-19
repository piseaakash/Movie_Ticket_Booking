package com.xyz.entertainment.ticketing.payment.service;

import com.xyz.entertainment.ticketing.payment.api.model.ConfirmPaymentRequest;
import com.xyz.entertainment.ticketing.payment.api.model.CreatePaymentRequest;
import com.xyz.entertainment.ticketing.payment.api.model.PaymentResponse;
import com.xyz.entertainment.ticketing.payment.domain.Payment;
import com.xyz.entertainment.ticketing.payment.repository.PaymentRepository;
import com.xyz.entertainment.ticketing.payment.security.CustomerContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Long userId = requireUserId();

        BigDecimal amount = request.getAmount() != null
                ? BigDecimal.valueOf(request.getAmount())
                : BigDecimal.ZERO;

        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .userId(userId)
                .amount(amount)
                .currency(request.getCurrency())
                .status(Payment.Status.PENDING)
                .createdAt(Instant.now())
                .build();

        Payment saved = paymentRepository.save(payment);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPaymentsForCurrentUser() {
        Long userId = requireUserId();
        return paymentRepository.findByUserId(userId).stream()
                .map(PaymentService::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long id) {
        Long userId = requireUserId();

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (!payment.getUserId().equals(userId)) {
            throw new ForbiddenPaymentAccessException("Payment does not belong to current user");
        }

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse confirmPayment(Long id, ConfirmPaymentRequest request) {
        Long userId = requireUserId();
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        if (!payment.getUserId().equals(userId)) {
            throw new ForbiddenPaymentAccessException("Payment does not belong to current user");
        }
        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new IllegalStateException("Payment is not in PENDING state");
        }
        String referenceId = request != null && request.getReferenceId() != null && request.getReferenceId().isPresent()
                ? request.getReferenceId().get()
                : null;
        Payment updated = Payment.builder()
                .id(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(Payment.Status.CONFIRMED)
                .referenceId(referenceId)
                .createdAt(payment.getCreatedAt())
                .build();
        Payment saved = paymentRepository.save(updated);
        return toResponse(saved);
    }

    @Transactional
    public PaymentResponse failPayment(Long id) {
        Long userId = requireUserId();
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        if (!payment.getUserId().equals(userId)) {
            throw new ForbiddenPaymentAccessException("Payment does not belong to current user");
        }
        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new IllegalStateException("Payment is not in PENDING state");
        }
        Payment updated = Payment.builder()
                .id(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(Payment.Status.FAILED)
                .referenceId(payment.getReferenceId())
                .createdAt(payment.getCreatedAt())
                .build();
        Payment saved = paymentRepository.save(updated);
        return toResponse(saved);
    }

    private Long requireUserId() {
        Long userId = CustomerContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return userId;
    }

    private static PaymentResponse toResponse(Payment payment) {
        PaymentResponse resp = new PaymentResponse()
                .id(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .amount(payment.getAmount().doubleValue())
                .currency(payment.getCurrency())
                .status(PaymentResponse.StatusEnum.valueOf(payment.getStatus().name()))
                .createdAt(payment.getCreatedAt().atOffset(ZoneOffset.UTC));
        if (payment.getReferenceId() != null) {
            resp.setReferenceId(org.openapitools.jackson.nullable.JsonNullable.of(payment.getReferenceId()));
        }
        return resp;
    }
}
