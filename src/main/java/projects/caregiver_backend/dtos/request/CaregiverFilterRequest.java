package projects.caregiver_backend.dtos.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CaregiverFilterRequest(
        String city,
        String neighborhood,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Double minRating,
        LocalDate availableDate
) {}
