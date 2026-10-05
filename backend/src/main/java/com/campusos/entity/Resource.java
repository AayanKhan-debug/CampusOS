package com.campusos.entity;

import com.campusos.enums.ResourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Resource entity — leaf node of the Resource Hub hierarchy.
 *
 * Table: resources
 *
 * Hierarchy: Department → Course → Subject → Resource
 *
 * For Phase 4, URLs are used instead of actual file storage.
 * File upload (S3/Cloud Storage) can be added in a future phase.
 */
@Entity
@Table(name = "resources", indexes = {
    @Index(name = "idx_resource_subject", columnList = "subject_id"),
    @Index(name = "idx_resource_type", columnList = "resource_type"),
    @Index(name = "idx_resource_uploaded_by", columnList = "uploaded_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 30)
    private ResourceType resourceType;

    /**
     * URL to the resource (PDF link, Google Drive, YouTube, etc.).
     * File upload support can be added in a future phase.
     */
    @Column(nullable = false, length = 500)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
