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
//Adding unique constraints to ensure appropriate approval
@Table(name = "change_approval", uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "change_request_id",
                "approver_user_id"
        })
})
public class ChangeApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "change_approval_id")
    private Long changeApprovalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ChangeRequest changeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_user_id", nullable = false)
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_role",nullable = false)
    private ApproverRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_decision",nullable = false)
    private ApprovalDecision decision;

    @Lob
    @Column(name = "comments", columnDefinition = "LONGTEXT", nullable = false)
    private String comments;

    @CreationTimestamp
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private Instant createdTimestamp;

    @UpdateTimestamp
    @Column(name = "updated_timestamp", nullable = false)
    private Instant updatedTimestamp;

}
