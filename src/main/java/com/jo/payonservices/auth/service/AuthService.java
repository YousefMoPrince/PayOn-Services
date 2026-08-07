package com.jo.payonservices.auth.service;

import com.jo.payonservices.auth.dto.*;
import com.jo.payonservices.auth.model.Role;
import com.jo.payonservices.auth.model.users;
import com.jo.payonservices.auth.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    private final String UPLOAD_DIR = "storage/thumbnails/";



    public AuthResponse register(RegisterRequest request) {
        if (userRepo.existsByUserName(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already taken");
        }
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already registered");
        }
        Role userRole = Role.INDIVIDUAL;
        LocalDateTime currentTime = LocalDateTime.now();
        Map<String,String> tokens = jwtService.generateTokens(request.getFullname(), request.getUsername(),userRole.toString());
        users user = new users();
        user.setUserName(request.getUsername().trim());
        user.setFullName(request.getFullname().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(currentTime);
        user.setUpdatedAt(null);
        user.setPhoneNumber(request.getPhonenumber());
        user.setThumbnailPath(null);
        user.setRole(userRole);
        userRepo.save(user);
        return new  AuthResponse(user.getUserId(), user.getUserName(),user.getEmail(),user.getPhoneNumber(),user.getFullName(), user.getThumbnailPath(), user.getCreatedAt(),user.getUpdatedAt(), tokens.get("accessToken"),tokens.get("refreshToken"));
    }

    public AuthResponse login(LoginRequest request) {
        users user = userRepo.findByUserName(request.getUsername().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        Map<String,String> tokens = jwtService.generateTokens(user.getFullName(), user.getUserName(),user.getRole().toString());

        return AuthResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .thumbnailPath(user.getThumbnailPath())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .Token(tokens.get("accessToken"))
                .refreshToken(tokens.get("refreshToken"))
                .build();
    }
    public Map<String, String> refreshToken(String refreshToken) {
        String username = refreshTokenService.getUsernameFromToken(refreshToken);

        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired");
        }

        refreshTokenService.deleteRefreshToken(refreshToken);

        users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return jwtService.generateTokens(user.getFullName(), user.getUserName(),user.getRole().toString());
    }

    public void updateUser(UpdateRequest request) {
        String currenrUserName = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        users user = userRepo.findByUserName(currenrUserName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getUsername() != null) user.setUserName(request.getUsername());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getFullname() != null) user.setFullName(request.getFullname());
        if (request.getPhonenumber() != null) user.setPhoneNumber(request.getPhonenumber());
        if (request.getPassword() != null) user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setUpdatedAt(request.getUpdatedAt() != null ? request.getUpdatedAt() : LocalDateTime.now());
        userRepo.save(user);
    }

    public String uploadThumbnailFile(MultipartFile file,Authentication auth) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File cannot be empty");
        }
        String userName = (String) auth.getPrincipal();
        log.info("Attempting to find user with identifier: {}", userName);
        users user = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String extension = ".png";
            if (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")) {
                extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            }

            String fileName = user.getUserName() + extension;
            Path filePath = uploadPath.resolve(fileName);

            Files.deleteIfExists(filePath);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            user.setThumbnailPath(filePath.toString().replace("\\", "/"));
            user.setUpdatedAt(LocalDateTime.now());
            userRepo.save(user);

            return user.getThumbnailPath();

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }
    }
    public AuthResponse getUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        String currenrUserName = (String) authentication.getPrincipal();

        users user = userRepo.findByUserName(currenrUserName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return AuthResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .thumbnailPath(user.getThumbnailPath())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .Token(null)
                .build();
    }
    public void logout(LogoutRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken != null) {
            refreshTokenService.deleteRefreshToken(refreshToken);
        }
        SecurityContextHolder.clearContext();

    }


}