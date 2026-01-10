package com.woven.app.web.controller;

import com.woven.app.dto.BusinessProcessFamilyCreateDto;
import com.woven.app.dto.BusinessProcessFamilyDto;
import com.woven.app.service.BusinessProcessFamilyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/business-process-families")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class BusinessProcessFamilyController {
    private final BusinessProcessFamilyService bsvc;

    @GetMapping
    public List<BusinessProcessFamilyDto> list(){
        return bsvc.list();
    }

    @GetMapping("/{id}")
    public BusinessProcessFamilyDto get(@PathVariable Integer id){
        return bsvc.get(id);
    }

    @PostMapping
    public BusinessProcessFamilyDto create(@Valid @RequestBody BusinessProcessFamilyCreateDto dto){
        return bsvc.create(dto);
    }

    @PutMapping(path = "/{id}")
    public BusinessProcessFamilyDto update(@PathVariable Integer id, @Valid @RequestBody BusinessProcessFamilyCreateDto dto) {
        return bsvc.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        bsvc.delete(id);
    }



}
