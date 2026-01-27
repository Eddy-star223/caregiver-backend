package projects.caregiver_backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import projects.caregiver_backend.model.CaregiverAvailability;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AvailabilityRepository
        extends JpaRepository<CaregiverAvailability, UUID> {

    @Query("""
        SELECT DISTINCT a.caregiver.id
        FROM CaregiverAvailability a
        WHERE a.date = :date
    """)
    List<UUID> findAvailableCaregiverIds(LocalDate date);
}
