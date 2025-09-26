package com.woven.app.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="dept_subgroup")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeptSubgroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="dept_subgroup_id")
    private Integer deptSubgroupId;

    @Column(name="dept_subgroup_name", nullable=false, length=255)
    private String deptSubgroupName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="department_id", nullable=false)
    private Department department;

    @Column(name="created_timestamp", updatable=false,
            columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdTimestamp;

    @Column(name="last_updated_timestamp",
            columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant lastUpdatedTimestamp;

    @PrePersist void onCreate() {
        var now = Instant.now();
        if (createdTimestamp == null) createdTimestamp = now;
        lastUpdatedTimestamp = now;
    }
    @PreUpdate void onUpdate() { lastUpdatedTimestamp = Instant.now(); }
}

