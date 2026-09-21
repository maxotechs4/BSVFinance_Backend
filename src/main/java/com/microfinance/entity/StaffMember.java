package com.microfinance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A field/collection staff member (e.g. a loan officer/agent) who can be
 * assigned to a customer at Create Member time. This is a business record
 * managed only from the admin-only "Staff" tab — it is unrelated to the
 * login {@link com.microfinance.entity.enums.Role#STAFF} role on
 * {@link Admin}, which controls app sign-in permissions instead.
 */
@Entity
@Table(name = "staff_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "alternate_phone_number", length = 15)
    private String alternatePhoneNumber;

    @Column(length = 100)
    private String place;

    // ── Nominee 1 ────────────────────────────────────────────────────────────
    @Column(name = "nominee1_name", length = 100)
    private String nominee1Name;

    @Column(name = "nominee1_phone_number", length = 15)
    private String nominee1PhoneNumber;

    // ── Nominee 2 ────────────────────────────────────────────────────────────
    @Column(name = "nominee2_name", length = 100)
    private String nominee2Name;

    @Column(name = "nominee2_phone_number", length = 15)
    private String nominee2PhoneNumber;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
