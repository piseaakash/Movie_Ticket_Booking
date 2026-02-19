package com.xyz.entertainment.ticketing.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xyz.entertainment.ticketing.payment.api.model.ConfirmPaymentRequest;
import com.xyz.entertainment.ticketing.payment.api.model.CreatePaymentRequest;
import com.xyz.entertainment.ticketing.payment.api.model.PaymentResponse;
import com.xyz.entertainment.ticketing.payment.domain.Payment;
import com.xyz.entertainment.ticketing.payment.repository.PaymentRepository;
import com.xyz.entertainment.ticketing.payment.security.CustomerContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private static final Long USER_ID = 50L;

    @AfterEach
    void tearDown() {
        CustomerContext.clear();
    }

    private PaymentService service() {
        return new PaymentService(paymentRepository);
    }

    private Payment payment(Long id, Long userId, Long bookingId) {
        return Payment.builder()
                .id(id)
                .bookingId(bookingId)
                .userId(userId)
                .amount(BigDecimal.valueOf(199.99))
                .currency("INR")
                .status(Payment.Status.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("createPayment")
    class CreatePayment {

        @Test
        @DisplayName("creates payment for current user with PENDING status")
        void createPayment_ok() {
            CustomerContext.setUserId(USER_ID);
            CreatePaymentRequest req = new CreatePaymentRequest()
                    .bookingId(10L)
                    .amount(199.99)
                    .currency("INR");

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                return Payment.builder()
                        .id(1L)
                        .bookingId(p.getBookingId())
                        .userId(p.getUserId())
                        .amount(p.getAmount())
                        .currency(p.getCurrency())
                        .status(p.getStatus())
                        .createdAt(p.getCreatedAt())
                        .build();
            });

            PaymentResponse resp = service().createPayment(req);

            assertThat(resp.getId()).isEqualTo(1L);
            assertThat(resp.getBookingId()).isEqualTo(10L);
            assertThat(resp.getUserId()).isEqualTo(USER_ID);
            assertThat(resp.getAmount()).isEqualTo(199.99);
            assertThat(resp.getCurrency()).isEqualTo("INR");
            assertThat(resp.getStatus()).isEqualTo(PaymentResponse.StatusEnum.PENDING);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getStatus()).isEqualTo(Payment.Status.PENDING);
        }

        @Test
        @DisplayName("throws IllegalStateException when no user in context")
        void createPayment_noUser() {
            CreatePaymentRequest req = new CreatePaymentRequest()
                    .bookingId(10L)
                    .amount(100.0)
                    .currency("INR");

            assertThatThrownBy(() -> service().createPayment(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No authenticated user");
        }
    }

    @Nested
    @DisplayName("listPaymentsForCurrentUser")
    class ListPayments {

        @Test
        @DisplayName("returns payments for current user only")
        void listPayments_ok() {
            CustomerContext.setUserId(USER_ID);
            Payment p1 = payment(1L, USER_ID, 10L);
            Payment p2 = payment(2L, USER_ID, 11L);
            when(paymentRepository.findByUserId(USER_ID)).thenReturn(List.of(p1, p2));

            List<PaymentResponse> list = service().listPaymentsForCurrentUser();

            assertThat(list).hasSize(2);
            assertThat(list.get(0).getUserId()).isEqualTo(USER_ID);
            assertThat(list.get(1).getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("throws when no user in context")
        void listPayments_noUser() {
            assertThatThrownBy(() -> service().listPaymentsForCurrentUser())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("getPayment")
    class GetPayment {

        @Test
        @DisplayName("returns payment when owned by current user")
        void getPayment_ok() {
            CustomerContext.setUserId(USER_ID);
            Payment p = payment(1L, USER_ID, 10L);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));

            PaymentResponse resp = service().getPayment(1L);

            assertThat(resp.getId()).isEqualTo(1L);
            assertThat(resp.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("throws PaymentNotFoundException when not found")
        void getPayment_notFound() {
            CustomerContext.setUserId(USER_ID);
            when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().getPayment(99L))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessageContaining("Payment not found");
        }

        @Test
        @DisplayName("throws ForbiddenPaymentAccessException when payment belongs to another user")
        void getPayment_forbidden() {
            CustomerContext.setUserId(USER_ID);
            Payment p = payment(1L, 999L, 10L);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> service().getPayment(1L))
                    .isInstanceOf(ForbiddenPaymentAccessException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        @DisplayName("throws when no user in context")
        void getPayment_noUser() {
            assertThatThrownBy(() -> service().getPayment(1L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPayment {

        @Test
        @DisplayName("sets CONFIRMED and referenceId when PENDING")
        void confirmPayment_ok() {
            CustomerContext.setUserId(USER_ID);
            Payment p = payment(1L, USER_ID, 10L);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            ConfirmPaymentRequest req = new ConfirmPaymentRequest();
            req.setReferenceId(org.openapitools.jackson.nullable.JsonNullable.of("txn_abc"));

            PaymentResponse resp = service().confirmPayment(1L, req);

            assertThat(resp.getStatus()).isEqualTo(PaymentResponse.StatusEnum.CONFIRMED);
            assertThat(resp.getReferenceId().get()).isEqualTo("txn_abc");
        }

        @Test
        @DisplayName("throws when payment not PENDING")
        void confirmPayment_notPending() {
            CustomerContext.setUserId(USER_ID);
            Payment p = Payment.builder()
                    .id(1L)
                    .bookingId(10L)
                    .userId(USER_ID)
                    .amount(BigDecimal.TEN)
                    .currency("INR")
                    .status(Payment.Status.CONFIRMED)
                    .createdAt(Instant.now())
                    .build();
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> service().confirmPayment(1L, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not in PENDING");
        }
    }

    @Nested
    @DisplayName("failPayment")
    class FailPayment {

        @Test
        @DisplayName("sets FAILED when PENDING")
        void failPayment_ok() {
            CustomerContext.setUserId(USER_ID);
            Payment p = payment(1L, USER_ID, 10L);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse resp = service().failPayment(1L);

            assertThat(resp.getStatus()).isEqualTo(PaymentResponse.StatusEnum.FAILED);
        }
    }
}
