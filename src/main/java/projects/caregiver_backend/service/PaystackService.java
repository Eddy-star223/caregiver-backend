package projects.caregiver_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import projects.caregiver_backend.dtos.response.PaymentInitResponse;
import projects.caregiver_backend.model.*;
import projects.caregiver_backend.repositories.BookingRepository;
import projects.caregiver_backend.repositories.PaymentRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaystackService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${paystack.secret.key}")
    private String paystackSecretKey;

    @Transactional
    public PaymentInitResponse initializePayment(
            UUID bookingId,
            String customerEmail
    ) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking cannot be paid for");
        }

        String reference = UUID.randomUUID().toString().replace("-", "");

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setReference(reference);
        payment.setAmount(booking.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        // ---- Call Paystack ----
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paystackSecretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("email", customerEmail);
        body.put("amount", booking.getTotalAmount()
                .multiply(BigDecimal.valueOf(100)).intValue());
        body.put("reference", reference);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.paystack.co/transaction/initialize",
                HttpMethod.POST,
                request,
                String.class
        );

        try {
            JsonNode data = objectMapper.readTree(response.getBody())
                    .get("data");

            String authorizationUrl = data.get("authorization_url").asText();

            payment.setAuthorizationUrl(authorizationUrl);
            paymentRepository.save(payment);

            return new PaymentInitResponse(
                    authorizationUrl,
                    reference
            );

        } catch (Exception e) {
            throw new RuntimeException("Paystack initialization failed");
        }
    }

    @Transactional
    public void handleWebhook(
            String payload,
            String signature
    ) {

        if (!verifySignature(payload, signature)) {
            throw new SecurityException("Invalid Paystack signature");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new RuntimeException("Invalid webhook payload");
        }

        String event = root.get("event").asText();

        if (!"charge.success".equals(event)) return;

        JsonNode data = root.get("data");
        String reference = data.get("reference").asText();

        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) return;

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setGatewayResponse(data.toString());

        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.PAID);

        paymentRepository.save(payment);
        bookingRepository.save(booking);
    }

    private boolean verifySignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(
                    paystackSecretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            ));

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedHash = bytesToHex(hash);

            return computedHash.equalsIgnoreCase(signature);

        } catch (Exception e) {
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
