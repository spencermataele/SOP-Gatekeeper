package com.woven.app.service.governance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woven.app.repository.RevisionSnapshotStore;
import com.woven.app.service.user.AppUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Governed commands shared by the opt-in lifecycle API; legacy MVP routes remain separate. */
@Service
@Transactional
public class LifecycleWorkflowService {
    private final JdbcTemplate jdbc;
    private final RevisionSnapshotStore snapshots;
    private final ObjectMapper json;
    private final ApprovalPolicy policy = new ApprovalPolicy();

    public LifecycleWorkflowService(JdbcTemplate jdbc, RevisionSnapshotStore snapshots, ObjectMapper json) {
        this.jdbc = jdbc;
        this.snapshots = snapshots;
        this.json = json;
    }

    private record Work(long id, long document, int process, Long published, Long base, Long candidate,
                        int author, String state, long version, String routing) {}
    private record Governance(int owner, Integer manager, Set<Integer> admins, String fingerprint) {}

    public record CopyResult(long requestId, long copyId, long version) {}

    public CopyResult createSop(int processId, String title, String description, String details, UUID commandId) {
        int actor = actor();
        validateDraftContent(title, description, details);
        // Serialize creation/retries for this process; no existing document lock is acquired here.
        jdbc.queryForMap("SELECT * FROM process_governance WHERE business_process_id = ? FOR UPDATE", processId);
        int org = clientScope(processId);
        requireUser(actor);
        String fingerprint = fingerprint("CREATE_SOP", processId, title, description, details);
        var saved = replayCreation(commandId, actor, fingerprint);
        if (saved != null) return copyResult(saved);
        jdbc.update("INSERT INTO sop_document (org_id, business_process_id) VALUES (?, ?)", org, processId);
        long document = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return createDraftRows(document, processId, null, actor, title, description, details, 1, commandId, fingerprint);
    }

    public CopyResult startRevision(long documentId, long expectedPublishedRevision, UUID commandId) {
        int actor = actor();
        var document = jdbc.queryForMap("SELECT * FROM sop_document WHERE document_id = ? FOR UPDATE", documentId);
        int process = validateDocument(document);
        requireUser(actor);
        jdbc.queryForMap("SELECT * FROM process_governance WHERE business_process_id = ? FOR SHARE", process);
        String fingerprint = fingerprint("START_REVISION", documentId, expectedPublishedRevision);
        var saved = replayCreation(commandId, actor, fingerprint);
        if (saved != null) return copyResult(saved);
        require(Objects.equals(number(document.get("current_revision_id")), expectedPublishedRevision), "Published revision changed");
        var revision = jdbc.queryForMap("SELECT * FROM sop_revision WHERE document_id = ? AND revision_id = ?", documentId, expectedPublishedRevision);
        return createDraftRows(documentId, process, expectedPublishedRevision, actor, (String) revision.get("title"),
                (String) revision.get("description"), (String) revision.get("details"),
                ((Number) revision.get("content_schema_version")).intValue(), commandId, fingerprint);
    }

