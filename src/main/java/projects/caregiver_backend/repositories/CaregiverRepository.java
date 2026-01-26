package projects.caregiver_backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import projects.caregiver_backend.model.Caregiver;
import projects.caregiver_backend.model.OnboardingStatus;
import projects.caregiver_backend.model.User;

import java.util.List;
import java.util.UUID;

public interface CaregiverRepository extends JpaRepository<Caregiver, UUID> {

    boolean existsByUser(User user);

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

}

