package projects.caregiver_backend.dtos.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingRequest(
        UUID caregiverId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
) {}
