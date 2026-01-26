package projects.caregiver_backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import projects.caregiver_backend.model.Caregiver;
import projects.caregiver_backend.model.User;

import java.util.List;

public interface CaregiverRepository extends JpaRepository<Caregiver, String> {

    boolean existsByUser(User user);

    List<Caregiver> findByCityAndVerifiedTrue(String city);

    List<Caregiver> findByCityAndNeighborhoodAndVerifiedTrue(
            String city,
            String neighborhood
    );
}

