package projects.caregiver_backend.dtos.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        String reviewerUsername,
        int rating,
        String comment,
        LocalDateTime createdAt
) {}
