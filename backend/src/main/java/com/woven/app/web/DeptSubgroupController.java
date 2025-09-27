package com.woven.app.web;

import com.woven.app.domain.DeptSubgroup;
import com.woven.app.service.DeptSubgroupService;
import com.woven.app.web.dto.admin.DeptSubgroupCreateDto;
import com.woven.app.dto.DeptSubgroupUpdateDto;
import jakarta.validation.Valid;
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

    @GetMapping
    public List<DeptSubgroup> all() {
        return service.list();
    }

    @GetMapping("/{id}")
    public DeptSubgroup one(@PathVariable Integer id) {
        return service.get(id);
    }

    // List subgroups under a specific Department
    @GetMapping("/by-department/{departmentId}")
    public List<DeptSubgroup> byDepartment(@PathVariable Integer departmentId) {
        return service.listByDepartment(departmentId);
    }

    @PostMapping
    public DeptSubgroup create(@Valid @RequestBody DeptSubgroupCreateDto dto) {
        return service.create(dto.departmentId(), dto.deptSubgroupName());
    }

    @PutMapping("/{id}")
    public DeptSubgroup update(@PathVariable Integer id, @Valid @RequestBody DeptSubgroupUpdateDto dto) {
        return service.update(id, dto.departmentId(), dto.deptSubgroupName());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}

