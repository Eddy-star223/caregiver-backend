package projects.caregiver_backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import projects.caregiver_backend.model.Booking;
import projects.caregiver_backend.model.BookingStatus;
import projects.caregiver_backend.model.Caregiver;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository
        extends JpaRepository<Booking, UUID> {

    boolean existsByCaregiverAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
            Caregiver caregiver,
            LocalDate date,
            LocalTime endTime,
            LocalTime startTime
    );

    List<Booking> findByCaregiverIdAndStatus(
            UUID caregiverId,
            BookingStatus status
    );
}
