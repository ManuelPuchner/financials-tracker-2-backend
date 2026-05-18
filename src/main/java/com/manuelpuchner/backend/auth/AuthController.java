package com.manuelpuchner.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final String username;
    private final String password;

    public AuthController(
            JwtUtil jwtUtil,
            @Value("${app.auth.username}") String username,
            @Value("${app.auth.password}") String password) {
        this.jwtUtil = jwtUtil;
        this.username = username;
        this.password = password;
    }

    record LoginRequest(String username, String password) {}
    record LoginResponse(String token) {}

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        if (!username.equals(req.username()) || !password.equals(req.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return new LoginResponse(jwtUtil.generateToken(req.username()));
    }
}
