package com.woven.app.service.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woven.app.service.user.AppUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class ChangeTrackingService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public ChangeTrackingService(JdbcTemplate jdbc,ObjectMapper json){this.jdbc=jdbc;this.json=json;}
    public record Ticket(@NotBlank @Size(max=100) String system,@NotBlank @Size(max=100) String number,
                         @NotBlank @Size(max=2000) String url){}
    public record Update(@NotNull @PositiveOrZero Long version,@NotNull UUID commandId,
                         @NotBlank @Size(max=2000) String reason,@NotBlank @Size(max=255) String title,
                         @Positive int ownerId,@NotBlank String state,@NotBlank @Size(max=10000) String plan,
                         @Size(max=10000) String evidence,LocalDate plannedStart,LocalDate plannedFinish,
                         @NotNull @Size(max=20) List<@Valid Ticket> tickets,
                         @NotNull @Size(min=1,max=100) List<@NotNull UUID> suggestionIds){}
    private int actor(){var auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null||!(auth.getPrincipal() instanceof AppUserDetails user))throw new SecurityException();return user.getUser().getId();}
    private int num(Map<String,Object> row,String key){return ((Number)row.get(key)).intValue();}
    private boolean admin(int user){return "ADMIN".equals(jdbc.queryForObject("SELECT role FROM users WHERE id=?",String.class,user));}
    private void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("Invalid change record");}}
    private Map<String,Object> load(String id){return jdbc.queryForMap("""
        SELECT m.*,pg.owner_user_id AS processOwnerId FROM managed_change m JOIN process_governance pg USING(business_process_id)
        JOIN sop_library_hierarchy h ON h.node_key=CONCAT('process:',m.business_process_id)
        JOIN client_configuration c ON c.org_id=h.org_id AND c.configuration_id=1 WHERE m.change_id=? FOR UPDATE
        """,id);}
    private boolean mayEdit(Map<String,Object> row){return actor()==num(row,"processOwnerId")||actor()==num(row,"implementation_owner_id");}
    private void mayRead(Map<String,Object> row){int user=actor();if(mayEdit(row)||admin(user))return;
        if(jdbc.queryForObject("SELECT COUNT(*) FROM user_reporting_line WHERE user_id=? AND manager_user_id=?",Integer.class,num(row,"processOwnerId"),user)>0)return;
        if(jdbc.queryForObject("SELECT COUNT(*) FROM managed_change_suggestion l JOIN process_suggestion s USING(suggestion_id) WHERE l.change_id=? AND s.submitter_id=?",Integer.class,row.get("change_id"),user)>0)return;
        throw new SecurityException();
    }
    public void ensureAccepted(String suggestion){
        var s=jdbc.queryForMap("SELECT s.*,pg.owner_user_id FROM process_suggestion s JOIN process_governance pg USING(business_process_id) WHERE suggestion_id=?",suggestion);
        if(!jdbc.queryForList("SELECT change_id FROM managed_change WHERE origin_suggestion_id=?",String.class,suggestion).isEmpty())return;
        String id=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO managed_change(change_id,origin_suggestion_id,business_process_id,title,implementation_owner_id,plan) VALUES(?,?,?,?,?,?)",id,suggestion,s.get("business_process_id"),s.get("title"),s.get("owner_user_id"),s.get("proposal"));
        jdbc.update("INSERT INTO managed_change_suggestion VALUES(?,?)",id,suggestion);
        jdbc.update("INSERT INTO managed_change_event(event_id,change_id,actor_id,reason,previous_value,new_value,command_payload) VALUES(?,?,?,?,?,?,?)",UUID.randomUUID().toString(),id,actor(),"Created from accepted suggestion","{}",encode(load(id)),"{}");
    }
    public List<Map<String,Object>> list(){
        var rows=jdbc.queryForList("""
          SELECT m.*,u.full_name AS implementationOwner,p.business_process_name AS processName,pg.owner_user_id AS processOwnerId
          FROM managed_change m JOIN users u ON u.id=m.implementation_owner_id JOIN business_process p USING(business_process_id)
          JOIN process_governance pg USING(business_process_id) JOIN sop_library_hierarchy h ON h.node_key=CONCAT('process:',m.business_process_id)
          JOIN client_configuration c ON c.org_id=h.org_id AND c.configuration_id=1 ORDER BY m.created_at DESC
          """);
        return rows.stream().filter(row->{try{mayRead(row);return true;}catch(SecurityException denied){return false;}}).toList();
    }
    public Map<String,Object> read(String id){var row=new LinkedHashMap<>(load(id));mayRead(row);row.put("canEdit",mayEdit(row));row.put("canAssign",actor()==num(row,"processOwnerId"));
        row.put("tickets",jdbc.queryForList("SELECT system_name AS `system`,ticket_number AS number,ticket_url AS url FROM managed_change_ticket WHERE change_id=? ORDER BY system_name,ticket_number",id));
        row.put("suggestions",jdbc.queryForList("SELECT s.suggestion_id AS id,s.title,s.state,s.concern FROM managed_change_suggestion l JOIN process_suggestion s USING(suggestion_id) WHERE change_id=?",id));
        row.put("revisions",jdbc.queryForList("SELECT DISTINCT r.work_item_id AS id,w.state FROM managed_change_suggestion l JOIN suggestion_revision_link r USING(suggestion_id) JOIN sop_work_item w ON w.work_item_id=r.work_item_id WHERE l.change_id=?",id));
        row.put("activity",jdbc.queryForList("SELECT e.*,u.full_name AS actor FROM managed_change_event e JOIN users u ON u.id=e.actor_id WHERE change_id=? ORDER BY recorded_at DESC,event_id",id));
        if(mayEdit(row)){
            row.put("users",jdbc.queryForList("SELECT id,full_name AS name FROM users ORDER BY full_name"));
            row.put("eligibleSuggestions",jdbc.queryForList("SELECT suggestion_id AS id,title FROM process_suggestion WHERE business_process_id=? AND decision='ACCEPTED' ORDER BY title",row.get("business_process_id")));
        }return row;
    }
    public Map<String,Object> update(String id,Update body){
        var row=load(id);if(!mayEdit(row))throw new SecurityException();
        var prior=jdbc.queryForList("SELECT * FROM managed_change_event WHERE event_id=?",body.commandId().toString());
        if(!prior.isEmpty()){
            var event=prior.getFirst();if(num(event,"actor_id")!=actor())throw new SecurityException();
            try{require(id.equals(event.get("change_id"))&&json.readTree((String)event.get("command_payload")).equals(json.readTree(encode(body))),"Command ID reused with different input");}catch(java.io.IOException e){throw new IllegalStateException("Unreadable prior command");}return read(id);
        }
        require(((Number)row.get("lock_version")).longValue()==body.version(),"Change updated by someone else. Reload before saving.");
        if(body.ownerId()!=num(row,"implementation_owner_id")&&actor()!=num(row,"processOwnerId"))throw new SecurityException();
        jdbc.queryForObject("SELECT id FROM users WHERE id=?",Integer.class,body.ownerId());
        var flow=List.of("PLANNED","IN_PROGRESS","READY_FOR_VALIDATION","IMPLEMENTED","EFFECTIVENESS_VERIFIED");
        var allowed=new HashSet<>(flow);allowed.addAll(Set.of("BLOCKED","CANCELLED"));
        if(!allowed.contains(body.state()))throw new IllegalArgumentException("Unknown change status");
        String previous=(String)row.get("state");
        if(previous.equals("EFFECTIVENESS_VERIFIED")&&body.state().equals(previous))require(Objects.equals(row.get("plan"),body.plan()),"A changed implementation plan requires renewed validation; move the change back to an earlier stage");
        if(flow.contains(previous)&&flow.contains(body.state()))require(flow.indexOf(body.state())<=flow.indexOf(previous)+1,"Advance one implementation stage at a time");
        if(Set.of("BLOCKED","CANCELLED").contains(previous))require(Set.of("PLANNED","IN_PROGRESS","BLOCKED","CANCELLED").contains(body.state()),"Resume implementation before validating a blocked or cancelled change");
        if(Set.of("IMPLEMENTED","EFFECTIVENESS_VERIFIED").contains(body.state()))require(body.evidence()!=null&&!body.evidence().isBlank(),"Record implementation or effectiveness evidence");
        if(body.plannedStart()!=null&&body.plannedFinish()!=null)require(!body.plannedFinish().isBefore(body.plannedStart()),"Finish date cannot precede start date");
        Set<String> links=new HashSet<>();for(UUID value:body.suggestionIds())links.add(value.toString());
        require(links.contains(row.get("origin_suggestion_id")),"Keep the originating suggestion linked");
        for(String suggestion:links)require(jdbc.queryForObject("SELECT COUNT(*) FROM process_suggestion WHERE suggestion_id=? AND business_process_id=? AND decision='ACCEPTED'",Integer.class,suggestion,row.get("business_process_id"))==1,"Link only accepted suggestions for this process");
        Set<String> ticketKeys=new HashSet<>();for(Ticket ticket:body.tickets()){
            URI url;try{url=URI.create(ticket.url());}catch(IllegalArgumentException e){throw new IllegalArgumentException("Use an absolute HTTP or HTTPS ticket URL");}
            if(!Set.of("https","http").contains(Objects.toString(url.getScheme(),"").toLowerCase(Locale.ROOT))||url.getHost()==null||url.getUserInfo()!=null)throw new IllegalArgumentException("Use an absolute HTTP or HTTPS ticket URL without embedded credentials");
            require(ticketKeys.add((ticket.system().trim()+":"+ticket.number().trim()).toLowerCase(Locale.ROOT)),"Ticket is listed twice");
        }
        Map<String,Object> beforeRecord=new LinkedHashMap<>(row);
        beforeRecord.put("tickets",jdbc.queryForList("SELECT system_name,ticket_number,ticket_url FROM managed_change_ticket WHERE change_id=?",id));
        beforeRecord.put("suggestions",jdbc.queryForList("SELECT suggestion_id FROM managed_change_suggestion WHERE change_id=?",id));
        String before=encode(beforeRecord);
        jdbc.update("UPDATE managed_change SET title=?,implementation_owner_id=?,state=?,plan=?,evidence=?,planned_start=?,planned_finish=?,lock_version=lock_version+1 WHERE change_id=?",body.title(),body.ownerId(),body.state(),body.plan(),body.evidence(),body.plannedStart(),body.plannedFinish(),id);
        jdbc.update("DELETE FROM managed_change_ticket WHERE change_id=?",id);
        for(Ticket ticket:body.tickets())jdbc.update("INSERT INTO managed_change_ticket VALUES(?,?,?,?)",id,ticket.system().trim(),ticket.number().trim(),ticket.url());
        jdbc.update("DELETE FROM managed_change_suggestion WHERE change_id=?",id);for(String suggestion:links)jdbc.update("INSERT INTO managed_change_suggestion VALUES(?,?)",id,suggestion);
        jdbc.update("INSERT INTO managed_change_event(event_id,change_id,actor_id,reason,previous_value,new_value,command_payload) VALUES(?,?,?,?,?,?,?)",body.commandId().toString(),id,actor(),body.reason(),before,encode(body),encode(body));
        return read(id);
    }
}
