package com.woven.app.web.controller;

import com.woven.app.dto.SopDto;
import com.woven.app.service.SopService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sops")
public class SopController {
    private final SopService sopService;

    public SopController(SopService sopService) {
        this.sopService = sopService;
    }

    @GetMapping
    public List<SopDto> sopDtoList() {
        return sopService.list();
    }

    @GetMapping("/{id}")
    public SopDto get(@PathVariable Integer id) {
        return sopService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SopDto create(@RequestBody SopDto sopDto) {
        return sopService.create(sopDto);
    }

    @PutMapping("/{id}")
    public SopDto update(@PathVariable Integer id, @Valid @RequestBody SopDto sopDto) {
        return sopService.update(id, sopDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        sopService.delete(id);
    }
}
