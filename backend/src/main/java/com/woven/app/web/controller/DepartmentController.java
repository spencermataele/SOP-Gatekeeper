package com.woven.app.web.controller;

import com.woven.app.service.DepartmentService;
import com.woven.app.web.dto.admin.DepartmentCreateDto;
import com.woven.app.web.dto.admin.DepartmentDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public List<DepartmentDto> all() {
        return service.listWithSubgroups();
    }

    @PostMapping
    public DepartmentDto create(@Valid @RequestBody DepartmentDto dto) {
        return service.create(dto);
    }

    @PutMapping(path = "/{id}")
    public DepartmentDto update(@PathVariable Integer id, @Valid @RequestBody DepartmentCreateDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }


}


