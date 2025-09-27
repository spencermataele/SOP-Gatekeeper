package com.woven.app.service;

import com.woven.app.domain.Org;
import com.woven.app.domain.OrgGroup;
import com.woven.app.repository.OrgRepository;
import com.woven.app.web.dto.admin.*;
import jakarta.persistence.EntityNotFoundException;
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
}






