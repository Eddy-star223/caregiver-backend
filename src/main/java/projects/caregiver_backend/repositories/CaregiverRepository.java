package projects.caregiver_backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import projects.caregiver_backend.model.Caregiver;
import projects.caregiver_backend.model.OnboardingStatus;
import projects.caregiver_backend.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaregiverRepository extends JpaRepository<Caregiver, UUID> {

    boolean existsByUser(User user);

    Optional<Caregiver> findByUser(User user);

    Optional<Caregiver> findByIdAndUser(UUID id, User user);


    List<Caregiver> findByCityAndVerifiedTrue(String city);

    List<Caregiver> findByCityAndNeighborhoodAndVerifiedTrue(
            String city,
            String neighborhood
    );

    List<Caregiver> findByCityAndNeighborhoodAndOnboardingStatus(
            String city,
            String neighborhood,
            OnboardingStatus status
    );

    List<Caregiver> findByCityAndOnboardingStatus(
            String city,
            OnboardingStatus status
    );

    @Query("""
SELECT c FROM Caregiver c
WHERE (:city IS NULL OR c.city = :city)
AND (:neighborhood IS NULL OR c.neighborhood = :neighborhood)
AND (:minPrice IS NULL OR c.hourlyRate >= :minPrice)
AND (:maxPrice IS NULL OR c.hourlyRate <= :maxPrice)
AND c.onboardingStatus = 'APPROVED'
""")
    List<Caregiver> filterCaregivers(
            String city,
            String neighborhood,
            BigDecimal minPrice,
            BigDecimal maxPrice
    );

}

