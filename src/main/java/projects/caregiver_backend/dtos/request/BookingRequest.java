package projects.caregiver_backend.dtos.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Request DTO for creating a booking
 * 
 * ADDED: Validation annotations for date and time
 */
public record BookingRequest(
        @NotNull(message = "Caregiver ID is required")
        UUID caregiverId,
        
        @NotNull(message = "Date is required")
        @FutureOrPresent(message = "Booking date must be today or in the future")
        LocalDate date,
        
        @NotNull(message = "Start time is required")
        LocalTime startTime,
        
        @NotNull(message = "End time is required")
        LocalTime endTime
) {
    /**
     * NEW: Compact constructor for additional validation
     * Validates that end time is after start time
     */
    public BookingRequest {
        if (startTime != null && endTime != null) {
            if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
                throw new IllegalArgumentException(
                        "End time must be after start time"
                );
            }
        }
    }
}
