package com.woven.app.web;

import com.woven.app.service.OrgGroupService;
import com.woven.app.web.dto.admin.OrgGroupDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/org-groups")
@CrossOrigin(origins = "http://localhost:4200")
public class OrgGroupController {
    private final OrgGroupService service;
    public OrgGroupController(OrgGroupService service) { this.service = service; }

    @GetMapping
    public List<OrgGroupDto> all() {
        return service.listWithDepartments();
    }
}



