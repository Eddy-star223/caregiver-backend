package projects.caregiver_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projects.caregiver_backend.dtos.request.BookingRequest;
import projects.caregiver_backend.dtos.response.BookingResponse;
import projects.caregiver_backend.model.*;
import projects.caregiver_backend.repositories.BookingRepository;
import projects.caregiver_backend.repositories.CaregiverRepository;
import projects.caregiver_backend.repositories.UserRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing bookings
 * Handles booking creation, conflict detection, and caregiver approval workflow
 * 
 * FIXED: Added total amount calculation based on hourly rate
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CaregiverRepository caregiverRepository;
    private final UserRepository userRepository;

    @Transactional
    public BookingResponse createBooking(
            String username,
            BookingRequest request
    ) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Caregiver caregiver = caregiverRepository.findById(request.caregiverId())
                .orElseThrow(() -> new RuntimeException("Caregiver not found"));

        if (caregiver.getOnboardingStatus() != OnboardingStatus.VERIFIED) {
            throw new IllegalStateException("Caregiver not approved");
        }

        boolean conflict =
                bookingRepository.existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                        caregiver,
                        request.date(),
                        request.endTime(),
                        request.startTime()
                );

        if (conflict) {
            throw new IllegalStateException("Time slot already booked");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCaregiver(caregiver);
        booking.setDate(request.date());
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());

        // Calculate total amount based on hourly rate and duration
        BigDecimal totalAmount = calculateTotalAmount(
                caregiver.getHourlyRate(),
                request.startTime(),
                request.endTime()
        );
        booking.setTotalAmount(totalAmount);

        Booking saved = bookingRepository.save(booking);

        return new BookingResponse(
                saved.getId(),
                caregiver.getId(),
                caregiver.getFullName(),
                saved.getDate(),
                saved.getStartTime(),
                saved.getEndTime(),
                saved.getStatus(),
                saved.getTotalAmount()
        );
    }

    /**
     * Calculate total booking amount
     * hourlyRate × duration (in hours, including fractions)
     * 
     * @param hourlyRate The caregiver's hourly rate
     * @param startTime Booking start time
     * @param endTime Booking end time
     * @return Total amount to charge
     */
    private BigDecimal calculateTotalAmount(
            BigDecimal hourlyRate,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime
    ) {
        // Calculate duration in minutes
        long minutes = Duration.between(startTime, endTime).toMinutes();
        
        // Convert minutes to hours (as decimal)
        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        
        // Calculate total: hourlyRate × hours
        return hourlyRate.multiply(hours)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Transactional
    public BookingResponse decideBooking(
            String caregiverUsername,
            UUID bookingId,
            boolean accept
    ) {

        User caregiverUser = userRepository.findByUsername(caregiverUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Caregiver caregiver = caregiverRepository.findByUser(caregiverUser)
                .orElseThrow(() -> new RuntimeException("Caregiver profile not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCaregiver().getId().equals(caregiver.getId())) {
            throw new SecurityException("You cannot modify this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking already processed");
        }

        booking.setStatus(
                accept ? BookingStatus.ACCEPTED : BookingStatus.REJECTED
        );

        Booking saved = bookingRepository.save(booking);

        return new BookingResponse(
                saved.getId(),
                caregiver.getId(),
                caregiver.getFullName(),
                saved.getDate(),
                saved.getStartTime(),
                saved.getEndTime(),
                saved.getStatus(),
                saved.getTotalAmount()
        );
    }
}
