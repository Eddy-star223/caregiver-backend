package projects.caregiver_backend.dtos.response;

import java.util.UUID;

public record CaregiverResponse(
        UUID id,
        String fullName,
        String city,
        String neighborhood,
        String phone,
        String bio,
        Double averageRating,
        Long reviewCount
) {}
