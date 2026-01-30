package projects.caregiver_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import projects.caregiver_backend.dtos.request.PaymentInitRequest;
import projects.caregiver_backend.dtos.response.PaymentInitResponse;
import projects.caregiver_backend.service.PaystackService;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    @Autowired
    private final PaystackService paystackService;

    @PostMapping("/init")
    public PaymentInitResponse initializePayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PaymentInitRequest request
    ) {
        return paystackService.initializePayment(
                request.bookingId(),
                userDetails.getUsername() // customer email
        );
    }
}
