package com.woven.app.web.controller;

import com.woven.app.service.report.ReportService;
import com.woven.app.web.dto.report.OrgHierarchyRowDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:4200")
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/org-hierarchy")
    public List<OrgHierarchyRowDto> orgHierarchy(
            @RequestParam(required = false) Integer orgId,
            @RequestParam(required = false) String nameLike,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "true") boolean includeEmptyChildren
    ) {
        return reports.orgHierarchy(orgId, nameLike, createdFrom, createdTo, includeEmptyChildren);
    }
}

