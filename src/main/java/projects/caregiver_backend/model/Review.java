package projects.caregiver_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "caregiver_id"})
        }
)
public class Review {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Caregiver caregiver;

    @Column(nullable = false)
    private int rating; // 1–5

    @Column(length = 500)
    private String comment;

    private LocalDateTime createdAt;
}
