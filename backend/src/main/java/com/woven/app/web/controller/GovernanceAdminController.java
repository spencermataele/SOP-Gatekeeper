package com.woven.app.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woven.app.service.user.AppUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/lifecycle/admin")
@ConditionalOnProperty(name="app.lifecycle.api-enabled", havingValue="true")
@Transactional
public class GovernanceAdminController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public GovernanceAdminController(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc=jdbc; this.json=json; }
    public record Client(@Positive int orgId, @NotBlank @Size(max=2000) String reason) {}
    public record Assignment(@Positive int ownerId, Integer managerId, Long expectedVersion,
                             Integer expectedManagerId, @NotBlank @Size(max=2000) String reason) {}
    private int admin() {
        var principal=(AppUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int actor=principal.getUser().getId();
        if (!"ADMIN".equals(jdbc.queryForObject("SELECT role FROM users WHERE id=?", String.class, actor)))
            throw new SecurityException("Administrator required");
        return actor;
    }
    @GetMapping("/setup")
    public Map<String,Object> setup() {
        admin();
        var configured=jdbc.queryForList("SELECT org_id AS orgId FROM client_configuration WHERE configuration_id=1");
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("clientOrgId", configured.isEmpty()?null:configured.getFirst().get("orgId"));
        result.put("organizations",jdbc.queryForList("SELECT org_id AS id,org_name AS name FROM org ORDER BY org_name"));
        result.put("users",jdbc.queryForList("""
                SELECT u.id,u.full_name AS name,u.username,l.manager_user_id AS managerId
                FROM users u LEFT JOIN user_reporting_line l ON l.user_id=u.id ORDER BY u.full_name
                """));
        result.put("processes",jdbc.queryForList("""
                SELECT p.business_process_id AS id,p.business_process_name AS name,
                       g.org_group_name AS groupName,d.department_name AS departmentName,
                       f.business_process_family_name AS familyName,pg.owner_user_id AS ownerId,
                       pg.lock_version AS version,l.manager_user_id AS managerId
                FROM business_process p JOIN business_process_family f USING(business_process_family_id)
                JOIN department d USING(department_id) JOIN org_group g USING(org_group_id)
                JOIN client_configuration c ON c.org_id=g.org_id AND c.configuration_id=1
                LEFT JOIN process_governance pg USING(business_process_id)
                LEFT JOIN user_reporting_line l ON l.user_id=pg.owner_user_id ORDER BY p.business_process_name
                """));
        result.put("activity",jdbc.queryForList("""
                SELECT a.event_id AS id,a.action,a.business_process_id AS processId,a.reason,
                       a.previous_value AS previousValue,a.new_value AS newValue,a.recorded_at AS recordedAt,u.full_name AS actor
                FROM governance_configuration_audit a JOIN users u ON u.id=a.actor_id ORDER BY a.event_id DESC LIMIT 100
                """));
        return result;
    }
    @PutMapping("/client")
    public Map<String,Object> client(@Valid @RequestBody Client body) {
        int actor=admin();
        jdbc.queryForObject("SELECT org_id FROM org WHERE org_id=? FOR UPDATE",Integer.class,body.orgId());
        var existing=jdbc.queryForList("SELECT org_id FROM client_configuration WHERE configuration_id=1 FOR UPDATE");
        if (!existing.isEmpty()) {
            if (((Number)existing.getFirst().get("org_id")).intValue()!=body.orgId())
                throw new IllegalStateException("The deployment organization is already configured. Moving client data requires a separate migration.");
            return setup();
        }
        jdbc.update("INSERT INTO client_configuration VALUES(1,?)",body.orgId());
        audit(actor,null,"CLIENT_CONFIGURED",null,Map.of("orgId",body.orgId()),body.reason());
        return setup();
    }
    @PutMapping("/processes/{id}/ownership")
    public Map<String,Object> assign(@PathVariable int id,@Valid @RequestBody Assignment body) {
        int actor=admin();
        jdbc.queryForObject("""
                SELECT p.business_process_id FROM business_process p JOIN business_process_family f USING(business_process_family_id)
                JOIN department d USING(department_id) JOIN org_group g USING(org_group_id)
                JOIN client_configuration c ON c.org_id=g.org_id AND c.configuration_id=1
                WHERE p.business_process_id=? FOR UPDATE
                """,Integer.class,id);
        var previous=jdbc.queryForList("SELECT owner_user_id AS ownerId,lock_version AS version FROM process_governance WHERE business_process_id=? FOR UPDATE",id);
        Long version=previous.isEmpty()?null:((Number)previous.getFirst().get("version")).longValue();
        if (!Objects.equals(version,body.expectedVersion())) throw new IllegalStateException("Process ownership changed. Refresh before saving.");
        jdbc.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE",Integer.class,body.ownerId());
        if (body.managerId()!=null) {
            if (body.managerId()==body.ownerId()) throw new IllegalArgumentException("An owner cannot be their own manager.");
            jdbc.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE",Integer.class,body.managerId());
        }
        var reporting=jdbc.queryForList("SELECT manager_user_id FROM user_reporting_line WHERE user_id=? FOR UPDATE",body.ownerId());
        Integer manager=reporting.isEmpty()?null:((Number)reporting.getFirst().get("manager_user_id")).intValue();
        if (!Objects.equals(manager,body.expectedManagerId())) throw new IllegalStateException("The owner's reporting line changed. Refresh before saving.");
        Map<String,Object> before=new LinkedHashMap<>();
        before.put("ownership",previous.isEmpty()?null:previous.getFirst());
        before.put("reportingUserId",body.ownerId()); before.put("managerId",manager);
        if (body.managerId()==null) jdbc.update("DELETE FROM user_reporting_line WHERE user_id=?",body.ownerId());
        else jdbc.update("INSERT INTO user_reporting_line(user_id,manager_user_id) VALUES(?,?) ON DUPLICATE KEY UPDATE manager_user_id=?",body.ownerId(),body.managerId(),body.managerId());
        if (version==null) jdbc.update("INSERT INTO process_governance(business_process_id,owner_user_id) VALUES(?,?)",id,body.ownerId());
        else jdbc.update("UPDATE process_governance SET owner_user_id=?,lock_version=lock_version+1 WHERE business_process_id=?",body.ownerId(),id);
        Map<String,Object> after=new LinkedHashMap<>(); after.put("ownerId",body.ownerId()); after.put("managerId",body.managerId());
        audit(actor,id,"OWNERSHIP_CONFIGURED",before,after,body.reason());
        return setup();
    }
    private void audit(int actor,Integer process,String action,Object before,Object after,String reason) {
        try { jdbc.update("INSERT INTO governance_configuration_audit(actor_id,business_process_id,action,previous_value,new_value,reason) VALUES(?,?,?,?,?,?)",
                actor,process,action,before==null?null:json.writeValueAsString(before),json.writeValueAsString(after),reason); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("Configuration audit could not be recorded"); }
    }
}
