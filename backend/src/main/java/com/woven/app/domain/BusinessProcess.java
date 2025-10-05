package com.woven.app.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "business_process", uniqueConstraints = @UniqueConstraint(columnNames = {"process_family_id", "process_name"}))
@Getter @Setter
public class BusinessProcess {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer businessProcessId;

    @Column(nullable = false, length = 255)
    private String businessProcessName;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "business_process_family_id")
    private BusinessProcessFamily businessProcessFamily;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_business_process_id")
    private BusinessProcess parentBusinessProcess;

    @OneToMany(mappedBy = "parentBusinessProcess")
    private List<BusinessProcess> children = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "business_process_dept_subgroup",
            joinColumns = @JoinColumn(name = "business_process_id"),
            inverseJoinColumns = @JoinColumn(name = "dept_subgroup_id")
    )
    private Set<DeptSubgroup> deptSubgroups = new HashSet<>();

    @Column(nullable = false)
    private Instant createdTimestamp = Instant.now();
    @Column(nullable = false)
    private Instant lastUpdatedTimestamp = Instant.now();
    @PreUpdate
    void onUpdate() {
        lastUpdatedTimestamp = Instant.now();
    }
}
