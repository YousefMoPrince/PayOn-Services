package com.jo.payonservices.auth.controller;

import com.jo.payonservices.auth.dto.*;
import com.jo.payonservices.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

@Autowired
private AuthService authService;



    @PostMapping("/register")
    public ResponseEntity<AuthApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthApiResponse<>("User registered successfully", authResponse));
    }


    @PostMapping("/login")
    public ResponseEntity<AuthApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(new AuthApiResponse<>("Login successful", authResponse));
    }
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> request) {
        String oldRefreshToken = request.get("refreshToken");
        return ResponseEntity.ok(authService.refreshToken(oldRefreshToken));
    }


    @PutMapping("/update")
    public ResponseEntity<AuthApiResponse<AuthResponse>> update(@RequestBody UpdateRequest request) {
        authService.updateUser(request);
        return ResponseEntity.ok(new AuthApiResponse<>("User profile updated successfully",null));
    }


    @PostMapping("/upload-thumbnail")
    public ResponseEntity<Map<String, Object>> uploadThumbnail(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }


        String filePath = authService.uploadThumbnailFile(file, authentication);

        return ResponseEntity.ok(Map.of(
                "message", "Thumbnail uploaded successfully",
                "data", Map.of("thumbnail_path", filePath)
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok("Logged out successfully");
    }


    @GetMapping("/me")
    public ResponseEntity<AuthApiResponse<AuthResponse>> getUser(Authentication authentication) {
        AuthResponse authResponse = authService.getUser(authentication);
        return ResponseEntity.ok(new AuthApiResponse<>("User profile retrieved successfully", authResponse));
    }

}