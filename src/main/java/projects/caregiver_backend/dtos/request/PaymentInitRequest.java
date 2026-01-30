package projects.caregiver_backend.dtos.request;

import java.util.UUID;

public record PaymentInitRequest(
        UUID bookingId
) {}
