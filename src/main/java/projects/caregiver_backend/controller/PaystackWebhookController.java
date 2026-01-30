package projects.caregiver_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import projects.caregiver_backend.service.PaystackService;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class PaystackWebhookController {

    private final PaystackService paystackService;

    @PostMapping("/paystack")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("x-paystack-signature") String signature
    ) {
        paystackService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}

