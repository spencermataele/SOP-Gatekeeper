package com.woven.app.service;

import com.woven.app.domain.Department;
import com.woven.app.domain.OrgGroup;
import com.woven.app.repository.OrgGroupRepository;
import com.woven.app.web.dto.admin.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrgGroupService {
    private final OrgGroupRepository repo;

    public OrgGroupService(OrgGroupRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<OrgGroupDto> listWithDepartments() {
        List<OrgGroup> groups = repo.findAllWithDepartments();
        return groups.stream().map(this::toDto).toList();
    }

    private OrgGroupDto toDto(OrgGroup g) {
        List<DepartmentSlimDto> depts = g.getDepartments() == null ? List.of()
                : g.getDepartments().stream()
                .map(this::toSlim)
                .toList();

        Integer orgId = (g.getOrg() != null) ? g.getOrg().getOrgId() : null;
        return new OrgGroupDto(g.getOrgGroupId(), g.getOrgGroupName(), orgId, depts);
    }

    private DepartmentSlimDto toSlim(Department d) {
        return new DepartmentSlimDto(d.getDepartmentId(), d.getDepartmentName());
    }

    @Transactional(readOnly = true)
    public OrgGroupDto getDto(Integer id) {
        OrgGroup g = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("OrgGroup " + id + " not found"));
        Integer orgId = (g.getOrg() != null) ? g.getOrg().getOrgId() : null;
        return new OrgGroupDto(g.getOrgGroupId(), g.getOrgGroupName(), orgId, List.of());
    }
}


