package projects.caregiver_backend.dtos.response;

import projects.caregiver_backend.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID caregiverId,
        String caregiverName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        BookingStatus status,
        BigDecimal price
) {}
