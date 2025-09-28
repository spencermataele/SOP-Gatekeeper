package com.woven.app.web.controller;

import com.woven.app.dto.BusinessProcessCreateDto;
import com.woven.app.dto.BusinessProcessDto;
import com.woven.app.service.BusinessProcessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/processes")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class BusinessProcessController {
    private final BusinessProcessService svc;

    @GetMapping
    public List<BusinessProcessDto> list() {
        return svc.list();
    }

    @PostMapping
    public BusinessProcessDto create(@Valid @RequestBody BusinessProcessCreateDto dto) {
        return svc.create(dto);
    }

    @PutMapping(path = "/{id}")
    public BusinessProcessDto update(@PathVariable Integer id, @Valid @RequestBody BusinessProcessCreateDto dto) {
        return svc.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        svc.delete(id);
    }

}


