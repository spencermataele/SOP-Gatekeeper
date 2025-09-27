package com.woven.app.service;

import com.woven.app.domain.Org;
import com.woven.app.domain.OrgGroup;
import com.woven.app.repository.OrgGroupRepository;
import com.woven.app.repository.OrgRepository;
import com.woven.app.web.dto.admin.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrgGroupService {
    private final OrgGroupRepository repo;
    private final OrgRepository orgRepo;

    public OrgGroupService(OrgGroupRepository repo, OrgRepository orgRepo) {
        this.repo = repo;
        this.orgRepo = orgRepo;
    }

    @Transactional(readOnly = true)
    public List<OrgGroupDto> listWithDepartments() {
        return repo.findAllWithDepartments().stream().map(this::toDto).toList();
    }

    public OrgGroupDto create(OrgGroupCreateDto dto) {
        Org org = orgRepo.findById(dto.orgId()).orElseThrow(() -> new EntityNotFoundException("Org " + dto.orgId() + " not found"));
        OrgGroup g = OrgGroup.builder().orgGroupName(dto.orgGroupName()).org(org).build();
        OrgGroup saved = repo.save(g);
        return new OrgGroupDto(saved.getOrgGroupId(), saved.getOrgGroupName(), org.getOrgId(), List.of());
    }

    public OrgGroupDto update(Integer id, OrgGroupCreateDto dto) {
        OrgGroup g = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("OrgGroup " + id + " not found"));
        g.setOrgGroupName(dto.orgGroupName());
        if (!g.getOrg().getOrgId().equals(dto.orgId())) {
            Org org = orgRepo.findById(dto.orgId()).orElseThrow(() -> new EntityNotFoundException("Org " + dto.orgId() + " not found"));
            g.setOrg(org);
        }
        OrgGroup saved = repo.save(g);
        return new OrgGroupDto(saved.getOrgGroupId(), saved.getOrgGroupName(), saved.getOrg().getOrgId(), List.of());
    }

    public void delete(Integer id) {
        if (!repo.existsById(id)) throw new EntityNotFoundException("OrgGroup " + id + " not found");
        repo.deleteById(id);
    }

    private OrgGroupDto toDto(OrgGroup g) {
        var depts = g.getDepartments() == null ? List.<DepartmentSlimDto>of()
                : g.getDepartments().stream().map(d -> new DepartmentSlimDto(d.getDepartmentId(), d.getDepartmentName())).toList();
        Integer orgId = g.getOrg() != null ? g.getOrg().getOrgId() : null;
        return new OrgGroupDto(g.getOrgGroupId(), g.getOrgGroupName(), orgId, depts);
    }
}


