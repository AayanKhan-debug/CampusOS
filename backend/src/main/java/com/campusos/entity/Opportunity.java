package com.campusos.entity;

import com.campusos.enums.OpportunityStatus;
import com.campusos.enums.OpportunityType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Opportunity entity — represents an internship, hackathon, competition, scholarship, etc.
 *
 * Table: opportunities
 *
 * Relationships:
 *   Opportunity (ManyToOne) → User (createdBy)
 */
@Entity
@Table(name = "opportunities", indexes = {
    @Index(name = "idx_opp_status", columnList = "status"),
    @Index(name = "idx_opp_type", columnList = "opportunity_type"),
    @Index(name = "idx_opp_deadline", columnList = "deadline"),
    @Index(name = "idx_opp_remote", columnList = "remote"),
    @Index(name = "idx_opp_created_by", columnList = "created_by"),
    @Index(name = "idx_opp_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 200)
    private String organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "opportunity_type", nullable = false, length = 30)
    private OpportunityType opportunityType;

    @Column(columnDefinition = "TEXT")
    private String eligibility;

    @Column(name = "application_url", length = 500)
    private String applicationUrl;

    @Column
    private LocalDateTime deadline;

    @Column(length = 200)
    private String location;

    @Column(nullable = false)
    @Builder.Default
    private Boolean remote = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OpportunityStatus status = OpportunityStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
