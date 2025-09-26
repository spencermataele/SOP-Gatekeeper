package com.woven.app.service;

import com.woven.app.domain.Org;
import com.woven.app.domain.OrgGroup;
import com.woven.app.repository.OrgGroupRepository;
import com.woven.app.repository.OrgRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrgGroupService {

    private final OrgGroupRepository groupRepo;
    private final OrgRepository orgRepo;

    public OrgGroupService(OrgGroupRepository groupRepo, OrgRepository orgRepo) {
        this.groupRepo = groupRepo;
        this.orgRepo = orgRepo;
    }

    @Transactional(readOnly = true)
    public List<OrgGroup> list() {
        return groupRepo.findAll();
    }

    @Transactional(readOnly = true)
    public OrgGroup get(Integer id) {
        return groupRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("OrgGroup " + id + " not found"));
    }

    public OrgGroup create(Integer orgId, String orgGroupName) {
        Org org = orgRepo.findById(orgId)
                .orElseThrow(() -> new EntityNotFoundException("Org " + orgId + " not found"));

        OrgGroup g = OrgGroup.builder()
                .orgGroupName(orgGroupName)
                .org(org)
                .build();

        return groupRepo.save(g);
    }

    public OrgGroup update(Integer id, Integer orgId, String orgGroupName) {
        OrgGroup existing = get(id);

        if (orgId != null && (existing.getOrg() == null
                || !orgId.equals(existing.getOrg().getOrgId()))) {
            Org newOrg = orgRepo.findById(orgId)
                    .orElseThrow(() -> new EntityNotFoundException("Org " + orgId + " not found"));
            existing.setOrg(newOrg);
        }

        if (orgGroupName != null && !orgGroupName.isBlank()) {
            existing.setOrgGroupName(orgGroupName);
        }

        return groupRepo.save(existing);
    }

    public void delete(Integer id) {
        // guard against deleting a group that still has departments
        if (!get(id).getDepartments().isEmpty()) { throw new IllegalStateException("Cannot delete Org Groups with associated departments"); }
        groupRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<OrgGroup> listByOrg(Integer orgId) {
        return groupRepo.findByOrg_OrgId(orgId);
    }
}

