package com.woven.app.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "sop")
public class Sop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sop_id")
    private Integer sopId;

    @NotBlank
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @NotNull
    @Column(name = "author_id", nullable = false)
    private Integer authorId;

    @NotNull
    @Column(name = "org_id", nullable = false)
    private Integer orgId;

    @NotNull
    @Column(name = "org_group_id", nullable = false)
    private Integer orgGroupId;

    @NotNull
    @Column(name = "department_id", nullable = false)
    private Integer departmentId;

    @NotNull
    @Column(name = "dept_subgroup_id", nullable = false)
    private Integer deptSubgroupId;

    @NotNull
    @Column(name = "current_business_process_owner_id", nullable = false)
    private Integer currentProcessOwnerId;

    @NotNull
    @Column(name = "current_business_process_owner_position_id", nullable = false)
    private Integer currentProcessOwnerPositionId;

    @NotNull
    @Column(name = "business_process_id", nullable = false)
    private Integer processId;

    @NotBlank
    @Column(name = "business_process_name", nullable = false, length = 255)
    private String processName;

    @NotNull
    @Column(name = "business_process_family_id", nullable = false)
    private Integer processFamilyId;

    @NotNull
    @Column(name = "parent_business_process_id", nullable = false)
    private Integer parentProcessId;

    @CreationTimestamp
    @Column(name = "created_timestamp", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdTimestamp;

    @UpdateTimestamp
    @Column(name = "updated_timestamp", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedTimestamp;

    @NotBlank
    @Column(name = "version_id", nullable = false, length = 255)
    private String versionId;

    @Lob
    @Column(name = "sop_description", columnDefinition = "LONGTEXT", nullable = false)
    private String sopDescription;

    @Lob
    @Column(name = "sop_details", columnDefinition = "LONGTEXT", nullable = false)
    private String sopDetails;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "published_timestamp")
    private Instant publishedTimestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id")
    private ChangeRequest changeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supersedes_sop_id")
    private Sop supersedesSopId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SopStatus status;

    @PrePersist
    public void prePersist() {
        System.out.println(">>> PrePersist fired. sopDescription before: " + sopDescription);
        if (this.sopDescription == null || this.sopDescription.isBlank()) {
            this.sopDescription = "N/A";
        }

        if (this.isActive == null) {
            this.isActive = false;
        }

        if (this.status == null) {
            this.status = SopStatus.DRAFT;
        }

    }

}