    private CopyResult createDraftRows(long document, int process, Long base, int actor, String title,
                                      String description, String details, int schema, UUID command, String fingerprint) {
        jdbc.update("INSERT INTO sop_work_item (document_id, original_author_id, base_revision_id) VALUES (?, ?, ?)", document, actor, base);
        long request = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("""
                INSERT INTO sop_working_copy (document_id, work_item_id, editor_id, title, description, details, content_schema_version)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, document, request, actor, title, description, details, schema);
        long copy = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Work work = new Work(request, document, process, base, base, null, actor, "DRAFT", 0, null);
        recordEdit(command, work, actor, fingerprint, "DRAFT_CREATED", copy, 0L);
        return new CopyResult(request, copy, 0);
    }

    private Map<String, Object> replayCreation(UUID command, int actor, String fingerprint) {
        Objects.requireNonNull(command, "Command ID is required");
        var prior = jdbc.queryForList("SELECT * FROM workflow_command WHERE command_id = ? FOR SHARE", command.toString());
        if (prior.isEmpty()) return null;
        var saved = prior.getFirst();
        if (((Number) saved.get("actor_id")).intValue() != actor) throw new SecurityException("Command belongs to another actor");
        require(fingerprint.equals(saved.get("fingerprint")), "Command ID was reused with different input");
        return saved;
    }

    public List<Map<String, Object>> publishedDocuments() {
        requireUser(actor());
        int org = jdbc.queryForObject("SELECT org_id FROM client_configuration WHERE configuration_id = 1 FOR SHARE", Integer.class);
        return jdbc.queryForList("""
                SELECT d.document_id AS documentId, d.business_process_id AS processId,
                       d.document_kind AS kind, d.current_revision_id AS revisionId,
                       r.title, r.description, r.details, r.provenance, r.source_label AS sourceLabel
                FROM sop_document d JOIN sop_revision r ON r.document_id = d.document_id AND r.revision_id = d.current_revision_id
                JOIN business_process p ON p.business_process_id = d.business_process_id
                JOIN business_process_family f ON f.business_process_family_id = p.business_process_family_id
                JOIN department dept ON dept.department_id = f.department_id
                JOIN org_group g ON g.org_group_id = dept.org_group_id
                WHERE d.org_id = ? AND g.org_id = ? ORDER BY d.document_id
                """, org, org);
    }

    public Map<String, Object> readCopy(long requestId, long copyId) {
        int actor = actor();
        lock(requestId);
        requireUser(actor);
        var rows = jdbc.queryForList("""
                SELECT working_copy_id AS copyId, work_item_id AS requestId, source_candidate_id AS sourceCandidateId,
                       title, description, details, state, lock_version AS version
                FROM sop_working_copy WHERE working_copy_id = ? AND work_item_id = ? AND editor_id = ?
                """, copyId, requestId, actor);
        if (rows.isEmpty()) throw new SecurityException("Working copy is not available to this user");
        return rows.getFirst();
    }

    public Map<String, Object> readRequest(long requestId) {
        int actor = actor();
        Work work = lock(requestId);
        Governance governance = governance(work, actor);
        Set<String> actions = new TreeSet<>();
        boolean permitted = work.author() == actor;
        if (work.state().equals("DRAFT") && permitted) actions.addAll(Set.of("EDIT", "SUBMIT", "CANCEL"));
        if (work.candidate() != null) {
            var context = context(governance, work.candidate());
            boolean routingCurrent = Objects.equals(work.routing(), governance.fingerprint());
            boolean reviewer = routingCurrent && assigned(work.candidate()).contains(actor)
                    && policy.independentReviewers(context).contains(actor);
            boolean self = routingCurrent && policy.maySelfApprove(context, actor);
            permitted |= reviewer || self;
            if (work.state().equals("IN_REVIEW")) {
                if (reviewer) actions.addAll(Set.of("SUGGEST_EDITS", "APPROVE", "REJECT"));
                if (self) actions.add("SELF_APPROVE");
            }
        }
        if (!permitted) throw new SecurityException("Review request is not available to this user");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestId", work.id()); result.put("documentId", work.document());
        result.put("version", work.version()); result.put("state", work.state());
        result.put("candidateId", work.candidate()); result.put("actions", actions);
        result.put("candidate", work.candidate() == null ? null : snapshots.findAuthored(work.document(), work.candidate()).orElseThrow());
        return result;
    }

    public CopyResult suggestEdits(long requestId, long candidateId, long requestVersion, UUID commandId) {
        int actor = actor();
        Work work = lock(requestId);
        Governance governance = governance(work, actor);
        String fingerprint = fingerprint("SUGGEST_EDITS", requestId, candidateId, requestVersion);
        var previous = replayRow(commandId, work, actor, fingerprint);
        if (previous != null) return copyResult(previous);
        require(work.state().equals("IN_REVIEW") && work.version() == requestVersion
                && Objects.equals(work.candidate(), candidateId), "Review candidate changed or request is closed");
        assignedIndependentReviewer(work, governance, actor);
        jdbc.update("""
                INSERT INTO sop_working_copy (document_id, work_item_id, editor_id, source_candidate_id,
                    title, description, details, content_schema_version)
                SELECT document_id, work_item_id, ?, revision_id, title, description, details, content_schema_version
                FROM sop_revision WHERE revision_id = ?
                """, actor, candidateId);
        long copy = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        recordEdit(commandId, work, actor, fingerprint, "REVIEW_COPY_CREATED", copy, 0L);
        return new CopyResult(work.id(), copy, 0);
    }

    public CopyResult saveCopy(long requestId, long copyId, long requestVersion, long copyVersion,
                               String title, String description, String details, UUID commandId) {
        int actor = actor();
        Work work = lock(requestId);
        Governance governance = governance(work, actor);
        String fingerprint = fingerprint("SAVE_COPY", requestId, copyId, requestVersion, copyVersion, title, description, details);
        var previous = replayRow(commandId, work, actor, fingerprint);
        if (previous != null) return copyResult(previous);
        require(work.version() == requestVersion, "Review request changed");
        boolean review = work.state().equals("IN_REVIEW");
        require(review || work.state().equals("DRAFT"), "Request is closed for editing");
        if (review) assignedIndependentReviewer(work, governance, actor);
        else if (work.author() != actor) throw new SecurityException("Only the draft author may edit");
        validateDraftContent(title, description, details);
        var copy = jdbc.queryForMap("SELECT * FROM sop_working_copy WHERE working_copy_id = ? FOR UPDATE", copyId);
        require(((Number) copy.get("work_item_id")).longValue() == requestId, "Copy belongs to another request");
        if (((Number) copy.get("editor_id")).intValue() != actor) throw new SecurityException("Not your working copy");
        require(Objects.equals(number(copy.get("source_candidate_id")), review ? work.candidate() : null), "Copy candidate changed");
        require(jdbc.update("""
                UPDATE sop_working_copy SET title = ?, description = ?, details = ?, lock_version = lock_version + 1
                WHERE working_copy_id = ? AND lock_version = ? AND state = 'EDITABLE'
                """, title, description, details, copyId, copyVersion) == 1, "Working copy changed or is no longer editable");
        recordEdit(commandId, work, actor, fingerprint, "COPY_SAVED", copyId, copyVersion + 1);
        return new CopyResult(work.id(), copyId, copyVersion + 1);
    }

    public long reject(long requestId, long candidateId, long requestVersion, String reason, UUID commandId) {
        int actor = actor();
        Work work = lock(requestId);
        Governance governance = governance(work, actor);
        String fingerprint = fingerprint("REJECT", requestId, candidateId, requestVersion, reason);
        Long previous = replay(commandId, work, actor, fingerprint);
        if (previous != null) return previous;
        require(work.state().equals("IN_REVIEW") && work.version() == requestVersion
                && Objects.equals(work.candidate(), candidateId), "Review candidate changed or request is closed");
        nonblank(reason, "Rejection requires a reason");
        assignedIndependentReviewer(work, governance, actor);
        var authority = policy.authorize(context(governance, candidateId), actor, ApprovalPolicy.Mode.NORMAL, null);
        jdbc.update("""
                INSERT INTO approval_decision (revision_id, actor_id, authority, self_approval, reason, decision)
                VALUES (?, ?, ?, false, ?, 'REJECTED')
                """, candidateId, actor, authority.name(), reason);
        jdbc.update("UPDATE sop_work_item SET state = 'REJECTED', lock_version = lock_version + 1 WHERE work_item_id = ?", requestId);
        jdbc.update("UPDATE sop_working_copy SET state = 'CANCELLED', lock_version = lock_version + 1 WHERE work_item_id = ? AND state = 'EDITABLE'", requestId);
        record(commandId, work, candidateId, actor, fingerprint, "REJECTED", reason, Set.of(work.author()));
        return candidateId;
    }

    public long cancel(long requestId, long requestVersion, UUID commandId) {
        int actor = actor();
        Work work = lock(requestId);
        governance(work, actor);
        String fingerprint = fingerprint("CANCEL", requestId, requestVersion);
        var previous = replayRow(commandId, work, actor, fingerprint);
        if (previous != null) return number(previous.get("result_version"));
        if (work.author() != actor) throw new SecurityException("Only the draft author may cancel");
        require(work.state().equals("DRAFT") && work.version() == requestVersion, "Only an unchanged draft may be cancelled");
        jdbc.update("UPDATE sop_work_item SET state = 'CANCELLED', lock_version = lock_version + 1 WHERE work_item_id = ?", requestId);
        jdbc.update("UPDATE sop_working_copy SET state = 'CANCELLED', lock_version = lock_version + 1 WHERE work_item_id = ? AND state = 'EDITABLE'", requestId);
        recordEdit(commandId, work, actor, fingerprint, "CANCELLED", null, requestVersion + 1);
        return requestVersion + 1;
    }

    private static void validateDraftContent(String title, String description, String details) {
        if (title == null || title.length() > 255 || description == null || details == null) {
            throw new IllegalArgumentException("Draft text is required; title must be at most 255 characters");
        }
    }

    private CopyResult copyResult(Map<String, Object> saved) {
        return new CopyResult(number(saved.get("work_item_id")), number(saved.get("result_working_copy_id")), number(saved.get("result_version")));
    }

    private void recordEdit(UUID command, Work work, int actor, String fingerprint, String action, Long copy, Long version) {
        jdbc.update("""
                INSERT INTO workflow_command (command_id, work_item_id, actor_id, fingerprint,
                    result_kind, result_working_copy_id, result_version)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, command.toString(), work.id(), actor, fingerprint, copy == null ? "REQUEST" : "COPY", copy, version);
        jdbc.update("""
                INSERT INTO business_audit_event (event_id, document_id, work_item_id, revision_id, working_copy_id, actor_id, action)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, command.toString(), work.document(), work.id(), work.candidate(), copy, actor, action);
    }

    public long submit(long requestId, long copyId, long requestVersion, long copyVersion,
                       String reason, UUID commandId) {
        int actor = actor();
        Work work = lock(requestId);
        Governance governance = governance(work, actor);
        String fingerprint = fingerprint("SUBMIT", requestId, copyId, requestVersion, copyVersion, reason);
        Long previous = replay(commandId, work, actor, fingerprint);
        if (previous != null) return previous;
        require(work.version() == requestVersion, "Review request changed");
        boolean replacement = work.state().equals("IN_REVIEW");
        require(replacement || work.state().equals("DRAFT"), "Request is not open for submission");
        if (replacement) {
            nonblank(reason, "Revised candidate requires a reason");
            assignedIndependentReviewer(work, governance, actor);
        } else if (work.author() != actor) {
            throw new SecurityException("Only the draft author may submit");
        }
        var copy = jdbc.queryForMap("SELECT * FROM sop_working_copy WHERE working_copy_id = ? FOR UPDATE", copyId);
        require(((Number) copy.get("work_item_id")).longValue() == work.id(), "Copy belongs to another request");
        if (((Number) copy.get("editor_id")).intValue() != actor) throw new SecurityException("Not your working copy");
        Long source = number(copy.get("source_candidate_id"));
        require(Objects.equals(source, replacement ? work.candidate() : null), "Copy is based on a stale candidate");
        nonblank((String) copy.get("title"), "Title is required");
        nonblank((String) copy.get("description"), "Description is required");
        nonblank((String) copy.get("details"), "Procedure details are required");

        long revision = snapshots.captureAuthoredCopy(copyId, copyVersion, actor);
        if (replacement) jdbc.update("""
                INSERT INTO revision_participant (revision_id, user_id, contribution_type)
                SELECT ?, user_id, contribution_type FROM revision_participant WHERE revision_id = ?
                """, revision, work.candidate());
        participant(revision, work.author(), "AUTHOR");
        participant(revision, actor, "EDITOR");
        participant(revision, actor, "SUBMITTER");
        ApprovalPolicy.Context context = context(governance, revision);
        Set<Integer> reviewers = policy.independentReviewers(context);
        require(!reviewers.isEmpty() || context.participantIds().stream().anyMatch(id -> policy.maySelfApprove(context, id)),
                "No independent reviewer or authorized self-approval path is available");
        for (int reviewer : reviewers) jdbc.update("""
                INSERT INTO review_assignment (revision_id, reviewer_id, authority, routing_fingerprint) VALUES (?, ?, ?, ?)
                """, revision, reviewer, policy.authorize(context, reviewer, ApprovalPolicy.Mode.NORMAL, null).name(), governance.fingerprint());
        jdbc.update("UPDATE sop_working_copy SET state = 'SUBMITTED', lock_version = lock_version + 1 WHERE working_copy_id = ?", copyId);
        jdbc.update("""
                UPDATE sop_work_item SET current_candidate_id = ?, state = 'IN_REVIEW',
                  routing_fingerprint = ?, lock_version = lock_version + 1 WHERE work_item_id = ?
                """, revision, governance.fingerprint(), work.id());
        Set<Integer> recipients = new HashSet<>(reviewers);
        if (replacement) recipients.add(work.author());
        record(commandId, work, revision, actor, fingerprint,
                replacement ? "CANDIDATE_REPLACED" : "SUBMITTED", reason, recipients);
        return revision;
    }

    public long approve(long requestId, long candidateId, long requestVersion,
                        ApprovalPolicy.Mode mode, String reason, UUID commandId) {
        int actor = actor();
        Work work = lock(requestId);
        Governance governance = governance(work, actor);
        String fingerprint = fingerprint("APPROVE", requestId, candidateId, requestVersion, mode, reason);
        Long previous = replay(commandId, work, actor, fingerprint);
        if (previous != null) return previous;
        require(work.version() == requestVersion && Objects.equals(work.candidate(), candidateId)
                && work.state().equals("IN_REVIEW"), "Review candidate changed or request is closed");
        currentRouting(work, governance);
        ApprovalPolicy.Context context = context(governance, candidateId);
        if (mode == ApprovalPolicy.Mode.NORMAL) assignedIndependentReviewer(work, governance, actor);
        ApprovalPolicy.Authority authority = policy.authorize(context, actor, mode, reason);
        require(Objects.equals(work.base(), work.published()), "Published version changed; reconcile and resubmit");

        jdbc.update("""
                INSERT INTO approval_decision (revision_id, actor_id, authority, self_approval, reason)
                VALUES (?, ?, ?, ?, ?)
                """, candidateId, actor, authority.name(), mode == ApprovalPolicy.Mode.SELF_APPROVAL, reason);
        long decision = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("""
                INSERT INTO publication_record (document_id, revision_id, previous_revision_id, decision_id)
                VALUES (?, ?, ?, ?)
                """, work.document(), candidateId, work.published(), decision);
        BigDecimal position = jdbc.queryForObject(
                "SELECT COALESCE(MAX(ordering_key), 0) + 1024 FROM revision_history_position WHERE document_id = ?",
                BigDecimal.class, work.document());
        jdbc.update("INSERT INTO revision_history_position (document_id, revision_id, ordering_key) VALUES (?, ?, ?)",
                work.document(), candidateId, position);
        jdbc.update("UPDATE sop_document SET current_revision_id = ?, lock_version = lock_version + 1 WHERE document_id = ?",
                candidateId, work.document());
        jdbc.update("UPDATE sop_work_item SET state = 'PUBLISHED', lock_version = lock_version + 1 WHERE work_item_id = ?", work.id());
        Set<Integer> recipients = new HashSet<>(Set.of(work.author()));
        recipients.add(governance.owner());
        boolean self = mode == ApprovalPolicy.Mode.SELF_APPROVAL;
        if (self) recipients.addAll(policy.selfApprovalRecipients(context, assigned(candidateId)));
        record(commandId, work, candidateId, actor, fingerprint, self ? "SELF_APPROVED" : "PUBLISHED", reason, recipients);
        return candidateId;
    }

    private Work lock(long requestId) {
        long documentId = jdbc.queryForObject("SELECT document_id FROM sop_work_item WHERE work_item_id = ?", Long.class, requestId);
        var document = jdbc.queryForMap("SELECT * FROM sop_document WHERE document_id = ? FOR UPDATE", documentId);
        int process = validateDocument(document);
        Work work = jdbc.queryForObject("SELECT * FROM sop_work_item WHERE work_item_id = ? FOR UPDATE", (row, i) ->
                new Work(requestId, row.getLong("document_id"), process, number(document.get("current_revision_id")),
                        row.getObject("base_revision_id", Long.class), row.getObject("current_candidate_id", Long.class),
                        row.getInt("original_author_id"), row.getString("state"), row.getLong("lock_version"),
                        row.getString("routing_fingerprint")), requestId);
        require(work.document() == documentId, "Request document changed");
        return work;
    }

    private int validateDocument(Map<String, Object> document) {
        require("CLIENT_SOP".equals(document.get("document_kind")), "Product guides cannot enter the client approval workflow");
        int org = ((Number) document.get("org_id")).intValue();
        int process = ((Number) document.get("business_process_id")).intValue();
        if (org != clientScope(process)) throw new SecurityException("Document is outside the configured client/process");
        return process;
    }

    private int clientScope(int process) {
        int client = jdbc.queryForObject("SELECT org_id FROM client_configuration WHERE configuration_id = 1 FOR SHARE", Integer.class);
        int processOrg = jdbc.queryForObject("""
                SELECT g.org_id FROM business_process p
                JOIN business_process_family f ON f.business_process_family_id = p.business_process_family_id
                JOIN department d ON d.department_id = f.department_id
                JOIN org_group g ON g.org_group_id = d.org_group_id
                WHERE p.business_process_id = ? FOR SHARE
                """, Integer.class, process);
        if (client != processOrg) throw new SecurityException("Process is outside the configured client");
        return client;
    }

    private void requireUser(int actor) {
        if (jdbc.queryForList("SELECT id FROM users WHERE id = ? FOR SHARE", Integer.class, actor).isEmpty())
            throw new SecurityException("Authenticated user no longer exists");
    }

    private Governance governance(Work work, int actor) {
        var assignment = jdbc.queryForMap("SELECT * FROM process_governance WHERE business_process_id = ? FOR SHARE", work.process());
        int owner = ((Number) assignment.get("owner_user_id")).intValue();
        List<Integer> managers = jdbc.queryForList("SELECT manager_user_id FROM user_reporting_line WHERE user_id = ? FOR SHARE", Integer.class, owner);
        Integer manager = managers.isEmpty() ? null : managers.getFirst();
        var users = jdbc.queryForList("SELECT id, role FROM users ORDER BY id FOR SHARE");
        Set<Integer> admins = new TreeSet<>();
        boolean actorExists = false;
        for (var user : users) {
            int id = ((Number) user.get("id")).intValue();
            if (id == actor) actorExists = true;
            if ("ADMIN".equals(user.get("role"))) admins.add(id);
        }
        if (!actorExists) throw new SecurityException("Authenticated user no longer exists");
        return new Governance(owner, manager, Set.copyOf(admins),
                fingerprint(owner, manager, admins, assignment.get("lock_version")));
    }

    private ApprovalPolicy.Context context(Governance governance, long revision) {
        int submitter = jdbc.queryForObject("SELECT submitted_by_id FROM sop_revision WHERE revision_id = ?", Integer.class, revision);
        Set<Integer> participants = new HashSet<>(jdbc.queryForList(
                "SELECT DISTINCT user_id FROM revision_participant WHERE revision_id = ?", Integer.class, revision));
        return new ApprovalPolicy.Context(governance.owner(), governance.manager(), submitter, governance.admins(), participants);
    }

    private void currentRouting(Work work, Governance governance) {
        require(Objects.equals(work.routing(), governance.fingerprint()), "Review routing changed; audited reassignment is required");
    }

    private void assignedIndependentReviewer(Work work, Governance governance, int actor) {
        currentRouting(work, governance);
        if (!assigned(work.candidate()).contains(actor)) throw new SecurityException("Actor is not assigned to this candidate");
        policy.authorize(context(governance, work.candidate()), actor, ApprovalPolicy.Mode.NORMAL, null);
    }

    private Set<Integer> assigned(long revision) {
        return new HashSet<>(jdbc.queryForList("SELECT reviewer_id FROM review_assignment WHERE revision_id = ?", Integer.class, revision));
    }

    private void participant(long revision, int actor, String type) {
        jdbc.update("""
                INSERT INTO revision_participant (revision_id, user_id, contribution_type) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE contribution_type = VALUES(contribution_type)
                """, revision, actor, type);
    }

    private Long replay(UUID command, Work work, int actor, String fingerprint) {
        var saved = replayRow(command, work, actor, fingerprint);
        return saved == null ? null : number(saved.get("result_revision_id"));
    }

    private Map<String, Object> replayRow(UUID command, Work work, int actor, String fingerprint) {
        Objects.requireNonNull(command, "Command ID is required");
        var prior = jdbc.queryForList("SELECT * FROM workflow_command WHERE command_id = ? FOR SHARE", command.toString());
        if (prior.isEmpty()) return null;
        var saved = prior.getFirst();
        if (((Number) saved.get("actor_id")).intValue() != actor) throw new SecurityException("Command belongs to another actor");
        require(((Number) saved.get("work_item_id")).longValue() == work.id()
                && fingerprint.equals(saved.get("fingerprint")), "Command ID was reused with different input");
        return saved;
    }

    private void record(UUID command, Work work, long revision, int actor, String fingerprint,
                        String action, String reason, Set<Integer> recipients) {
        String event = command.toString();
        jdbc.update("INSERT INTO workflow_command (command_id, work_item_id, actor_id, fingerprint, result_revision_id) VALUES (?, ?, ?, ?, ?)",
                event, work.id(), actor, fingerprint, revision);
        jdbc.update("""
                INSERT INTO business_audit_event (event_id, document_id, work_item_id, revision_id, actor_id, action, reason)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, event, work.document(), work.id(), revision, actor, action, reason);
        jdbc.update("INSERT INTO notification_event (event_id) VALUES (?)", event);
        for (int recipient : recipients) for (String channel : List.of("IN_APP", "EMAIL")) {
            jdbc.update("INSERT INTO notification_recipient (event_id, user_id, channel) VALUES (?, ?, ?)", event, recipient, channel);
        }
    }

    private int actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AppUserDetails principal)) {
            throw new SecurityException("Authenticated application user required");
        }
        return principal.getUser().getId();
    }

    private String fingerprint(Object... values) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(values)));
        } catch (NoSuchAlgorithmException | JsonProcessingException exception) {
            throw new IllegalStateException("Cannot fingerprint workflow command", exception);
        }
    }

    private static Long number(Object value) { return value == null ? null : ((Number) value).longValue(); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static void nonblank(String value, String message) { if (value == null || value.isBlank()) throw new IllegalArgumentException(message); }
}
