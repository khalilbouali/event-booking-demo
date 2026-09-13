package fr.carrefour.payment.infrastructure.adapter.in.web;

import fr.carrefour.payment.application.port.in.GetPaymentUseCase;
import fr.carrefour.payment.application.port.in.ListMyPaymentsUseCase;
import fr.carrefour.payment.application.port.in.ProcessPaymentUseCase;
import fr.carrefour.payment.infrastructure.adapter.in.web.dto.PaymentResponse;
import fr.carrefour.payment.infrastructure.adapter.in.web.dto.ProcessPaymentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final ListMyPaymentsUseCase listMyPaymentsUseCase;

    @PostMapping
    public Mono<PaymentResponse> process(
            @Valid @RequestBody ProcessPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return processPaymentUseCase.process(
                request.reservationId(),
                jwt.getSubject(),
                request.amount(),
                request.isValid()
        ).map(PaymentWebMapper::toResponse);
    }

    @GetMapping("/{paymentId}")
    public Mono<PaymentResponse> getById(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return getPaymentUseCase.getById(paymentId, jwt.getSubject())
                .map(PaymentWebMapper::toResponse);
    }

    @GetMapping
    public Flux<PaymentResponse> findMine(
            @AuthenticationPrincipal Jwt jwt
    ) {

        String customerId = jwt.getSubject();

        return listMyPaymentsUseCase
                .findByCustomerId(customerId)
                .map(PaymentWebMapper::toResponse);
    }
}
