package com.xyz.entertainment.ticketing.payment.api;

import com.xyz.entertainment.ticketing.payment.api.model.ConfirmPaymentRequest;
import com.xyz.entertainment.ticketing.payment.api.model.CreatePaymentRequest;
import com.xyz.entertainment.ticketing.payment.api.model.PaymentResponse;
import com.xyz.entertainment.ticketing.payment.service.PaymentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class PaymentsApiDelegateImpl implements PaymentsApiDelegate {

    private final PaymentService paymentService;

    @Override
    public ResponseEntity<PaymentResponse> createPayment(CreatePaymentRequest createPaymentRequest) {
        PaymentResponse resp = paymentService.createPayment(createPaymentRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @Override
    public ResponseEntity<PaymentResponse> getPayment(Long id) {
        PaymentResponse resp = paymentService.getPayment(id);
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<List<PaymentResponse>> listPayments() {
        List<PaymentResponse> list = paymentService.listPaymentsForCurrentUser();
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PaymentResponse> confirmPayment(Long id, ConfirmPaymentRequest confirmPaymentRequest) {
        PaymentResponse resp = paymentService.confirmPayment(id, confirmPaymentRequest);
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<PaymentResponse> failPayment(Long id) {
        PaymentResponse resp = paymentService.failPayment(id);
        return ResponseEntity.ok(resp);
    }
}
