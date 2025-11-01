package com.woven.app.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
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

    @Column(name = "sop_location_path", length = 255)
    private String sopLocationPath;

    @Column(name = "created_timestamp", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdTimestamp;

    @Column(name = "updated_timestamp", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Instant updatedTimestamp;

    @NotBlank
    @Column(name = "version_id", nullable = false, length = 255)
    private String versionId;
    /* MVP will not include description
    @Lob
    @Column(name = "sop_description", columnDefinition = "LONGTEXT", nullable = false)
    private String sopDescription;
    */
    @Lob
    @Column(name = "sop_details", columnDefinition = "LONGTEXT", nullable = false)
    private String sopDetails;

    @OneToMany(mappedBy = "sop", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Tool> tools = new ArrayList<>();

    public void addTool(Tool tool) {
        tool.setSop(this);
        this.tools.add(tool);    }


    public void removeTool(Tool tool) {
        tool.setSop(null);
        this.tools.remove(tool);
    }

    public Integer getSop_id() {
        return sopId;
    }

    public void setSop_id(Integer sop_id) {
        this.sopId = sop_id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getAuthor_id() {
        return authorId;
    }

    public void setAuthor_id(Integer author_id) {
        this.authorId = author_id;
    }

    public Integer getOrg_id() {
        return orgId;
    }

    public void setOrg_id(Integer org_id) {
        this.orgId = org_id;
    }

    public Integer getOrg_group_id() {
        return orgGroupId;
    }

    public void setOrg_group_id(Integer org_group_id) {
        this.orgGroupId = org_group_id;
    }

    public Integer getDepartment_id() {
        return departmentId;
    }

    public void setDepartment_id(Integer department_id) {
        this.departmentId = department_id;
    }

    public Integer getDept_subgroup_id() {
        return deptSubgroupId;
    }

    public void setDept_subgroup_id(Integer dept_subgroup_id) {
        this.deptSubgroupId = dept_subgroup_id;
    }

    public Integer getCurrent_process_owner_id() {
        return currentProcessOwnerId;
    }

    public void setCurrent_process_owner_id(Integer current_process_owner_id) {
        this.currentProcessOwnerId = current_process_owner_id;
    }

    public Integer getCurrent_process_owner_position_id() {
        return currentProcessOwnerPositionId;
    }

    public void setCurrent_process_owner_position_id(Integer current_process_owner_position_id) {
        this.currentProcessOwnerPositionId = current_process_owner_position_id;
    }

    public Integer getProcess_id() {
        return processId;
    }

    public void setProcess_id(Integer process_id) {
        this.processId = process_id;
    }

    public String getProcess_name() {
        return processName;
    }

    public void setProcess_name(String process_name) {
        this.processName = process_name;
    }

    public Integer getProcess_family_id() {
        return processFamilyId;
    }

    public void setProcess_family_id(Integer process_family_id) {
        this.processFamilyId = process_family_id;
    }

    public Integer getParent_process_id() {
        return parentProcessId;
    }

    public void setParent_process_id(Integer parent_process_id) {
        this.parentProcessId = parent_process_id;
    }

    public String getSop_location_path() {
        return sopLocationPath;
    }

    public void setSop_location_path(String sop_location_path) {
        this.sopLocationPath = sop_location_path;
    }

    public Instant getCreated_timestamp() {
        return createdTimestamp;
    }

    public void setCreated_timestamp(Instant created_timestamp) {
        this.createdTimestamp = created_timestamp;
    }

    public Instant getUpdated_timestamp() {
        return updatedTimestamp;
    }

    public void setUpdated_timestamp(Instant updated_timestamp) {
        this.updatedTimestamp = updated_timestamp;
    }

    public String getVersion_id() {
        return versionId;
    }

    public void setVersion_id(String version_id) {
        this.versionId = version_id;
    }

    /* MVP will not include description
    public String getSopDescription() {
        return sopDescription;
    }

    public void setSopDescription(String sopDescription) {
        this.sopDescription = sopDescription;
    }
    */
    public String getSopDetails() {
        return sopDetails;
    }

    public void setSopDetails(String sopDetails) {
        this.sopDetails = sopDetails;
    }

    public List<Tool> getTools() { return tools; }
    public void setTools(List<Tool> attributes) { this.tools = attributes; }
}
