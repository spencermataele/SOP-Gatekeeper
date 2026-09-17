package com.woven.app.service.governance;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Pure routing rules. Callers must load these facts from authoritative server-side data. */
public final class ApprovalPolicy {
    public enum Mode { NORMAL, SELF_APPROVAL }
    public enum Authority { PROCESS_OWNER, DIRECT_MANAGER, ADMINISTRATOR }

    public record Context(int ownerId, Integer managerId, int submitterId,
                          Set<Integer> administratorIds, Set<Integer> participantIds) {
        public Context {
            if (ownerId <= 0 || submitterId <= 0 || (managerId != null && managerId <= 0)) {
                throw new IllegalArgumentException("User IDs must be positive");
            }
            administratorIds = Set.copyOf(administratorIds);
            participantIds = Set.copyOf(participantIds);
            if (administratorIds.stream().anyMatch(id -> id <= 0)
                    || participantIds.stream().anyMatch(id -> id <= 0)) {
                throw new IllegalArgumentException("User IDs must be positive");
            }
            if (!participantIds.contains(submitterId)) {
                throw new IllegalArgumentException("The submitter must be a captured participant");
            }
        }
    }

    public Set<Integer> independentReviewers(Context context) {
        Set<Integer> reviewers = new HashSet<>();
        if (context.submitterId() == context.ownerId()) {
            if (context.managerId() != null) reviewers.add(context.managerId());
            reviewers.addAll(context.administratorIds());
        } else {
            reviewers.add(context.ownerId());
        }
        reviewers.removeAll(context.participantIds());
        return Set.copyOf(reviewers);
    }

    public boolean maySelfApprove(Context context, int actorId) {
        return context.participantIds().contains(actorId)
                && (actorId == context.ownerId() || context.administratorIds().contains(actorId));
    }

    /** Current assignments must also be checked by the transactional workflow service. */
    public Authority authorize(Context context, int actorId, Mode mode, String reason) {
        Objects.requireNonNull(mode, "Approval mode is required");
        if (mode == Mode.SELF_APPROVAL) {
            if (!maySelfApprove(context, actorId)) {
                throw new SecurityException("Self-approval requires a participating process owner or administrator");
            }
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Self-approval requires a reason");
            }
        } else if (!independentReviewers(context).contains(actorId)) {
            throw new SecurityException("Actor is not an eligible independent reviewer");
        }
        if (actorId == context.ownerId()) return Authority.PROCESS_OWNER;
        // Record administrator authority for a manager using the administrator exception.
        if (mode == Mode.SELF_APPROVAL || !Objects.equals(context.managerId(), actorId)) {
            return Authority.ADMINISTRATOR;
        }
        return Authority.DIRECT_MANAGER;
    }

    public Set<Integer> selfApprovalRecipients(Context context, Set<Integer> designatedApproverIds) {
        Set<Integer> recipients = new HashSet<>(designatedApproverIds);
        if (context.managerId() != null) recipients.add(context.managerId());
        recipients.addAll(context.administratorIds());
        return Set.copyOf(recipients);
    }
}
