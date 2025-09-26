package com.woven.app.web;

import com.woven.app.domain.Department;
import com.woven.app.service.DepartmentService;
import com.woven.app.dto.DepartmentCreateDto;
import com.woven.app.dto.DepartmentUpdateDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/departments")
@CrossOrigin(origins = "http://localhost:4200")
public class DepartmentController {

    private final DepartmentService service;

    public DepartmentController(DepartmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Department> all() {
        return service.list();
    }

    @GetMapping("/{id}")
    public Department one(@PathVariable Integer id) {
        return service.get(id);
    }

    // List departments under a specific Org Group
    @GetMapping("/by-org-group/{orgGroupId}")
    public List<Department> byOrgGroup(@PathVariable Integer orgGroupId) {
        return service.listByOrgGroup(orgGroupId);
    }

    @PostMapping
    public Department create(@Valid @RequestBody DepartmentCreateDto dto) {
        return service.create(dto.orgGroupId(), dto.departmentName());
    }

    @PutMapping("/{id}")
    public Department update(@PathVariable Integer id, @Valid @RequestBody DepartmentUpdateDto dto) {
        return service.update(id, dto.orgGroupId(), dto.departmentName());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}

