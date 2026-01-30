package projects.caregiver_backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import projects.caregiver_backend.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByReference(String reference);

    Optional<Payment> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);
}
