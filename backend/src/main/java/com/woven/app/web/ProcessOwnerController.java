package com.woven.app.web;

import com.woven.app.domain.ProcessOwner;
import com.woven.app.dto.ProcessOwnerCreateDto;
import com.woven.app.service.ProcessOwnerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/process-owners")
@CrossOrigin(origins = "http://localhost:4200")
public class ProcessOwnerController {

    private final ProcessOwnerService processOwnerService;
    public ProcessOwnerController(ProcessOwnerService processOwnerService) {
        this.processOwnerService = processOwnerService;
    }

    @GetMapping
    public List<ProcessOwner> getAllProcessOwners() {
        return processOwnerService.list();
    }

    @GetMapping("/{id}")
    public ProcessOwner one(@PathVariable Integer id) {
        return processOwnerService.get(id); }

    @PostMapping
    public ProcessOwner create(@Valid @RequestBody ProcessOwnerCreateDto po) {
        return processOwnerService.create(po); }

    @PutMapping("/{id}")
    public ProcessOwner update(@PathVariable Integer id,@Valid @RequestBody ProcessOwnerCreateDto po) {
        return processOwnerService.update(id, po);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) { processOwnerService.delete(id); }
}
