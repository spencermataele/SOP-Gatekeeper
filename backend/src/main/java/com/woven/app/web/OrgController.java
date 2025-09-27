package com.woven.app.web;

import com.woven.app.domain.Org;
import com.woven.app.repository.OrgRepository;
import com.woven.app.web.dto.admin.OrgDto;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/orgs")
@CrossOrigin(origins = "http://localhost:4200")
public class OrgController {
    private final OrgRepository repo;

    public OrgController(OrgRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<OrgDto> list() {
        return repo.findAll().stream()
                .map(org -> new OrgDto(org.getOrgId(), org.getOrgName(), List.of()))
                .toList();
    }

    // for create, avoid returning the entity too
    @PostMapping
    public OrgDto create(@RequestBody Org dto) {
        Org entity = new Org();
        entity.setOrgName(dto.orgName());
        entity.setCreatedTimestamp(Instant.now());
        entity.setLastUpdatedTimestamp(Instant.now());


        Org saved = repo.save(entity);
        return new OrgDto(
                saved.getOrgId(),
                saved.getOrgName(),
                List.of()
        );
    }
}



