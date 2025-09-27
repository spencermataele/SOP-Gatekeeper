package com.woven.app.web;

import com.woven.app.service.DepartmentService;
import com.woven.app.web.dto.admin.DepartmentDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/departments")
@CrossOrigin(origins = "http://localhost:4200")
public class DepartmentController {
    private final DepartmentService service;
    public DepartmentController(DepartmentService service) { this.service = service; }

    @GetMapping
    public List<DepartmentDto> all() {
        return service.listWithSubgroups();
    }
}


