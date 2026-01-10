package com.woven.app.web.controller;

import com.woven.app.repository.OrgRepository;
import com.woven.app.service.OrgService;
import com.woven.app.web.dto.admin.OrgCreateDto;
import com.woven.app.web.dto.admin.OrgDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/orgs")
@CrossOrigin(origins = "http://localhost:4200")
public class OrgController {
    private final OrgRepository repo;

    private final OrgService service;

    public OrgController(OrgRepository repo, OrgService service) {
        this.repo = repo;
        this.service = service;
    }

    @GetMapping
    public List<OrgDto> all() { return service.listWithGroups(); }

    @PostMapping
    public OrgDto create(@Valid @RequestBody OrgCreateDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public OrgDto update(@PathVariable Integer id, @Valid @RequestBody OrgCreateDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}



