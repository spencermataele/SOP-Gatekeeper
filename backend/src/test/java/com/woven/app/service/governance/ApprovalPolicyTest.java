package com.woven.app.service.governance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static com.woven.app.service.governance.ApprovalPolicy.Authority.*;
import static com.woven.app.service.governance.ApprovalPolicy.Mode.*;
import static org.junit.jupiter.api.Assertions.*;

class ApprovalPolicyTest {
    private static final int OWNER = 10, MANAGER = 20, ADMIN = 30, AUTHOR = 40;
    private final ApprovalPolicy policy = new ApprovalPolicy();

    private ApprovalPolicy.Context context(int submitter, Set<Integer> participants) {
        return new ApprovalPolicy.Context(OWNER, MANAGER, submitter, Set.of(ADMIN), participants);
    }

    @ParameterizedTest
    @ValueSource(ints = {AUTHOR, MANAGER, ADMIN})
    void authorManagerAndAdminSubmissionsRouteToProcessExpert(int submitter) {
        var context = context(submitter, Set.of(submitter));
        assertEquals(Set.of(OWNER), policy.independentReviewers(context));
        assertEquals(PROCESS_OWNER, policy.authorize(context, OWNER, NORMAL, null));
    }

    @Test
    void eitherManagerOrAdminCanIndependentlyApproveOwnerSubmission() {
        var context = context(OWNER, Set.of(OWNER));
        assertEquals(Set.of(MANAGER, ADMIN), policy.independentReviewers(context));
        assertEquals(DIRECT_MANAGER, policy.authorize(context, MANAGER, NORMAL, null));
        assertEquals(ADMINISTRATOR, policy.authorize(context, ADMIN, NORMAL, null));
        assertThrows(SecurityException.class, () -> policy.authorize(context, OWNER, NORMAL, null));
        assertEquals(PROCESS_OWNER, policy.authorize(context, OWNER, SELF_APPROVAL, "Emergency correction"));
    }

    @Test
    void ownerReviewerEditsRerouteAndRetainOriginalAuthorParticipation() {
        var context = context(OWNER, Set.of(AUTHOR, OWNER));
        assertEquals(Set.of(MANAGER, ADMIN), policy.independentReviewers(context));
        assertThrows(SecurityException.class, () -> policy.authorize(context, AUTHOR, NORMAL, null));
        assertThrows(SecurityException.class, () -> policy.authorize(context, OWNER, NORMAL, null));
    }

    @Test
    void changingSubmitterDoesNotEraseAnOwnersContribution() {
        var context = context(MANAGER, Set.of(OWNER, MANAGER));
        assertTrue(policy.independentReviewers(context).isEmpty());
        assertThrows(SecurityException.class, () -> policy.authorize(context, OWNER, NORMAL, null));
        assertTrue(policy.maySelfApprove(context, OWNER));
        assertFalse(policy.maySelfApprove(context, MANAGER));
    }

    @Test
    void adminEditorCanUseExplicitExceptionButNotNormalApproval() {
        var context = context(ADMIN, Set.of(AUTHOR, ADMIN));
        assertEquals(Set.of(OWNER), policy.independentReviewers(context));
        assertThrows(SecurityException.class, () -> policy.authorize(context, ADMIN, NORMAL, null));
        assertEquals(ADMINISTRATOR, policy.authorize(context, ADMIN, SELF_APPROVAL, "Reviewed correction"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void blankSelfApprovalReasonIsRejected(String reason) {
        var context = context(OWNER, Set.of(OWNER));
        assertThrows(IllegalArgumentException.class,
                () -> policy.authorize(context, OWNER, SELF_APPROVAL, reason));
    }

    @ParameterizedTest
    @ValueSource(ints = {AUTHOR, MANAGER, 99})
    void unrelatedActorsCannotUseSelfApproval(int actor) {
        var context = context(actor, Set.of(actor));
        assertFalse(policy.maySelfApprove(context, actor));
        assertThrows(SecurityException.class,
                () -> policy.authorize(context, actor, SELF_APPROVAL, "I want to approve"));
    }

    @Test
    void managerWithAdminRoleUsesAdminAuthorityForSelfApproval() {
        var context = new ApprovalPolicy.Context(OWNER, MANAGER, MANAGER, Set.of(MANAGER), Set.of(MANAGER));
        assertEquals(ADMINISTRATOR, policy.authorize(context, MANAGER, SELF_APPROVAL, "Correction verified"));
    }

    @Test
    void missingManagerRoutesOwnerWorkToAdmin() {
        var context = new ApprovalPolicy.Context(OWNER, null, OWNER, Set.of(ADMIN), Set.of(OWNER));
        assertEquals(Set.of(ADMIN), policy.independentReviewers(context));
    }

    @Test
    void allParticipatingReviewersLeaveNoIndependentApprovalPath() {
        var context = context(OWNER, Set.of(OWNER, MANAGER, ADMIN));
        assertTrue(policy.independentReviewers(context).isEmpty());
        assertThrows(SecurityException.class, () -> policy.authorize(context, ADMIN, NORMAL, null));
    }

    @Test
    void nonParticipantCannotMislabelIndependentApprovalAsSelfApproval() {
        var context = context(OWNER, Set.of(OWNER));
        assertFalse(policy.maySelfApprove(context, ADMIN));
        assertThrows(SecurityException.class,
                () -> policy.authorize(context, ADMIN, SELF_APPROVAL, "Not a contributor"));
    }

    @Test
    void participantsCannotBeRemovedByMutatingCallerCollection() {
        var participants = new HashSet<>(Set.of(OWNER, MANAGER));
        var context = context(OWNER, participants);
        participants.remove(MANAGER);
        assertThrows(SecurityException.class, () -> policy.authorize(context, MANAGER, NORMAL, null));
    }

    @Test
    void missingSubmitterParticipationFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> context(AUTHOR, Set.of(OWNER)));
    }

    @Test
    void selfApprovalRecipientsCombineAssignmentsManagerAndAdminsWithoutDuplicates() {
        var context = new ApprovalPolicy.Context(OWNER, MANAGER, OWNER,
                Set.of(MANAGER, ADMIN), Set.of(OWNER));
        assertEquals(Set.of(OWNER, MANAGER, ADMIN),
                policy.selfApprovalRecipients(context, Set.of(OWNER, MANAGER)));
    }
}
