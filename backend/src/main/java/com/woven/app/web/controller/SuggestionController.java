package com.woven.app.web.controller;

import com.woven.app.service.governance.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/lifecycle/suggestions")
@ConditionalOnProperty(name="app.lifecycle.api-enabled",havingValue="true")
public class SuggestionController {
    private final SuggestionService suggestions;
    public SuggestionController(SuggestionService suggestions) {this.suggestions=suggestions;}
    @GetMapping public List<Map<String,Object>> list() {return suggestions.list();}
    @GetMapping("/eligible") public List<Map<String,Object>> eligible(@RequestParam long documentId) {return suggestions.eligible(documentId);}
    @GetMapping("/{id}") public Map<String,Object> read(@PathVariable UUID id) {return suggestions.read(id.toString());}
    @PostMapping public Map<String,Object> create(@Valid @RequestBody SuggestionService.Create body) {return suggestions.create(body);}
    @PostMapping("/{id}/actions") public Map<String,Object> act(@PathVariable UUID id,@Valid @RequestBody SuggestionService.Action body) {return suggestions.act(id.toString(),body);}
}
