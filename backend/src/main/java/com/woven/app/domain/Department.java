package com.woven.app.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="department")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="department_id")
    private Integer departmentId;

    @Column(name="department_name", nullable=false, length=255)
    private String departmentName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="org_group_id", nullable=false)
    private OrgGroup orgGroup;

    @Column(name="created_timestamp", updatable=false,
            columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdTimestamp;

    @Column(name="last_updated_timestamp",
            columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant lastUpdatedTimestamp;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeptSubgroup> subgroups = new ArrayList<>();

    @PrePersist void onCreate() {
        var now = Instant.now();
        if (createdTimestamp == null) createdTimestamp = now;
        lastUpdatedTimestamp = now;
    }
    @PreUpdate void onUpdate() { lastUpdatedTimestamp = Instant.now(); }
}

