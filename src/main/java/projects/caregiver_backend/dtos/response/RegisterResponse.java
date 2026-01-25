package projects.caregiver_backend.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {
    private String username;
    private String email;
    private String message;
}
