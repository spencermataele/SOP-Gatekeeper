package com.woven.app.web;

import com.woven.app.service.DeptSubgroupService;
import com.woven.app.web.dto.admin.DeptSubgroupCreateDto;
import com.woven.app.web.dto.admin.DeptSubgroupDto;
import com.woven.app.web.dto.admin.DeptSubgroupUpdateDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dept-subgroups")
@CrossOrigin(origins = "http://localhost:4200")
public class DeptSubgroupController {

    private final DeptSubgroupService service;

    public DeptSubgroupController(DeptSubgroupService service) {
        this.service = service;
    }

    // List subgroups
    @GetMapping
    public List<DeptSubgroupDto> all() {
        return service.list();
    }

    // Filters subgroups under a specific Department
    @GetMapping("/by-department/{departmentId}")
    public List<DeptSubgroupDto> byDepartment(@PathVariable Integer departmentId) {
        return service.listByDepartment(departmentId);
    }

    @PostMapping
    public DeptSubgroupDto create(@Valid @RequestBody DeptSubgroupCreateDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public DeptSubgroupDto update(@PathVariable Integer id, @Valid @RequestBody DeptSubgroupUpdateDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}

