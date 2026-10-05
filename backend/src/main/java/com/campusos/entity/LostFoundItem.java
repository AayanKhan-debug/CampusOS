package com.campusos.entity;

import com.campusos.enums.ListingType;
import com.campusos.enums.LostFoundStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * LostFoundItem entity — represents an item reported as lost or found by students.
 *
 * Table: lost_found_items
 *
 * Relationships:
 *   LostFoundItem (ManyToOne) → User (reportedBy)
 */
@Entity
@Table(name = "lost_found_items", indexes = {
    @Index(name = "idx_lostfound_status", columnList = "status"),
    @Index(name = "idx_lostfound_listing_type", columnList = "listing_type"),
    @Index(name = "idx_lostfound_reported_by", columnList = "reported_by"),
    @Index(name = "idx_lostfound_date_occurred", columnList = "date_occurred"),
    @Index(name = "idx_lostfound_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LostFoundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "item_type", nullable = false, length = 100)
    private String itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false, length = 20)
    private ListingType listingType;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(name = "date_occurred", nullable = false)
    private LocalDate dateOccurred;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "contact_info", length = 200)
    private String contactInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LostFoundStatus status = LostFoundStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
