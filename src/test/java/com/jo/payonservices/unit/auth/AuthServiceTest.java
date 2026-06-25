package com.jo.payonservices.unit.auth;

import com.jo.payonservices.auth.dto.AuthResponse;
import com.jo.payonservices.auth.dto.LoginRequest;
import com.jo.payonservices.auth.dto.RegisterRequest;
import com.jo.payonservices.auth.dto.UpdateRequest;
import com.jo.payonservices.auth.model.users;
import com.jo.payonservices.auth.repository.UserRepo;
import com.jo.payonservices.auth.service.AuthService;
import com.jo.payonservices.auth.service.JwtService;
import com.jo.payonservices.auth.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepo userRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks private AuthService authService;

    private users mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new users();
        mockUser.setUserId(UUID.randomUUID());
        mockUser.setUserName("yousef_jo");
        mockUser.setEmail("jo@payon.com");
        mockUser.setPassword("hashed_password");
        mockUser.setFullName("Yousef Mohammed");
        mockUser.setPhoneNumber("01000000000");
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("yousef_jo", "Yousef Mohammed", "jo@payon.com", "plain_password", "01000000000",null,null);

        when(userRepo.existsByUserName(request.getUsername())).thenReturn(false);
        when(userRepo.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed_password");
        when(jwtService.generateTokens(any(), any())).thenReturn(Map.of("accessToken", "access_jws", "refreshToken", "refresh_jws"));

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("yousef_jo", response.getUserName());
        verify(userRepo, times(1)).save(any(users.class));
    }

    @Test
    void register_ThrowsException_WhenUsernameTaken() {
        RegisterRequest request = new RegisterRequest("yousef_jo", "Yousef", "jo@payon.com", "pass", "010",null,null);
        when(userRepo.existsByUserName(request.getUsername())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Username is already taken", ex.getReason());
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("yousef_jo", "plain_password");

        when(userRepo.findByUserName(request.getUsername())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.getPassword(), mockUser.getPassword())).thenReturn(true);
        when(jwtService.generateTokens(mockUser.getFullName(), mockUser.getUserName()))
                .thenReturn(Map.of("accessToken", "access_jws", "refreshToken", "refresh_jws"));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_jws", response.getToken());
    }

    @Test
    void login_ThrowsException_WhenPasswordInvalid() {
        LoginRequest request = new LoginRequest("yousef_jo", "wrong_password");
        when(userRepo.findByUserName(request.getUsername())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.getPassword(), mockUser.getPassword())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void updateUser_Success() {
        // هنا بنشحن الـ SecurityContextHolder يدوياً عشان يلقط اليوزر الحالي
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("yousef_jo");
        SecurityContextHolder.setContext(securityContext);

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setEmail("new_email@payon.com");
        updateRequest.setFullname("Yousef New Name");

        when(userRepo.findByUserName("yousef_jo")).thenReturn(Optional.of(mockUser));

        authService.updateUser(updateRequest);

        assertEquals("new_email@payon.com", mockUser.getEmail());
        assertEquals("Yousef New Name", mockUser.getFullName());
        verify(userRepo, times(1)).save(mockUser);

        SecurityContextHolder.clearContext(); // تنظيف الـ Context بعد التيست
    }

    @Test
    void uploadThumbnailFile_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "fake image data".getBytes());
        when(authentication.getPrincipal()).thenReturn("yousef_jo");
        when(userRepo.findByUserName("yousef_jo")).thenReturn(Optional.of(mockUser));

        String savedPath = authService.uploadThumbnailFile(file, authentication);

        assertNotNull(savedPath);
        assertTrue(savedPath.contains("yousef_jo.png"));
        verify(userRepo, times(1)).save(mockUser);
    }

    @Test
    void getUser_ThrowsException_WhenUnauthenticated() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.getUser(null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}