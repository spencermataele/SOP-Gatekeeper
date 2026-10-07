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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static com.woven.support.StructuredFixtures.details;
import static org.hamcrest.Matchers.not;

@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@TestPropertySource(properties = "app.lifecycle.api-enabled=true")
@Transactional
class LifecycleApiTest extends DatabaseTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;

    @BeforeEach void configureClient() {
        jdbc.update("INSERT INTO client_configuration VALUES (1, 1)");
        jdbc.update("INSERT INTO process_governance VALUES (1, 2, 0)");
        jdbc.update("INSERT INTO user_reporting_line VALUES (2, 3)");
    }

    private ResultActions call(int actor, MockHttpServletRequestBuilder request, Object body) throws Exception {
        String username = jdbc.queryForObject("SELECT username FROM users WHERE id = ?", String.class, actor);
        request.header("Authorization", "Bearer " + jwt.generateToken(username));
        if (body != null) request.contentType("application/json").content(json.writeValueAsBytes(body));
        return mvc.perform(request);
    }
    private JsonNode result(ResultActions action) throws Exception {
        return json.readTree(action.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
    }
    private JsonNode draft() throws Exception {
        return result(call(4, post("/api/lifecycle/documents"), Map.of("processId", 1, "title", "Tutorial test",
                "description", "Purpose", "details", details("Original steps"), "commandId", UUID.randomUUID())));
    }
    private String path(JsonNode draft) { return "/api/lifecycle/requests/" + draft.get("requestId").asLong(); }
    private long submit(JsonNode draft) throws Exception {
        return result(call(4, post(path(draft) + "/submit"), Map.of("copyId", draft.get("copyId").asLong(),
                "requestVersion", 0, "copyVersion", 0, "commandId", UUID.randomUUID()))).get("candidateId").asLong();
    }
    private Map<String, Object> candidate(long candidate, long version, String reason) {
        return Map.of("candidateId", candidate, "requestVersion", version, "reason", reason, "commandId", UUID.randomUUID());
    }
    private Map<String, Object> approval(long candidate, long version, String mode, String reason) {
        return Map.of("candidateId", candidate, "requestVersion", version, "mode", mode, "reason", reason, "commandId", UUID.randomUUID());
    }

    @Test void onlyAdminsCanConfigureOwnershipAndChangesAreAudited() throws Exception {
        var assignment = Map.of("ownerId", 2, "managerId", 3, "expectedVersion", 0,
                "expectedManagerId", 3, "reason", "Confirmed reporting line");
        call(4, get("/api/lifecycle/admin/setup"), null).andExpect(status().isForbidden());
        call(4, put("/api/lifecycle/admin/processes/1/ownership"), assignment).andExpect(status().isForbidden());
        call(1, put("/api/lifecycle/admin/processes/1/ownership"), assignment).andExpect(status().isOk())
                .andExpect(jsonPath("$.activity[0].reason").value("Confirmed reporting line"));
        assertEquals(1L, jdbc.queryForObject("SELECT lock_version FROM process_governance WHERE business_process_id=1", Long.class));
        call(1, put("/api/lifecycle/admin/processes/1/ownership"), assignment).andExpect(status().isConflict());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM governance_configuration_audit", Integer.class));
    }

    @Test void incompleteTemplateIsSavedButCannotEnterReview() throws Exception {
        String incomplete = details("Work").replace("Workstation", " ");
        JsonNode saved = result(call(4, post("/api/lifecycle/documents"), Map.of("processId", 1,
                "title", "Incomplete", "description", "Purpose", "details", incomplete, "commandId", UUID.randomUUID())));
        call(4, post(path(saved) + "/submit"), Map.of("copyId", saved.get("copyId").asLong(),
                "requestVersion", 0, "copyVersion", 0, "commandId", UUID.randomUUID())).andExpect(status().isBadRequest());
        assertEquals("DRAFT", jdbc.queryForObject("SELECT state FROM sop_work_item WHERE work_item_id=?", String.class, saved.get("requestId").asLong()));
    }

    @Test void submissionFreezesServerControlledHierarchy() throws Exception {
        JsonNode saved = draft();
        long revision = submit(saved);
        var content = json.readTree(jdbc.queryForObject("SELECT details FROM sop_revision WHERE revision_id=?", String.class, revision));
        assertFalse(content.path("context").path("process").asText().isBlank());
        assertEquals(jdbc.queryForObject("SELECT full_name FROM users WHERE id=2", String.class), content.path("context").path("processOwner").asText());
        assertEquals(2, jdbc.queryForObject("SELECT content_schema_version FROM sop_revision WHERE revision_id=?", Integer.class, revision));
    }

    @Test void workspaceListsRespectDraftPrivacyAndPublishedHistory() throws Exception {
        JsonNode draft = draft();
        call(4, get("/api/lifecycle/processes"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        call(4, get("/api/lifecycle/requests"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(draft.get("requestId").asLong()));
        call(1, get("/api/lifecycle/requests"), null).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        call(2, get("/api/lifecycle/requests"), null).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        long candidate = submit(draft);
        call(2, get("/api/lifecycle/requests"), null).andExpect(status().isOk()).andExpect(jsonPath("$[0].actions", hasItem("APPROVE")));
        JsonNode view = result(call(2, get(path(draft)), null));
        long document = view.get("documentId").asLong();
        call(4, get("/api/lifecycle/documents/" + document + "/history"), null).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        call(2, get("/api/lifecycle/notifications"), null).andExpect(status().isOk()).andExpect(jsonPath("$[0].requestId").value(draft.get("requestId").asLong()));
        call(3, get("/api/lifecycle/notifications"), null).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        call(2, post(path(draft) + "/approve"), approval(candidate, 1, "NORMAL", "Ready")).andExpect(status().isOk());
        call(3, get("/api/lifecycle/documents/" + document + "/history"), null).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(candidate));
        call(3, get("/api/lifecycle/documents"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].publishedVersion").value(1)).andExpect(jsonPath("$[0].nodeKey").value("process:1"));
    }

    @Test void libraryHierarchyIsClientScopedAndCodesRequireAuditedAdminChange() throws Exception {
        JsonNode nodes=result(call(4,get("/api/lifecycle/library/hierarchy"),null));
        assertTrue(nodes.size()>0);
        for(JsonNode node:nodes) assertFalse(node.path("nodeKey").asText().equals("org:2"));
        String old=jdbc.queryForObject("SELECT display_code FROM hierarchy_display_code WHERE node_key='process:1'",String.class);
        var body=Map.of("code","SETUP","expectedCode",old,"reason","Use meaningful process code");
        call(4,put("/api/lifecycle/admin/hierarchy/process:1/code"),body).andExpect(status().isForbidden());
        call(1,put("/api/lifecycle/admin/hierarchy/process:1/code"),body).andExpect(status().isOk());
        call(1,put("/api/lifecycle/admin/hierarchy/process:1/code"),body).andExpect(status().isConflict());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM governance_configuration_audit WHERE action='HIERARCHY_CODE_CHANGED'",Integer.class));
    }

    @Test void adminCreatesFamilyUnderRequiredSubdepartmentAndProcessInheritsIt() throws Exception {
        var parent=jdbc.queryForMap("SELECT dept_subgroup_id AS subgroup,department_id AS department FROM business_process_family WHERE business_process_family_id=1");
        int subgroup=((Number)parent.get("subgroup")).intValue();
        int department=((Number)parent.get("department")).intValue();
        call(4,get("/admin/hierarchy"),null).andExpect(status().isForbidden());
        call(1,post("/admin/business-process-families"),Map.of("businessProcessFamilyName","Missing parent")).andExpect(status().isBadRequest());
        JsonNode family=result(call(1,post("/admin/business-process-families"),Map.of("businessProcessFamilyName","Hierarchy test","deptSubgroupId",subgroup)));
        assertEquals(subgroup,family.get("deptSubgroupId").asInt());
        assertEquals(department,family.get("departmentId").asInt());
        JsonNode process=result(call(1,post("/admin/business-processes"),Map.of("businessProcessName","Test process","businessProcessFamilyId",family.get("businessProcessFamilyId").asInt(),"departmentId",department)));
        assertEquals(subgroup,process.get("deptSubgroupIds").get(0).asInt());
        call(1,post("/admin/business-processes"),Map.of("businessProcessName","Invalid branch","businessProcessFamilyId",family.get("businessProcessFamilyId").asInt(),"departmentId",999999)).andExpect(status().isBadRequest());
        JsonNode hierarchy=result(call(1,get("/admin/hierarchy"),null));
        String key="family:"+family.get("businessProcessFamilyId").asInt();
        assertTrue(java.util.stream.StreamSupport.stream(hierarchy.spliterator(),false).anyMatch(n->n.path("nodeKey").asText().equals(key)&&n.path("parentKey").asText().equals("subgroup:"+subgroup)));
    }

    @Test void requestDetailsDoNotExposeAnotherReviewersPrivateCopyOrActivity() throws Exception {
        JsonNode draft = draft();
        long candidate = submit(draft);
        JsonNode copy = result(call(2, post(path(draft) + "/reviewer-copies"), candidate(candidate, 1, "")));
        call(2, put(path(draft) + "/copies/" + copy.get("copyId").asLong()), Map.of("requestVersion", 1, "copyVersion", 0,
                "title", "Private title", "description", "Private purpose", "details", details("Private steps"), "commandId", UUID.randomUUID())).andExpect(status().isOk());
        JsonNode authorView = result(call(4, get(path(draft)), null));
        assertFalse(authorView.toString().contains("Private title"));
        for (JsonNode owned : authorView.get("ownCopies")) assertNotEquals(copy.get("copyId"), owned.get("copyId"));
        for (JsonNode event : authorView.get("activity")) assertNotEquals("REVIEW_COPY_CREATED", event.get("action").asText());
        call(2, get(path(draft)), null).andExpect(jsonPath("ownCopies[0].copyId").value(copy.get("copyId").asLong()));
    }

    @Test void governedDeploymentDisablesLegacyWorkflowControllers() throws Exception {
        call(1, get("/sops"), null).andExpect(status().isNotFound());
        call(1, get("/change-requests"), null).andExpect(status().isNotFound());
    }

    @Test void authorReviewerEditorAndManagerCompleteWorkflowThroughHttp() throws Exception {
        JsonNode draft = draft();
        long first = submit(draft);
        JsonNode edited = result(call(2, post(path(draft) + "/reviewer-copies"), candidate(first, 1, "")));
        long copy = edited.get("copyId").asLong();
        call(2, put(path(draft) + "/copies/" + copy), Map.of("requestVersion", 1, "copyVersion", 0,
                "title", "Tutorial test", "description", "Purpose", "details", details("Corrected steps"), "commandId", UUID.randomUUID()))
                .andExpect(status().isOk()).andExpect(jsonPath("version").value(1));
        call(4, get(path(draft)), null).andExpect(jsonPath("candidate.details").value(containsString("Original steps")));
        call(4, get(path(draft) + "/copies/" + copy), null).andExpect(status().isForbidden());
        long second = result(call(2, post(path(draft) + "/submit"), Map.of("copyId", copy, "requestVersion", 1,
                "copyVersion", 1, "reason", "Corrected steps", "commandId", UUID.randomUUID()))).get("candidateId").asLong();
        call(2, get(path(draft)), null).andExpect(jsonPath("actions", hasItem("SELF_APPROVE")))
                .andExpect(jsonPath("actions", not(hasItem("APPROVE"))));
        call(4, get(path(draft)), null).andExpect(jsonPath("actions", not(hasItem("SELF_APPROVE"))));
        call(4, post(path(draft) + "/approve"), approval(second, 2, "SELF_APPROVAL", "Authored this" )).andExpect(status().isForbidden());
        call(2, post(path(draft) + "/approve"), approval(second, 2, "SELF_APPROVAL", " " )).andExpect(status().isBadRequest());
        call(2, post(path(draft) + "/approve"), approval(second, 2, "NORMAL", "" )).andExpect(status().isForbidden());
        call(3, post(path(draft) + "/approve"), approval(second, 2, "NORMAL", "Verified" )).andExpect(status().isOk());
        call(4, get("/api/lifecycle/documents"), null).andExpect(jsonPath("$[0].details").value(containsString("Corrected steps")));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM sop_revision", Integer.class));
    }

    @Test void createAndSaveRetriesReturnSameCopyAndVersion() throws Exception {
        var body = Map.of("processId", 1, "title", "", "description", "", "details", "", "commandId", UUID.randomUUID());
        JsonNode first = result(call(4, post("/api/lifecycle/documents"), body));
        assertEquals(first, result(call(4, post("/api/lifecycle/documents"), body)));
        var save = Map.of("requestVersion", 0, "copyVersion", 0, "title", "Draft", "description", "",
                "details", "", "commandId", UUID.randomUUID());
        JsonNode saved = result(call(4, put(path(first) + "/copies/" + first.get("copyId").asLong()), save));
        assertEquals(saved, result(call(4, put(path(first) + "/copies/" + first.get("copyId").asLong()), save)));
        call(4, post(path(first) + "/submit"), Map.of("copyId", first.get("copyId").asLong(), "requestVersion", 0,
                "copyVersion", 1, "commandId", UUID.randomUUID())).andExpect(status().isBadRequest());
    }

    @Test void staleSaveCannotOverwriteAnotherSave() throws Exception {
        JsonNode draft = draft();
        var body = Map.of("requestVersion", 0, "copyVersion", 0, "title", "Saved title", "description", "Purpose",
                "details", details("New steps"), "commandId", UUID.randomUUID());
        call(4, put(path(draft) + "/copies/" + draft.get("copyId").asLong()), body).andExpect(status().isOk());
        call(4, put(path(draft) + "/copies/" + draft.get("copyId").asLong()), Map.of("requestVersion", 0, "copyVersion", 0,
                "title", "Stale title", "description", "Purpose", "details", details("Old steps"), "commandId", UUID.randomUUID()))
                .andExpect(status().isConflict());
        call(4, get(path(draft) + "/copies/" + draft.get("copyId").asLong()), null).andExpect(jsonPath("title").value("Saved title"));
    }

    @Test void adminCannotReadOrEditAnotherAuthorsPrivateDraft() throws Exception {
        JsonNode draft = draft();
        call(1, get(path(draft)), null).andExpect(status().isForbidden());
        call(1, put(path(draft) + "/copies/" + draft.get("copyId").asLong()), Map.of("requestVersion", 0, "copyVersion", 0,
                "title", "Override", "description", "Purpose", "details", details("Steps"), "actorId", 4, "commandId", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test void rejectionPreservesCandidateAndCannotBePublished() throws Exception {
        JsonNode draft = draft();
        long candidate = submit(draft);
        var body = candidate(candidate, 1, "Please clarify step two");
        call(2, post(path(draft) + "/reject"), body).andExpect(status().isOk());
        call(2, post(path(draft) + "/reject"), body).andExpect(status().isOk());
        call(4, get(path(draft)), null).andExpect(jsonPath("state").value("REJECTED"))
                .andExpect(jsonPath("candidate.details").value(containsString("Original steps")));
        call(2, post(path(draft) + "/approve"), approval(candidate, 2, "NORMAL", "")).andExpect(status().isConflict());
        assertEquals("REJECTED", jdbc.queryForObject("SELECT decision FROM approval_decision", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM publication_record", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM notification_event", Integer.class));
        long decision = jdbc.queryForObject("SELECT decision_id FROM approval_decision", Long.class);
        long document = jdbc.queryForObject("SELECT document_id FROM sop_work_item WHERE work_item_id = ?", Long.class, draft.get("requestId").asLong());
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update(
                "INSERT INTO publication_record (document_id, revision_id, decision_id) VALUES (?, ?, ?)", document, candidate, decision));
    }

    @Test void cancellationIsAuthorOnlyAndDraftOnly() throws Exception {
        JsonNode draft = draft();
        var body = Map.of("requestVersion", 0, "commandId", UUID.randomUUID());
        call(1, post(path(draft) + "/cancel"), body).andExpect(status().isForbidden());
        call(4, post(path(draft) + "/cancel"), body).andExpect(status().isOk());
        call(4, post(path(draft) + "/cancel"), body).andExpect(status().isOk());
        call(4, get(path(draft)), null).andExpect(jsonPath("state").value("CANCELLED"));
        call(4, post(path(draft) + "/submit"), Map.of("copyId", draft.get("copyId").asLong(), "requestVersion", 1,
                "copyVersion", 1, "commandId", UUID.randomUUID())).andExpect(status().isConflict());
    }

    @Test void cannotCancelInReviewOrRejectWithoutReason() throws Exception {
        JsonNode draft = draft();
        long candidate = submit(draft);
        call(4, post(path(draft) + "/cancel"), Map.of("requestVersion", 1, "commandId", UUID.randomUUID())).andExpect(status().isConflict());
        call(2, post(path(draft) + "/reject"), candidate(candidate, 1, " ")).andExpect(status().isBadRequest());
        call(4, get(path(draft)), null).andExpect(jsonPath("state").value("IN_REVIEW"));
    }

    @Test void openingReviewCopyDoesNotBlockPublicationAndLaterSaveConflicts() throws Exception {
        JsonNode draft = draft();
        long candidate = submit(draft);
        var command = candidate(candidate, 1, "");
        JsonNode copy = result(call(2, post(path(draft) + "/reviewer-copies"), command));
        assertEquals(copy, result(call(2, post(path(draft) + "/reviewer-copies"), command)));
        call(2, post(path(draft) + "/approve"), approval(candidate, 1, "NORMAL", "Verified")).andExpect(status().isOk());
        call(2, put(path(draft) + "/copies/" + copy.get("copyId").asLong()), Map.of("requestVersion", 1, "copyVersion", 0,
                "title", "Too late", "description", "Purpose", "details", details("Steps"), "commandId", UUID.randomUUID()))
                .andExpect(status().isConflict());
    }

    @Test void ownerCanStartRevisionButOtherUsersNeedAcceptedSuggestion() throws Exception {
        JsonNode draft = draft();
        long candidate = submit(draft);
        call(2, post(path(draft) + "/approve"), approval(candidate, 1, "NORMAL", "Verified")).andExpect(status().isOk());
        JsonNode published = result(call(3, get("/api/lifecycle/documents"), null));
        long document = published.get(0).get("documentId").asLong();
        var command = Map.of("publishedRevisionId", candidate, "commandId", UUID.randomUUID(),"rationale","Owner identified necessary correction");
        call(3, post("/api/lifecycle/documents/" + document + "/drafts"), command).andExpect(status().isForbidden());
        call(1, post("/api/lifecycle/documents/" + document + "/drafts"), command).andExpect(status().isForbidden());
        JsonNode revision = result(call(2, post("/api/lifecycle/documents/" + document + "/drafts"), command));
        assertEquals(revision, result(call(2, post("/api/lifecycle/documents/" + document + "/drafts"), command)));
        call(2, get(path(revision) + "/copies/" + revision.get("copyId").asLong()), null)
                .andExpect(jsonPath("details").value(containsString("Original steps")));
    }

    @Test void unassignedReviewerCannotCreateCopy() throws Exception {
        JsonNode draft = draft();
        long candidate = submit(draft);
        call(3, post(path(draft) + "/reviewer-copies"), candidate(candidate, 1, "")).andExpect(status().isForbidden());
    }

    @Test void anonymousRequestsAndMissingVersionsAreRejected() throws Exception {
        mvc.perform(get("/api/lifecycle/documents")).andExpect(status().is4xxClientError());
        JsonNode draft = draft();
        call(4, post(path(draft) + "/submit"), Map.of("copyId", draft.get("copyId").asLong(), "commandId", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    @Test void rejectedAttemptResubmitsWithAllEarlierContributorsAndKeepsOldDecision() throws Exception {
        JsonNode original = draft();
        long first = submit(original);
        JsonNode ownerCopy = result(call(2, post(path(original) + "/reviewer-copies"), candidate(first, 1, "")));
        long edited = result(call(2, post(path(original) + "/submit"), Map.of("copyId", ownerCopy.get("copyId").asLong(),
                "requestVersion", 1, "copyVersion", 0, "reason", "Owner contribution", "commandId", UUID.randomUUID())))
                .get("candidateId").asLong();
        call(3, post(path(original) + "/reject"), candidate(edited, 2, "Clarify this version")).andExpect(status().isOk());
        var revise = Map.of("requestVersion", 3, "commandId", UUID.randomUUID());
        JsonNode newAttempt = result(call(4, post(path(original) + "/revise"), revise));
        assertEquals(newAttempt, result(call(4, post(path(original) + "/revise"), revise)));
        call(4, get(path(newAttempt)), null).andExpect(jsonPath("priorRejectedRequestId").value(original.get("requestId").asLong()));
        long submitted = submit(newAttempt);
        assertEquals(java.util.Set.of(2, 4), java.util.Set.copyOf(jdbc.queryForList(
                "SELECT DISTINCT user_id FROM revision_participant WHERE revision_id = ?", Integer.class, submitted)));
        call(2, post(path(newAttempt) + "/approve"), approval(submitted, 1, "NORMAL", "")).andExpect(status().isForbidden());
        call(2, post(path(newAttempt) + "/approve"), approval(submitted, 1, "SELF_APPROVAL", "Verified revised procedure"))
                .andExpect(status().isOk());
        call(4, get(path(original)), null).andExpect(jsonPath("state").value("REJECTED"));
        assertEquals("REJECTED", jdbc.queryForObject("SELECT decision FROM approval_decision WHERE revision_id = ?", String.class, edited));
    }

    @Test void onlyOriginalAuthorCanReviseAndPublishedChangesRequireReconciliation() throws Exception {
        JsonNode rejected = draft();
        long candidate = submit(rejected);
        call(2, post(path(rejected) + "/reject"), candidate(candidate, 1, "Needs work")).andExpect(status().isOk());
        call(1, post(path(rejected) + "/revise"), Map.of("requestVersion", 2, "commandId", UUID.randomUUID()))
                .andExpect(status().isForbidden());
        long document = jdbc.queryForObject("SELECT document_id FROM sop_work_item WHERE work_item_id = ?", Long.class, rejected.get("requestId").asLong());
        // Simulate another published baseline; the service must not silently stamp it onto old rejected content.
        jdbc.update("UPDATE sop_document SET current_revision_id = ? WHERE document_id = ?", candidate, document);
        call(4, post(path(rejected) + "/revise"), Map.of("requestVersion", 2, "commandId", UUID.randomUUID()))
                .andExpect(status().isConflict());
    }

    @Test void administratorReassignmentArchivesOldOwnerAndEnablesNewReviewer() throws Exception {
        JsonNode draft = draft();
        long candidate = submit(draft);
        jdbc.update("UPDATE process_governance SET owner_user_id = 3, lock_version = 1 WHERE business_process_id = 1");
        call(1, get(path(draft)), null).andExpect(jsonPath("actions", hasItem("REASSIGN")));
        call(2, post(path(draft) + "/reassign"), candidate(candidate, 1, "New owner")).andExpect(status().isForbidden());
        call(1, post(path(draft) + "/reassign"), candidate(candidate, 1, " ")).andExpect(status().isBadRequest());
        var body = candidate(candidate, 1, "New accountable owner appointed");
        call(1, post(path(draft) + "/reassign"), body).andExpect(status().isOk());
        call(1, post(path(draft) + "/reassign"), body).andExpect(status().isOk());
        assertEquals(java.util.List.of(2), jdbc.queryForList("SELECT reviewer_id FROM review_assignment_history", Integer.class));
        assertEquals(java.util.List.of(3), jdbc.queryForList("SELECT reviewer_id FROM review_assignment", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM review_routing_change", Integer.class));
        call(2, post(path(draft) + "/approve"), approval(candidate, 2, "NORMAL", "")).andExpect(status().isForbidden());
        call(3, post(path(draft) + "/approve"), approval(candidate, 2, "NORMAL", "Verified")).andExpect(status().isOk());
    }

    @Test void unchangedOrCompletedReviewCannotBeReassigned() throws Exception {
        JsonNode draft = draft();
        long candidate = submit(draft);
        call(1, post(path(draft) + "/reassign"), candidate(candidate, 1, "Unnecessary")).andExpect(status().isConflict());
        call(2, post(path(draft) + "/approve"), approval(candidate, 1, "NORMAL", "Verified")).andExpect(status().isOk());
        jdbc.update("UPDATE process_governance SET owner_user_id = 3, lock_version = 1 WHERE business_process_id = 1");
        call(1, post(path(draft) + "/reassign"), candidate(candidate, 2, "Too late")).andExpect(status().isConflict());
    }
}
