package com.woven.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "process_owner")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "process_owner_id")
    private Integer processOwnerId;

    @Column(name = "process_owner_name", nullable = false, length = 255)
    private String name;

    @Column(name = "process_owner_position_id", nullable = false)
    private Integer positionId;

    @Column(name = "created_timestamp", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdTimestamp;

    @Column(name = "last_updated_timestamp", nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant lastUpdatedTimestamp;
    /*
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_process_owner_id")
    private ProcessOwner parentProcessOwnerID; // nullable
    */
    @PrePersist
    void onCreate() {
        if (createdTimestamp == null) createdTimestamp = Instant.now();
        if (lastUpdatedTimestamp == null) lastUpdatedTimestamp = createdTimestamp;
    }

    @PreUpdate
    void onUpdate() {
        lastUpdatedTimestamp = Instant.now();
    }
}
