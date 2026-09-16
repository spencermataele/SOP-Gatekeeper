package com.woven.app.web.controller.test;

import com.woven.app.domain.Role;
import com.woven.app.domain.User;
import com.woven.app.dto.authorization.AuthRequest;
import com.woven.app.repository.UserRepository;
import com.woven.app.service.userAuth.JwtService;
import com.woven.app.web.controller.AuthController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock AuthenticationManager authenticationManager;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthController controller;

    @Test
    void validCredentialsReturnTokenAndIdentity() {
        User user = new User();
        user.setId(1);
        user.setUsername("testUser");
        user.setFullName("Test User");
        user.setRole(Role.USER);
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("testUser")).thenReturn("test-token");

        var response = controller.login(new AuthRequest("testUser", "test-only-password"));
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("test-token", response.getBody().token());
        assertEquals("testUser", response.getBody().username());
        assertEquals(Role.USER, response.getBody().roles());
        verify(authenticationManager).authenticate(argThat(authentication ->
                "testUser".equals(authentication.getPrincipal())
                        && "test-only-password".equals(authentication.getCredentials())));
    }

    @Test
    void invalidCredentialsNeverLoadAUserOrIssueAToken() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        assertThrows(BadCredentialsException.class,
                () -> controller.login(new AuthRequest("testUser", "wrong-password")));
        verifyNoInteractions(userRepository, jwtService);
    }
}
