package projects.caregiver_backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import projects.caregiver_backend.model.Review;
import projects.caregiver_backend.model.Caregiver;
import projects.caregiver_backend.model.User;
import projects.caregiver_backend.repositories.projections.CaregiverRatingView;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByUserAndCaregiver(User user, Caregiver caregiver);

    List<Review> findByCaregiver(Caregiver caregiver);

    @Query("""
        SELECT 
            r.caregiver.id AS caregiverId,
            AVG(r.rating) AS averageRating,
            COUNT(r.id) AS reviewCount
        FROM Review r
        GROUP BY r.caregiver.id
    """)
    List<CaregiverRatingView> fetchCaregiverRatings();
}
