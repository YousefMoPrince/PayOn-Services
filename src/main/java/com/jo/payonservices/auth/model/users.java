package com.jo.payonservices.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "users")
public class users {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id",nullable = false,updatable = false)
    private UUID userId;
    @Column(name = "username",nullable = false)
    private String userName;
    @Column(name = "email",nullable = false,length = 100)
    private String email;
    @Column(name = "password",nullable = false,length = 255)
    private String password;
    @Column(name = "full_name",nullable = false,length = 100)
    private String fullName;
    @Column(name = "phone_number",nullable = false,length = 11)
    private String phoneNumber;
    @Column(name = "thumbnail_path")
    private String thumbnailPath;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
