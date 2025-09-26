package com.woven.app.web;

import com.woven.app.domain.Org;
import com.woven.app.service.OrgService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orgs")
@CrossOrigin(origins = "http://localhost:4200")
public class OrgController {
    private final OrgService service;

    public OrgController(OrgService service) {
        this.service = service;
    }

    @GetMapping
    public List<Org> all() {
        return service.list();
    }

    @GetMapping("/{id}")
    public Org one(@PathVariable Integer id) {
        return service.get(id);
    }

    @PostMapping
    public Org create(@RequestBody Org body) {
        return service.create(body);
    }
    @PutMapping("/{id}")
    public Org update(@PathVariable Integer id, @RequestBody Org body) {
        return service.update(id, body);
    }
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}

