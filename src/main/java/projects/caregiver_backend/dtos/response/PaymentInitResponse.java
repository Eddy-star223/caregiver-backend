package projects.caregiver_backend.dtos.response;

public record PaymentInitResponse(
        String authorizationUrl,
        String reference
) {}
