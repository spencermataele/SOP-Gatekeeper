package com.woven.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woven.app.service.userAuth.JwtService;
import com.woven.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.woven.support.StructuredFixtures.details;

@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@TestPropertySource(properties="app.lifecycle.api-enabled=true")
@Transactional
class SuggestionApiTest extends DatabaseTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc; @Autowired JwtService jwt;
 @BeforeEach void setup(){jdbc.update("INSERT INTO client_configuration VALUES(1,1)");jdbc.update("INSERT INTO process_governance VALUES(1,2,0)");jdbc.update("INSERT INTO user_reporting_line VALUES(2,3)");}
 ResultActions call(int actor,MockHttpServletRequestBuilder request,Object body)throws Exception{
  request.header("Authorization","Bearer "+jwt.generateToken(jdbc.queryForObject("SELECT username FROM users WHERE id=?",String.class,actor)));
  if(body!=null)request.contentType("application/json").content(json.writeValueAsBytes(body));return mvc.perform(request);
 }
 JsonNode ok(ResultActions result)throws Exception{return json.readTree(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());}
 Map<String,Object> submission(){return new HashMap<>(Map.of("processId",1,"title","Reduce errors","problem","Labels are ambiguous","proposal","Use clear labels","benefit","Fewer errors","commandId",UUID.randomUUID()));}
 JsonNode create()throws Exception{return ok(call(4,post("/api/lifecycle/suggestions"),submission()));}
 String path(JsonNode s){return "/api/lifecycle/suggestions/"+s.get("suggestion_id").asText();}
 Map<String,Object> action(JsonNode s,String action){return new HashMap<>(Map.of("version",s.get("lock_version").asLong(),"action",action,"message","Explained to the submitter","concern","DEFECT","declineReason","ALTERNATIVE_SELECTED","plan","Replace labels and train operators","evidence","Observed correct execution across ten shifts","reviewDate",LocalDate.now().toString(),"commandId",UUID.randomUUID()));}
 JsonNode act(int actor,JsonNode s,String action)throws Exception{return ok(call(actor,post(path(s)+"/actions"),action(s,action)));}

 @Test void validatesRequiredFieldsAndRecordsRecipientsWithoutClaimingAcknowledgment()throws Exception{
  var body=submission();body.put("problem"," ");call(4,post("/api/lifecycle/suggestions"),body).andExpect(status().isBadRequest());
  body=submission();var s=ok(call(4,post("/api/lifecycle/suggestions"),body));
  assertEquals("SUBMITTED",s.get("state").asText());assertEquals(4,s.get("recipients").size());
  assertEquals("AVAILABLE_IN_APP",s.get("recipients").get(0).get("delivery_status").asText());
  assertEquals(s,ok(call(4,post("/api/lifecycle/suggestions"),body)));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM process_suggestion",Integer.class));
  call(4,get("/api/lifecycle/notifications"),null).andExpect(jsonPath("$[0].suggestionId").value(s.get("suggestion_id").asText()));
 }
 @Test void declineNeedsReasonAndCannotCloseWithoutSubmitterAndVerifiedFollowup()throws Exception{
  var s=create();var invalid=action(s,"DECLINE");invalid.remove("declineReason");
  call(2,post(path(s)+"/actions"),invalid).andExpect(status().isBadRequest());
  s=act(2,s,"DECLINE");assertEquals("DECLINED_AWAITING_RESPONSE",s.get("state").asText());
  call(2,post(path(s)+"/actions"),action(s,"AGREE_NEXT_STEPS")).andExpect(status().isForbidden());
  call(2,post(path(s)+"/actions"),action(s,"PROPOSE_CLOSURE")).andExpect(status().isConflict());
  s=act(4,s,"AGREE_NEXT_STEPS");assertEquals("FOLLOW_UP_REQUIRED",s.get("state").asText());
  var missing=action(s,"PROPOSE_CLOSURE");missing.put("evidence","");call(2,post(path(s)+"/actions"),missing).andExpect(status().isBadRequest());
  s=act(2,s,"PROPOSE_CLOSURE");assertEquals("AWAITING_AGREEMENT",s.get("state").asText());
  call(2,post(path(s)+"/actions"),action(s,"AGREE_RESOLUTION")).andExpect(status().isForbidden());
  s=act(4,s,"AGREE_RESOLUTION");assertEquals("CLOSED",s.get("state").asText());
  assertEquals("VERIFIED",s.get("action_state").asText());
  s=act(4,s,"REOPEN");assertEquals("ALIGNMENT_UNRESOLVED",s.get("state").asText());assertEquals("DEFECT",s.get("concern").asText());
 }
 @Test void challengeEscalatesWithoutForcingAgreementOrHidingDefect()throws Exception{
  var s=act(2,create(),"DECLINE");s=act(4,s,"CHALLENGE");
  call(2,post(path(s)+"/actions"),action(s,"ESCALATION_RESPONSE")).andExpect(status().isForbidden());
  s=act(3,s,"ESCALATION_RESPONSE");assertEquals("ALIGNMENT_UNRESOLVED",s.get("state").asText());
  var downgrade=action(s,"ACCEPT");downgrade.put("concern","NONE");call(2,post(path(s)+"/actions"),downgrade).andExpect(status().isConflict());
  s=act(2,s,"ACCEPT");assertEquals("ACCEPTED",s.get("state").asText());
 }
 @Test void changedPlansInvalidatePendingClosureAndContainmentCannotClose()throws Exception{
  var s=act(2,create(),"ACCEPT");s=act(2,s,"PROPOSE_CLOSURE");s=act(2,s,"CONTAIN");
  call(4,post(path(s)+"/actions"),action(s,"AGREE_RESOLUTION")).andExpect(status().isConflict());
  call(2,post(path(s)+"/actions"),action(s,"PROPOSE_CLOSURE")).andExpect(status().isConflict());
  s=act(2,s,"PLAN");var future=action(s,"PROPOSE_CLOSURE");future.put("reviewDate",LocalDate.now().plusDays(1).toString());
  call(2,post(path(s)+"/actions"),future).andExpect(status().isConflict());
 }
 @Test void onlyOwnerDecidesAndCommandsAreIdempotentAndVersioned()throws Exception{
  var s=create();var command=action(s,"ACCEPT");
  call(1,post(path(s)+"/actions"),command).andExpect(status().isForbidden());
  call(4,post(path(s)+"/actions"),command).andExpect(status().isForbidden());
  var accepted=ok(call(2,post(path(s)+"/actions"),command));assertEquals(accepted,ok(call(2,post(path(s)+"/actions"),command)));
  call(2,post(path(s)+"/actions"),action(s,"COMMENT")).andExpect(status().isConflict());
  command.put("message","Different input");call(2,post(path(s)+"/actions"),command).andExpect(status().isConflict());
 }
 @Test void unrelatedUsersCannotReadAndClientBoundaryIsEnforced()throws Exception{
  var s=create();jdbc.update("DELETE FROM user_reporting_line WHERE user_id=2");
  call(3,get(path(s)),null).andExpect(status().isForbidden());
  call(3,get("/api/lifecycle/suggestions"),null).andExpect(jsonPath("$.length()").value(0));
  jdbc.update("INSERT INTO org(org_id,org_name) VALUES(2,'Other client')");
  jdbc.update("UPDATE client_configuration SET org_id=2 WHERE configuration_id=1");
  call(4,get(path(s)),null).andExpect(status().isNotFound());
 }
 @Test void acceptedSuggestionEnablesRevisionAndPublicationDoesNotResolveConcern()throws Exception{
  var draft=ok(call(4,post("/api/lifecycle/documents"),Map.of("processId",1,"title","Test","description","Purpose","details",details("Step"),"commandId",UUID.randomUUID())));
  String work="/api/lifecycle/requests/"+draft.get("requestId").asLong();
  long candidate=ok(call(4,post(work+"/submit"),Map.of("copyId",draft.get("copyId").asLong(),"requestVersion",0,"copyVersion",0,"commandId",UUID.randomUUID()))).get("candidateId").asLong();
  call(2,post(work+"/approve"),Map.of("candidateId",candidate,"requestVersion",1,"mode","NORMAL","commandId",UUID.randomUUID())).andExpect(status().isOk());
  long document=jdbc.queryForObject("SELECT document_id FROM sop_work_item WHERE work_item_id=?",Long.class,draft.get("requestId").asLong());
  var s=create();var request=new HashMap<String,Object>(Map.of("publishedRevisionId",candidate,"suggestionId",s.get("suggestion_id").asText(),"rationale","Implement clearer labels","commandId",UUID.randomUUID()));
  call(4,post("/api/lifecycle/documents/"+document+"/drafts"),request).andExpect(status().isConflict());
  s=act(2,s,"ACCEPT");var revision=ok(call(4,post("/api/lifecycle/documents/"+document+"/drafts"),request));
  assertEquals(revision,ok(call(4,post("/api/lifecycle/documents/"+document+"/drafts"),request)));
  assertEquals(1,ok(call(4,get(path(s)),null)).get("revisions").size());
  String revisionPath="/api/lifecycle/requests/"+revision.get("requestId").asLong();
  long revised=ok(call(4,post(revisionPath+"/submit"),Map.of("copyId",revision.get("copyId").asLong(),"requestVersion",0,"copyVersion",0,"commandId",UUID.randomUUID()))).get("candidateId").asLong();
  call(2,post(revisionPath+"/approve"),Map.of("candidateId",revised,"requestVersion",1,"mode","NORMAL","commandId",UUID.randomUUID())).andExpect(status().isOk());
  assertEquals("ACCEPTED",ok(call(4,get(path(s)),null)).get("state").asText());
 }
 String changePath(JsonNode s){return "/api/lifecycle/changes/"+s.get("changes").get(0).get("id").asText();}
 Map<String,Object> changeBody(JsonNode record){
  return new HashMap<>(Map.of("version",record.get("lock_version").asLong(),"commandId",UUID.randomUUID(),"reason","Progress confirmed","title",record.get("title").asText(),"ownerId",2,"state","IN_PROGRESS","plan","Implement the accepted solution","tickets",List.of(Map.of("system","Jira","number","OPS-123","url","https://example.atlassian.net/browse/OPS-123")),"suggestionIds",List.of(record.get("origin_suggestion_id").asText())));
 }
 @Test void acceptanceCreatesOneChangeAndSubmitterCanReadButNotEditIt()throws Exception{
  var suggestion=create();var command=action(suggestion,"ACCEPT");
  var accepted=ok(call(2,post(path(suggestion)+"/actions"),command));
  ok(call(2,post(path(suggestion)+"/actions"),command));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM managed_change",Integer.class));
  String path=changePath(accepted);var owner=ok(call(2,get(path),null));
  assertTrue(owner.get("canEdit").asBoolean());assertFalse(ok(call(4,get(path),null)).get("canEdit").asBoolean());
  call(4,put(path),changeBody(owner)).andExpect(status().isForbidden());
  call(1,put(path),changeBody(owner)).andExpect(status().isForbidden());
  call(4,get("/api/lifecycle/changes"),null).andExpect(jsonPath("$.length()").value(1));
 }
 @Test void ticketUpdatesAreVersionedIdempotentAndRejectUnsafeLinks()throws Exception{
  String path=changePath(act(2,create(),"ACCEPT"));var change=ok(call(2,get(path),null));var body=changeBody(change);
  body.put("tickets",List.of(Map.of("system","Jira","number","OPS-123","url","javascript:alert(1)")));
  call(2,put(path),body).andExpect(status().isBadRequest());
  body=changeBody(change);var saved=ok(call(2,put(path),body));
  assertEquals("OPS-123",saved.get("tickets").get(0).get("number").asText());
  assertEquals(saved,ok(call(2,put(path),body)));
  call(2,put(path),changeBody(change)).andExpect(status().isConflict());
  body.put("reason","Altered command");call(2,put(path),body).andExpect(status().isConflict());
 }
 @Test void changeStagesRequireEvidenceAndDoNotCloseSuggestions()throws Exception{
  var suggestion=act(2,create(),"ACCEPT");String path=changePath(suggestion);var change=ok(call(2,get(path),null));var body=changeBody(change);
  body.put("state","EFFECTIVENESS_VERIFIED");call(2,put(path),body).andExpect(status().isConflict());
  for(String state:List.of("IN_PROGRESS","READY_FOR_VALIDATION","IMPLEMENTED","EFFECTIVENESS_VERIFIED")){
   body=changeBody(change);body.put("state",state);
   if(state.equals("IMPLEMENTED"))call(2,put(path),body).andExpect(status().isConflict());
   body.put("evidence","Implementation inspected and performance verified");change=ok(call(2,put(path),body));
  }
  assertEquals("ACCEPTED",ok(call(4,get(path(suggestion)),null)).get("state").asText());
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM approval_decision",Integer.class));
 }
 @Test void ownerCanAssignImplementationAndLinkMultipleAcceptedSuggestions()throws Exception{
  var first=act(2,create(),"ACCEPT");var second=act(2,create(),"ACCEPT");String path=changePath(first);var record=ok(call(2,get(path),null));var body=changeBody(record);
  body.put("suggestionIds",List.of(first.get("suggestion_id").asText(),second.get("suggestion_id").asText()));body.put("ownerId",4);
  var saved=ok(call(2,put(path),body));assertEquals(2,saved.get("suggestions").size());
  assertTrue(ok(call(4,get(path),null)).get("canEdit").asBoolean());
  body=changeBody(saved);body.put("ownerId",3);call(4,put(path),body).andExpect(status().isForbidden());
 }
}
