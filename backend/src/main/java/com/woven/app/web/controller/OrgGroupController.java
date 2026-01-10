package com.woven.app.web.controller;

import com.woven.app.web.dto.admin.OrgGroupCreateDto;
import com.woven.app.web.dto.admin.OrgGroupDto;
import com.woven.app.service.OrgGroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/org-groups")
@CrossOrigin(origins = "http://localhost:4200")
public class OrgGroupController {

    private final OrgGroupService service;

    public OrgGroupController(OrgGroupService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrgGroupDto> all() {
        return service.listWithDepartments();
    }

    @PostMapping
    public OrgGroupDto create(@Valid @RequestBody OrgGroupCreateDto dto) {
        return service.create(dto);
    }

    @PutMapping(path = "/{id}")
    public OrgGroupDto update(@PathVariable Integer id, @Valid @RequestBody OrgGroupCreateDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}





