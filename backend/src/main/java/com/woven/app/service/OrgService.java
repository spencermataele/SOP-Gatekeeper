package com.woven.app.service;

import com.woven.app.domain.Org;
import com.woven.app.repository.OrgRepository;
import com.woven.app.web.dto.admin.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrgService {
    private final OrgRepository repo;
    public OrgService(OrgRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<OrgDto> listWithGroups() {
        return repo.findAllWithGroups().stream()
                .map(o -> new OrgDto(
                        o.getOrgId(),
                        o.getOrgName(),
                        o.getOrgGroups() == null ? List.of()
                                : o.getOrgGroups().stream()
                                .map(g -> new OrgGroupSlimDto(g.getOrgGroupId(), g.getOrgGroupName()))
                                .toList()
                ))
                .toList();
    }

    @Transactional
    public OrgDto create(OrgCreateDto dto) {
        Org entity = new Org();
        entity.setOrgName(dto.orgName());
        Org saved = repo.save(entity);
        return new OrgDto(saved.getOrgId(), saved.getOrgName(), List.of());
    }


    @Transactional
    public OrgDto update(Integer id, @Valid OrgCreateDto dto) {
        Org o = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Org " + id + " not found"));
        o.setOrgName(dto.orgName());
        Org saved = repo.save(o);
        return new OrgDto(saved.getOrgId(), saved.getOrgName(), List.of());
    }

    @Transactional
    public void delete(Integer id) {
        if (!repo.existsById(id)) {
            throw new EntityNotFoundException("Org " + id + " not found");
        }
        repo.deleteById(id);
    }
}






