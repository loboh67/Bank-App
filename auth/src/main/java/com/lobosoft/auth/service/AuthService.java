package com.lobosoft.auth.service;

import com.lobosoft.auth.domain.User;
import com.lobosoft.auth.domain.UserRepository;
import com.lobosoft.auth.dto.AuthResponse;
import com.lobosoft.auth.dto.LoginRequest;
import com.lobosoft.auth.dto.RegisterRequest;
import com.lobosoft.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest req) {
        userRepository.findByEmail(req.getEmail())
                .ifPresent(u -> { throw new EmailAlreadyRegisteredException(req.getEmail()); });

        String hash = BCrypt.hashpw(req.getPassword(), BCrypt.gensalt());

        User user = new User();
        user.setEmail(req.getEmail().toLowerCase(Locale.ROOT));
        user.setPasswordHash(hash);

        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail());

        log.info("User '{}' registered", user.getEmail());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        var candidate = userRepository.findByEmail(req.getEmail().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("invalid_credentials"));

        if (!BCrypt.checkpw(req.getPassword(), candidate.getPasswordHash())) {
            throw new RuntimeException("invalid_credentials");
        }

        String token = jwtService.generateToken(candidate.getId(), candidate.getEmail());

        log.info("User '{}' logged in", candidate.getEmail());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .build();
    }

}
