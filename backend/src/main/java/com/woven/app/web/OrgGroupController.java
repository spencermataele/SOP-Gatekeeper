package com.woven.app.web;

import com.woven.app.domain.OrgGroup;
import com.woven.app.dto.OrgGroupCreateDto;
import com.woven.app.dto.OrgGroupUpdateDto;
import com.woven.app.service.OrgGroupService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/org-groups")
public class OrgGroupController {
    private final OrgGroupService service;

    public OrgGroupController(OrgGroupService service) { this.service = service; }

    @GetMapping
    public List<OrgGroup> all() { return service.list(); }

    @GetMapping("/by-org/{orgId}")
    public List<OrgGroup> byOrg(@PathVariable Integer orgId) { return service.listByOrg(orgId); }

    @PostMapping
    public OrgGroup create(@RequestBody OrgGroupCreateDto dto) {
        return service.create(dto.orgId(), dto.orgGroupName());
    }

    @PutMapping("/{id}")
    public OrgGroup update(@PathVariable Integer id, @RequestBody OrgGroupUpdateDto dto) {
        return service.update(id, dto.orgId(), dto.orgGroupName());
    }

    @DeleteMapping("/{id}") public void delete(@PathVariable Integer id) { service.delete(id); }
}


