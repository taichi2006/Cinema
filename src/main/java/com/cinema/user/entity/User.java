package com.cinema.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "users", schema = "cinema")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    // Alias cho dateOfBirth để tương thích ngược với UserDAO / UserService
    public LocalDate getDateOfBirth() { return dob; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dob = dateOfBirth; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
