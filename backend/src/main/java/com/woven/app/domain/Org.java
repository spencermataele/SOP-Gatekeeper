package com.woven.app.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="org")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Org {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="org_id")
    private Integer orgId;

    @Column(name="org_name", nullable=false, length=255)
    private String orgName;

    @Column(name="created_timestamp", updatable=false,
            columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdTimestamp;

    @Column(name="last_updated_timestamp",
            columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant lastUpdatedTimestamp;

    @OneToMany(mappedBy = "org", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrgGroup> orgGroups = new ArrayList<>();

    @PrePersist void onCreate() {
        var now = Instant.now();
        if (createdTimestamp == null) createdTimestamp = now;
        lastUpdatedTimestamp = now;
    }
    @PreUpdate void onUpdate() { lastUpdatedTimestamp = Instant.now(); }
}

