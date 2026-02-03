package com.woven.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Table(name = "change_request")
public class ChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "change_request_id")
    private Long changeRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_sop_id", nullable = false)
    private Sop originalSop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_sop_id", nullable = false)
    private Sop proposedSop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedByUser;

    @Lob
    @Column(name = "change_summary", columnDefinition = "LONGTEXT", nullable = false)
    private String changeSummary;

    @Lob
    @Column(name = "change_reason", columnDefinition = "LONGTEXT", nullable = false)
    private String changeReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_status",nullable = false)
    private ChangeStatus changeStatus;

    @CreationTimestamp
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private Instant createdTimestamp;

    @UpdateTimestamp
    @Column(name = "updated_timestamp", nullable = false)
    private Instant updatedTimestamp;

}
