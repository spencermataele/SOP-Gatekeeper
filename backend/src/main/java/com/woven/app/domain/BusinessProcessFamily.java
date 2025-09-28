package com.woven.app.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "process_family", uniqueConstraints = @UniqueConstraint(columnNames = {"department_id", "process_family_name"}))
@Getter @Setter
public class BusinessProcessFamily {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer processFamilyId;

    @Column(nullable = false, length = 255)
    private String processFamilyName;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @OneToMany(mappedBy = "businessProcessFamily", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<BusinessProcess> businessProcessList = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdTimestamp = Instant.now();
    @Column(nullable = false)
    private Instant lasUpdatedTimestamp = Instant.now();
    @PreUpdate
    void onUpdate() {
        lasUpdatedTimestamp = Instant.now();
    }
}
