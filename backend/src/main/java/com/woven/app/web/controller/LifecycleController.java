package com.woven.app.web.controller;

import com.woven.app.service.governance.ApprovalPolicy;
import com.woven.app.service.governance.LifecycleWorkflowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/lifecycle")
@ConditionalOnProperty(name = "app.lifecycle.api-enabled", havingValue = "true")
public class LifecycleController {
    private final LifecycleWorkflowService workflow;
    public LifecycleController(LifecycleWorkflowService workflow) { this.workflow = workflow; }

    @GetMapping("/processes")
    public List<Map<String, Object>> processes() { return workflow.processes(); }
    @GetMapping("/requests")
    public List<Map<String, Object>> inbox() { return workflow.inbox(); }
    @GetMapping("/notifications")
    public List<Map<String, Object>> notices() { return workflow.notices(); }
    @GetMapping("/documents/{id}/history")
    public List<Map<String, Object>> history(@PathVariable long id) { return workflow.publishedHistory(id); }

    public record Create(@Positive int processId, @NotNull @Size(max = 255) String title,
                         @NotNull String description, @NotNull String details, @NotNull UUID commandId) {}
    public record Start(@NotNull @Positive Long publishedRevisionId, @NotNull UUID commandId) {}
    public record Save(@NotNull @PositiveOrZero Long requestVersion, @NotNull @PositiveOrZero Long copyVersion,
                       @NotNull @Size(max = 255) String title, @NotNull String description,
                       @NotNull String details, @NotNull UUID commandId) {}
    public record Submit(@NotNull @Positive Long copyId, @NotNull @PositiveOrZero Long requestVersion,
                         @NotNull @PositiveOrZero Long copyVersion, String reason, @NotNull UUID commandId) {}
    public record Candidate(@NotNull @Positive Long candidateId, @NotNull @PositiveOrZero Long requestVersion,
                            String reason, @NotNull UUID commandId) {}
    public record Approval(@NotNull @Positive Long candidateId, @NotNull @PositiveOrZero Long requestVersion,
                           @NotNull ApprovalPolicy.Mode mode, String reason, @NotNull UUID commandId) {}
    public record Cancel(@NotNull @PositiveOrZero Long requestVersion, @NotNull UUID commandId) {}

    @GetMapping("/documents")
    public List<Map<String, Object>> published() { return workflow.publishedDocuments(); }

    @PostMapping("/documents")
    public LifecycleWorkflowService.CopyResult create(@Valid @RequestBody Create body) {
        return workflow.createSop(body.processId(), body.title(), body.description(), body.details(), body.commandId());
    }

    @PostMapping("/documents/{id}/drafts")
    public LifecycleWorkflowService.CopyResult start(@PathVariable long id, @Valid @RequestBody Start body) {
        return workflow.startRevision(id, body.publishedRevisionId(), body.commandId());
    }

    @GetMapping("/requests/{id}")
    public Map<String, Object> request(@PathVariable long id) { return workflow.readRequest(id); }

    @GetMapping("/requests/{id}/copies/{copyId}")
    public Map<String, Object> copy(@PathVariable long id, @PathVariable long copyId) { return workflow.readCopy(id, copyId); }

    @PutMapping("/requests/{id}/copies/{copyId}")
    public LifecycleWorkflowService.CopyResult save(@PathVariable long id, @PathVariable long copyId, @Valid @RequestBody Save body) {
        return workflow.saveCopy(id, copyId, body.requestVersion(), body.copyVersion(), body.title(), body.description(), body.details(), body.commandId());
    }

    @PostMapping("/requests/{id}/reviewer-copies")
    public LifecycleWorkflowService.CopyResult suggest(@PathVariable long id, @Valid @RequestBody Candidate body) {
        return workflow.suggestEdits(id, body.candidateId(), body.requestVersion(), body.commandId());
    }

    @PostMapping("/requests/{id}/submit")
    public Map<String, Long> submit(@PathVariable long id, @Valid @RequestBody Submit body) {
        return Map.of("candidateId", workflow.submit(id, body.copyId(), body.requestVersion(), body.copyVersion(), body.reason(), body.commandId()));
    }

    @PostMapping("/requests/{id}/approve")
    public Map<String, Long> approve(@PathVariable long id, @Valid @RequestBody Approval body) {
        return Map.of("candidateId", workflow.approve(id, body.candidateId(), body.requestVersion(), body.mode(), body.reason(), body.commandId()));
    }

    @PostMapping("/requests/{id}/reject")
    public Map<String, Long> reject(@PathVariable long id, @Valid @RequestBody Candidate body) {
        return Map.of("candidateId", workflow.reject(id, body.candidateId(), body.requestVersion(), body.reason(), body.commandId()));
    }

    @PostMapping("/requests/{id}/cancel")
    public Map<String, Long> cancel(@PathVariable long id, @Valid @RequestBody Cancel body) {
        return Map.of("version", workflow.cancel(id, body.requestVersion(), body.commandId()));
    }

    @PostMapping("/requests/{id}/revise")
    public LifecycleWorkflowService.CopyResult revise(@PathVariable long id, @Valid @RequestBody Cancel body) {
        return workflow.reviseRejected(id, body.requestVersion(), body.commandId());
    }

    @PostMapping("/requests/{id}/reassign")
    public Map<String, Long> reassign(@PathVariable long id, @Valid @RequestBody Candidate body) {
        return Map.of("candidateId", workflow.reassign(id, body.candidateId(), body.requestVersion(), body.reason(), body.commandId()));
    }
}
