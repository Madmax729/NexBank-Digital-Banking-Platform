package com.banking.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "user_profiles", schema = "user_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfile {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "auth_user_id", nullable = false, unique = true)
    private UUID authUserId;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(name = "phone_number")
    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    @Column(name = "pin_code")
    private String pinCode;
    @Column(name = "date_of_birth")
    private String dateOfBirth;
    @Column(name = "kyc_verified")
    @Builder.Default
    private boolean kycVerified = false;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
