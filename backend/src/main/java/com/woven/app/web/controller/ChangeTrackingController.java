package com.woven.app.web.controller;
import com.woven.app.service.governance.ChangeTrackingService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/lifecycle/changes")
@ConditionalOnProperty(name="app.lifecycle.api-enabled",havingValue="true")
public class ChangeTrackingController {
 private final ChangeTrackingService changes;
 public ChangeTrackingController(ChangeTrackingService changes){this.changes=changes;}
 @GetMapping public List<Map<String,Object>> list(){return changes.list();}
 @GetMapping("/{id}") public Map<String,Object> read(@PathVariable UUID id){return changes.read(id.toString());}
 @PutMapping("/{id}") public Map<String,Object> update(@PathVariable UUID id,@Valid @RequestBody ChangeTrackingService.Update body){return changes.update(id.toString(),body);}
}
