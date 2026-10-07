package com.woven.app.service.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woven.app.service.user.AppUserDetails;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class SuggestionService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final ChangeTrackingService changes;
    public SuggestionService(JdbcTemplate jdbc, ObjectMapper json, ChangeTrackingService changes) { this.jdbc=jdbc; this.json=json;this.changes=changes; }
    public record Create(@Positive int processId, @Positive Long documentId,
                         @NotBlank @Size(max=255) String title, @NotBlank @Size(max=10000) String problem,
                         @NotBlank @Size(max=10000) String proposal, @NotBlank @Size(max=10000) String benefit,
                         @Size(max=2000) String costEstimate, @Size(max=2000) String roiEstimate,
                         @NotNull UUID commandId) {}
    public record Action(@NotNull @PositiveOrZero Long version, @NotBlank String action,
                         @NotBlank @Size(max=10000) String message, String concern, String declineReason,
                         @Size(max=10000) String plan, @Size(max=10000) String evidence, LocalDate reviewDate,
                         @NotNull UUID commandId) {}
    public static final Set<String> DECLINE_REASONS=Set.of("EXISTING_STANDARD", "ALTERNATIVE_SELECTED", "DUPLICATE", "INSUFFICIENT_EVIDENCE", "IMPLEMENTATION_CONSTRAINTS");
    private static final Set<String> CONCERNS=Set.of("NONE","UNDETERMINED","TRAINING","DEFECT","BOTH");
    private int actor() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !(auth.getPrincipal() instanceof AppUserDetails user)) throw new SecurityException();
        int id=user.getUser().getId();
        jdbc.queryForObject("SELECT id FROM users WHERE id=?",Integer.class,id);
        return id;
    }
    private int owner(int process) {
        return jdbc.queryForObject("""
            SELECT pg.owner_user_id FROM process_governance pg JOIN sop_library_hierarchy h
            ON h.node_key=CONCAT('process:',pg.business_process_id)
            JOIN client_configuration c ON c.org_id=h.org_id AND c.configuration_id=1
            WHERE pg.business_process_id=? FOR SHARE
            """,Integer.class,process);
    }
    private boolean admin(int user) { return "ADMIN".equals(jdbc.queryForObject("SELECT role FROM users WHERE id=?",String.class,user)); }
    private boolean manager(int user,int owner) { return jdbc.queryForObject("SELECT COUNT(*) FROM user_reporting_line WHERE user_id=? AND manager_user_id=?",Integer.class,owner,user)>0; }
    private int number(Map<String,Object> row,String key) { return ((Number)row.get(key)).intValue(); }
    private String encode(Object body) { try { return json.writeValueAsString(body); } catch(Exception e) { throw new IllegalArgumentException("Invalid message"); } }
    private void require(boolean ok,String message) { if(!ok) throw new IllegalStateException(message); }
    private void text(String value,String message) { if(value==null || value.isBlank()) throw new IllegalArgumentException(message); }
    private Map<String,Object> locked(String id) {
        var row=jdbc.queryForMap("SELECT * FROM process_suggestion WHERE suggestion_id=? FOR UPDATE",id);
        int owner=owner(number(row,"business_process_id")),user=actor();
        if(user!=number(row,"submitter_id") && user!=owner && !admin(user) && !manager(user,owner)) throw new SecurityException();
        return row;
    }
    public List<Map<String,Object>> list() {
        int user=actor();
        return jdbc.queryForList("""
            SELECT s.*, u.full_name AS submitter, p.business_process_name AS processName
            FROM process_suggestion s JOIN users u ON u.id=s.submitter_id
            JOIN business_process p ON p.business_process_id=s.business_process_id
            JOIN process_governance pg ON pg.business_process_id=s.business_process_id
            JOIN sop_library_hierarchy h ON h.node_key=CONCAT('process:',s.business_process_id)
            JOIN client_configuration c ON c.org_id=h.org_id AND c.configuration_id=1
            WHERE s.submitter_id=? OR pg.owner_user_id=? OR EXISTS(SELECT 1 FROM users WHERE id=? AND role='ADMIN')
            OR EXISTS(SELECT 1 FROM user_reporting_line WHERE user_id=pg.owner_user_id AND manager_user_id=?)
            ORDER BY s.created_at DESC
            """,user,user,user,user);
    }
    public List<Map<String,Object>> eligible(long document) {
        actor();var doc=jdbc.queryForMap("SELECT business_process_id FROM sop_document WHERE document_id=?",document);
        int process=number(doc,"business_process_id");owner(process);
        return jdbc.queryForList("SELECT suggestion_id AS id,title FROM process_suggestion WHERE business_process_id=? AND (document_id IS NULL OR document_id=?) AND decision='ACCEPTED' AND state='ACCEPTED' ORDER BY created_at",process,document);
    }
    public List<Map<String,Object>> notices() {
        return jdbc.queryForList("""
            SELECT m.communication_id AS id,m.suggestion_id AS suggestionId,m.action,m.recorded_at AS recordedAt,s.title,u.full_name AS actor
            FROM suggestion_recipient r JOIN suggestion_communication m ON m.communication_id=r.communication_id
            JOIN process_suggestion s ON s.suggestion_id=m.suggestion_id JOIN users u ON u.id=m.actor_id
            JOIN sop_library_hierarchy h ON h.node_key=CONCAT('process:',s.business_process_id)
            JOIN client_configuration c ON c.org_id=h.org_id AND c.configuration_id=1
            WHERE r.user_id=? ORDER BY m.recorded_at DESC LIMIT 100
            """,actor());
    }
    public Map<String,Object> read(String id) {
        var row=new LinkedHashMap<>(locked(id)); int user=actor(),owner=owner(number(row,"business_process_id"));
        row.put("canReview",user==owner); row.put("canRespond",user==number(row,"submitter_id"));
        row.put("canEscalateReview",user!=owner && (admin(user)||manager(user,owner)));
        row.put("owner",jdbc.queryForObject("SELECT full_name FROM users WHERE id=?",String.class,owner));
        var linkedChanges=jdbc.queryForList("SELECT m.change_id AS id,m.title,m.state,m.planned_start AS plannedStart,m.planned_finish AS plannedFinish,u.full_name AS implementationOwner FROM managed_change_suggestion l JOIN managed_change m USING(change_id) JOIN users u ON u.id=m.implementation_owner_id WHERE l.suggestion_id=?",id);
        for(var change:linkedChanges)change.put("tickets",jdbc.queryForList("SELECT system_name AS `system`,ticket_number AS number,ticket_url AS url FROM managed_change_ticket WHERE change_id=?",change.get("id")));
        row.put("changes",linkedChanges);
        row.put("communications",jdbc.queryForList("""
            SELECT m.*,u.full_name AS actor FROM suggestion_communication m JOIN users u ON u.id=m.actor_id
            WHERE suggestion_id=? ORDER BY recorded_at,communication_id
            """,id));
        row.put("recipients",jdbc.queryForList("""
            SELECT r.*,u.full_name AS recipient FROM suggestion_recipient r JOIN users u ON u.id=r.user_id
            JOIN suggestion_communication m ON m.communication_id=r.communication_id WHERE m.suggestion_id=?
            """,id));
        row.put("revisions",jdbc.queryForList("""
            SELECT l.work_item_id AS requestId,w.state FROM suggestion_revision_link l
            JOIN sop_work_item w ON w.work_item_id=l.work_item_id WHERE suggestion_id=?
            """,id));
        return row;
    }
    private boolean replay(UUID command,String id,Object body) {
        var rows=jdbc.queryForList("SELECT * FROM suggestion_communication WHERE communication_id=?",command.toString());
        if(rows.isEmpty()) return false;
        var saved=rows.getFirst();
        if(number(saved,"actor_id")!=actor()) throw new SecurityException();
        try { require(id.equals(saved.get("suggestion_id")) && json.readTree((String)saved.get("details")).equals(json.readTree(encode(body))),"Command was reused with different input"); }
        catch(java.io.IOException e) { throw new IllegalStateException("Cannot read prior command"); }
        return true;
    }
    private void record(String id,UUID command,String action,String message,Object body) {
        var row=jdbc.queryForMap("SELECT * FROM process_suggestion WHERE suggestion_id=?",id);
        int owner=owner(number(row,"business_process_id"));
        jdbc.update("INSERT INTO suggestion_communication(communication_id,suggestion_id,actor_id,action,message,details) VALUES(?,?,?,?,?,?)",
            command.toString(),id,actor(),action,message,encode(body));
        Set<Integer> recipients=new HashSet<>(jdbc.queryForList("SELECT id FROM users WHERE role='ADMIN'",Integer.class));
        recipients.add(owner);recipients.add(number(row,"submitter_id"));
        recipients.addAll(jdbc.queryForList("SELECT manager_user_id FROM user_reporting_line WHERE user_id=?",Integer.class,owner));
        for(int user:recipients) jdbc.update("INSERT INTO suggestion_recipient(communication_id,user_id) VALUES(?,?)",command.toString(),user);
    }
    public Map<String,Object> create(Create body) {
        int user=actor();owner(body.processId());String id=body.commandId().toString();
        if(replay(body.commandId(),id,body)) return read(id);
        if(body.documentId()!=null) require(jdbc.queryForObject("SELECT COUNT(*) FROM sop_document WHERE document_id=? AND business_process_id=? AND current_revision_id IS NOT NULL",Integer.class,body.documentId(),body.processId())==1,"Select a published SOP in this process");
        jdbc.update("""
            INSERT INTO process_suggestion(suggestion_id,business_process_id,document_id,submitter_id,title,problem,proposal,benefit,cost_estimate,roi_estimate)
            VALUES(?,?,?,?,?,?,?,?,?,?)
            """,id,body.processId(),body.documentId(),user,body.title(),body.problem(),body.proposal(),body.benefit(),body.costEstimate(),body.roiEstimate());
        record(id,body.commandId(),"SUBMITTED",body.problem(),body);return read(id);
    }
    public Map<String,Object> act(String id,Action body) {
        var row=locked(id);if(replay(body.commandId(),id,body)) return read(id);
        require(((Number)row.get("lock_version")).longValue()==body.version(),"Suggestion changed. Refresh before responding.");
        int user=actor(),owner=owner(number(row,"business_process_id"));
        boolean submitter=user==number(row,"submitter_id"),reviewer=user==owner;
        String state=(String)row.get("state"), decision=(String)row.get("decision"),concern=(String)row.get("concern"),actionState=(String)row.get("action_state");
        String reason=(String)row.get("decline_reason"),plan=(String)row.get("action_plan"),evidence=(String)row.get("evidence");
        Object date=row.get("review_date");
        require(!state.equals("CLOSED") || body.action().equals("REOPEN"),"Closed suggestions must be reopened first");
        switch(body.action()) {
            case "ACCEPT", "DECLINE", "CLARIFY" -> {
                if(!reviewer) throw new SecurityException();
                require(Set.of("SUBMITTED","NEEDS_CLARIFICATION","ALIGNMENT_UNRESOLVED","DECLINED_AWAITING_RESPONSE").contains(state),"Suggestion is not awaiting a decision");
                if(!CONCERNS.contains(body.concern()==null?"":body.concern())) throw new IllegalArgumentException("Assess the underlying concern");
                require(!Set.of("DEFECT","BOTH").contains(concern) || Set.of("DEFECT","BOTH").contains(body.concern()),"A recorded defect must retain its defect classification");
                concern=body.concern();
                if(body.action().equals("DECLINE")) {
                    if(!DECLINE_REASONS.contains(body.declineReason()==null?"":body.declineReason())) throw new IllegalArgumentException("Choose a decline reason");
                    reason=body.declineReason();decision="DECLINED";state="DECLINED_AWAITING_RESPONSE";
                } else if(body.action().equals("ACCEPT")) {decision="ACCEPTED";state="ACCEPTED";reason=null;}
                else state="NEEDS_CLARIFICATION";
            }
            case "RESPOND" -> { if(!submitter) throw new SecurityException();require(state.equals("NEEDS_CLARIFICATION"),"No clarification requested");state="SUBMITTED"; }
            case "AGREE_NEXT_STEPS" -> {if(!submitter) throw new SecurityException();require(state.equals("DECLINED_AWAITING_RESPONSE"),"No decline awaiting response");state="FOLLOW_UP_REQUIRED";}
            case "CHALLENGE" -> {if(!submitter) throw new SecurityException();state="ALIGNMENT_UNRESOLVED";}
            case "ESCALATION_RESPONSE" -> {if(user==owner || !(admin(user)||manager(user,owner))) throw new SecurityException();require(state.equals("ALIGNMENT_UNRESOLVED"),"No unresolved alignment to review");}
            case "PLAN", "CONTAIN", "VERIFY", "PROPOSE_CLOSURE" -> {
                if(!reviewer) throw new SecurityException();
                require(!Set.of("SUBMITTED","NEEDS_CLARIFICATION","DECLINED_AWAITING_RESPONSE","ALIGNMENT_UNRESOLVED").contains(state),"Resolve the outstanding decision or response first");
                if(body.concern()!=null) {
                    if(!CONCERNS.contains(body.concern())) throw new IllegalArgumentException("Assess the underlying concern");
                    require(!Set.of("DEFECT","BOTH").contains(concern) || Set.of("DEFECT","BOTH").contains(body.concern()),"A recorded defect must retain its defect classification");
                    concern=body.concern();
                }
                text(body.plan(),"Document the corrective action or training plan");plan=body.plan();
                state=decision.equals("ACCEPTED")?"ACCEPTED":"FOLLOW_UP_REQUIRED";
                if(body.reviewDate()==null) throw new IllegalArgumentException("An effectiveness review date is required");date=body.reviewDate();
                if(body.action().equals("PLAN")) {actionState="IN_PROGRESS";evidence=null;}
                else if(body.action().equals("CONTAIN")) {actionState="TEMPORARY_CONTAINMENT";evidence=null;}
                else {
                    text(body.evidence(),"Record training, understanding, or effectiveness evidence");evidence=body.evidence();
                    actionState="AWAITING_VERIFICATION";
                    if(body.action().equals("PROPOSE_CLOSURE")) {
                        require(!concern.equals("UNDETERMINED"),"Assess the concern before proposing closure");
                        require(!"TEMPORARY_CONTAINMENT".equals(row.get("action_state")),"Temporary containment is not a permanent resolution; document a corrective plan first");
                        require(!body.reviewDate().isAfter(LocalDate.now()),"Complete the effectiveness review before proposing closure");
                        state="AWAITING_AGREEMENT";
                    }
                }
            }
            case "AGREE_RESOLUTION" -> {if(!submitter) throw new SecurityException();require(state.equals("AWAITING_AGREEMENT"),"No verified resolution awaiting agreement");state="CLOSED";actionState="VERIFIED";}
            case "REOPEN" -> {if(!submitter && !reviewer) throw new SecurityException();require(state.equals("CLOSED"),"Suggestion is already open");state="ALIGNMENT_UNRESOLVED";actionState="OPEN";}
            case "COMMENT" -> { }
            default -> throw new IllegalArgumentException("Unknown suggestion action");
        }
        jdbc.update("""
            UPDATE process_suggestion SET state=?,decision=?,concern=?,action_state=?,decline_reason=?,action_plan=?,evidence=?,review_date=?,lock_version=lock_version+1 WHERE suggestion_id=?
            """,state,decision,concern,actionState,reason,plan,evidence,date,id);
        if(body.action().equals("ACCEPT"))changes.ensureAccepted(id);
        record(id,body.commandId(),body.action(),body.message(),body);return read(id);
    }
    public void authorizeRevision(int process,long document,String suggestion,String rationale) {
        int user=actor(),owner=owner(process);
        text(rationale,"Explain why this revision is needed");
        if(suggestion==null || suggestion.isBlank()) { if(user!=owner) throw new SecurityException("An accepted suggestion is required");return; }
        var row=jdbc.queryForMap("SELECT * FROM process_suggestion WHERE suggestion_id=? FOR UPDATE",suggestion);
        require(number(row,"business_process_id")==process && (row.get("document_id")==null || ((Number)row.get("document_id")).longValue()==document),"Suggestion must apply to this process and SOP");
        require("ACCEPTED".equals(row.get("decision")) && "ACCEPTED".equals(row.get("state")),"Suggestion must be accepted and have no unresolved alignment");
    }
    public void linkRevision(String suggestion,long request,String rationale) {
        jdbc.update("UPDATE sop_work_item SET initiation_reason=? WHERE work_item_id=?",rationale,request);
        if(suggestion!=null && !suggestion.isBlank()) {
            jdbc.update("INSERT INTO suggestion_revision_link(suggestion_id,work_item_id,linked_by) VALUES(?,?,?)",suggestion,request,actor());
            record(suggestion,UUID.randomUUID(),"REVISION_LINKED",rationale,Map.of("requestId",request));
        }
    }
}
