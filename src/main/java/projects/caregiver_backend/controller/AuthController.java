package projects.caregiver_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import projects.caregiver_backend.dtos.request.LoginRequest;
import projects.caregiver_backend.dtos.request.RegisterRequest;
import projects.caregiver_backend.dtos.response.RegisterResponse;
import projects.caregiver_backend.model.User;
import projects.caregiver_backend.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {

        User user = authService.register(request);

        RegisterResponse response = new RegisterResponse(
                user.getUsername(),
                user.getEmail(),
                "User registered successfully"
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        authService.login(request);

        return ResponseEntity.ok("Login successful");
    }
}
