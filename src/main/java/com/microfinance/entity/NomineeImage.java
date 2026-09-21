package com.microfinance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * One uploaded nominee photo for a {@link Member}. A member can have any
 * number of these (multi-image support) — replaces the old single
 * nominee_image_data/content_type/file_name columns that used to live
 * directly on the members table.
 *
 * Upload is Admin-only (enforced in SecurityConfig, POST
 * /api/members/{id}/nominee-images). Viewing/downloading an already-uploaded
 * image is available to every authenticated role (GET endpoints).
 */
@Entity
@Table(name = "nominee_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NomineeImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    /**
     * Raw image bytes. NOT annotated with @Lob for the same reason as the
     * old Member.nomineeImageData field: on PostgreSQL, Hibernate 6 maps a
     * plain byte[] straight to "bytea", which matches the migration script.
     * @Lob would instead map it to an OID large-object reference and blow up
     * at runtime with a bytea/oid type mismatch.
     */
    @Column(name = "image_data", columnDefinition = "bytea", nullable = false)
    private byte[] imageData;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
