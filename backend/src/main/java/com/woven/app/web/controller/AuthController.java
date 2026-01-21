package com.woven.app.web.controller;

import com.woven.app.domain.Role;
import com.woven.app.domain.User;
import com.woven.app.dto.authorization.AuthRequest;
import com.woven.app.dto.authorization.AuthResponse;
import com.woven.app.dto.authorization.MeResponse;
import com.woven.app.dto.authorization.RegisterRequest;
import com.woven.app.repository.UserRepository;
import com.woven.app.service.user.AppUserDetails;
import com.woven.app.service.userAuth.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // NEW USER
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.badRequest().body("Username already exists!");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(Role.USER);

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    // LOGIN - RETURNS JWT TOKEN
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        // Load the actual user from DB
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate JWT
        String token = jwtService.generateToken(user.getUsername());

        return ResponseEntity.ok(
                                new AuthResponse(
                                                token,
                                                user.getId(),
                                                user.getUsername(),
                                                user.getFullName(),
                                                user.getRole()
                                )
        );
    }

    //Current User
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal AppUserDetails appUserDetails) {
        User user = appUserDetails.getUser();
        return ResponseEntity.ok(
                new MeResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getRole()
                )
        );
    }

}
