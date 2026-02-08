package projects.caregiver_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * User entity representing all users in the system
 * Users can have different roles: USER, CAREGIVER, or ADMIN
 * 
 * FIXED: Changed id type from String to UUID
 */
@Entity
@Table(name = "app_users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;  // Was String, now UUID

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
