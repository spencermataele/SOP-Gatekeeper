package com.woven.app.web.controller.test;

import com.woven.app.domain.Role;
import com.woven.app.domain.User;
import com.woven.app.dto.authorization.AuthResponse;
import com.woven.app.repository.UserRepository;
import com.woven.app.service.userAuth.JwtService;
import com.woven.app.dto.authorization.AuthRequest;
import com.woven.app.web.controller.AuthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private User validUser;

    @BeforeEach
    void setup() {

        validUser = new User();

        validUser.setId(1);
        validUser.setUsername("testUser");
        validUser.setEmail("testuser@test.com");
        validUser.setPassword("testPassword");
        validUser.setFullName("Test User");
        validUser.setRole(Role.USER);
    }

    /***Pass - User logs in with correct creds and receives token ***/
    @Test
    void login_withValidCreds_returnToken() {
        System.out.println("\n+++ VALIDATE WITH CORRECT CREDENTIALS +++");

        AuthRequest authRequest = new AuthRequest("testUser", "testPassword");

        System.out.println("Test username = " + authRequest.username());
        System.out.println("Test password = " + authRequest.password());

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(validUser));

        when(jwtService.generateToken("testUser")).thenReturn("validToken");

        ResponseEntity<AuthResponse> authResponse = authController.login(authRequest);

        AuthResponse responseBody = authResponse.getBody();

        System.out.println("Generated Token = " + responseBody.token());
        System.out.println("Returned Username = " + responseBody.username());

        assertNotNull(responseBody);

        assertEquals("validToken", responseBody.token());

        assertEquals("testUser", responseBody.username());

        verify(authenticationManager).authenticate(any());

        verify(userRepository).findByUsername("testUser");

        verify(jwtService).generateToken("testUser");

        System.out.println("PASS - Token generated and returned correctly");
    }
    /*** Fail - User logs in with incorrect password and cannot authenticate ***/
    @Test
    void login_withInvalidCreds_returnAsFail() {
        System.out.println("\n+++ VALIDATE WITH INCORRECT CREDENTIALS +++");

        AuthRequest authRequest = new AuthRequest("testUser", "incorrectPassword");

        System.out.println("Test username = " + authRequest.username());
        System.out.println("Test password = " + authRequest.password());

        when(authenticationManager.authenticate(any())).thenThrow(new RuntimeException("Invalid credentials"));

        try {
            authController.login(authRequest);

            System.out.println("FAIL - Authentication failed");

            fail("Authentication should have failed");

        } catch (Exception exception) {
            System.out.println("Authentication error: " + exception.getMessage());

            System.out.println("PASS - Invalid credentials did not authenticate");
        }

    }
}
