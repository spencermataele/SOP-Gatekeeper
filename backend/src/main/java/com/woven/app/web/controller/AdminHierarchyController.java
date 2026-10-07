package com.woven.app.web.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
@RestController
@RequestMapping("/admin/hierarchy")
public class AdminHierarchyController {
 private final JdbcTemplate jdbc;
 public AdminHierarchyController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @GetMapping
 public List<Map<String,Object>> hierarchy(){return jdbc.queryForList("SELECT node_key AS nodeKey,parent_key AS parentKey,name,kind FROM sop_library_hierarchy ORDER BY name,node_key");}
}
