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

import java.time.LocalDateTime;

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

        Booking saved = bookingRepository.save(booking);

        return new BookingResponse(
                saved.getId(),
                caregiver.getId(),
                caregiver.getFullName(),
                saved.getDate(),
                saved.getStartTime(),
                saved.getEndTime(),
                saved.getStatus(),
                saved.getPrice()
        );
    }
}
