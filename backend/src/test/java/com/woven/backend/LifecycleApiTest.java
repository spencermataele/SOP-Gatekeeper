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
                "description", "Purpose", "details", "Original steps", "commandId", UUID.randomUUID())));
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

    @Test void authorReviewerEditorAndManagerCompleteWorkflowThroughHttp() throws Exception {
        JsonNode draft = draft();
        long first = submit(draft);
        JsonNode edited = result(call(2, post(path(draft) + "/reviewer-copies"), candidate(first, 1, "")));
        long copy = edited.get("copyId").asLong();
        call(2, put(path(draft) + "/copies/" + copy), Map.of("requestVersion", 1, "copyVersion", 0,
                "title", "Tutorial test", "description", "Purpose", "details", "Corrected steps", "commandId", UUID.randomUUID()))
                .andExpect(status().isOk()).andExpect(jsonPath("version").value(1));
        call(4, get(path(draft)), null).andExpect(jsonPath("candidate.details").value("Original steps"));
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
        call(4, get("/api/lifecycle/documents"), null).andExpect(jsonPath("$[0].details").value("Corrected steps"));
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
                "details", "New steps", "commandId", UUID.randomUUID());
        call(4, put(path(draft) + "/copies/" + draft.get("copyId").asLong()), body).andExpect(status().isOk());
        call(4, put(path(draft) + "/copies/" + draft.get("copyId").asLong()), Map.of("requestVersion", 0, "copyVersion", 0,
                "title", "Stale title", "description", "Purpose", "details", "Old steps", "commandId", UUID.randomUUID()))
                .andExpect(status().isConflict());
        call(4, get(path(draft) + "/copies/" + draft.get("copyId").asLong()), null).andExpect(jsonPath("title").value("Saved title"));
    }

    @Test void adminCannotReadOrEditAnotherAuthorsPrivateDraft() throws Exception {
        JsonNode draft = draft();
        call(1, get(path(draft)), null).andExpect(status().isForbidden());
        call(1, put(path(draft) + "/copies/" + draft.get("copyId").asLong()), Map.of("requestVersion", 0, "copyVersion", 0,
                "title", "Override", "description", "Purpose", "details", "Steps", "actorId", 4, "commandId", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test void rejectionPreservesCandidateAndCannotBePublished() throws Exception {
        JsonNode draft = draft();
        long candidate = submit(draft);
        var body = candidate(candidate, 1, "Please clarify step two");
        call(2, post(path(draft) + "/reject"), body).andExpect(status().isOk());
        call(2, post(path(draft) + "/reject"), body).andExpect(status().isOk());
        call(4, get(path(draft)), null).andExpect(jsonPath("state").value("REJECTED"))
                .andExpect(jsonPath("candidate.details").value("Original steps"));
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
                "title", "Too late", "description", "Purpose", "details", "Steps", "commandId", UUID.randomUUID()))
                .andExpect(status().isConflict());
    }

    @Test void anyClientUserCanStartNewDraftFromPublishedSop() throws Exception {
        JsonNode draft = draft();
        long candidate = submit(draft);
        call(2, post(path(draft) + "/approve"), approval(candidate, 1, "NORMAL", "Verified")).andExpect(status().isOk());
        JsonNode published = result(call(3, get("/api/lifecycle/documents"), null));
        long document = published.get(0).get("documentId").asLong();
        var command = Map.of("publishedRevisionId", candidate, "commandId", UUID.randomUUID());
        JsonNode revision = result(call(3, post("/api/lifecycle/documents/" + document + "/drafts"), command));
        assertEquals(revision, result(call(3, post("/api/lifecycle/documents/" + document + "/drafts"), command)));
        call(3, get(path(revision) + "/copies/" + revision.get("copyId").asLong()), null)
                .andExpect(jsonPath("details").value("Original steps"));
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
}
