package com.fixflow.model;

import com.fixflow.enums.RepairStatus;
import com.fixflow.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RepairStatusTransitionTest {

    @Test
    @DisplayName("Verify valid transitions from RECEIVED")
    void testTransitionsFromReceived() {
        RepairJob job = new RepairJob(1, 1, "Screen cracked");
        assertEquals(RepairStatus.RECEIVED, job.getCurrentStatus());

        Set<RepairStatus> next = job.getValidNextStatuses();
        assertEquals(2, next.size());
        assertTrue(next.contains(RepairStatus.DIAGNOSING));
        assertTrue(next.contains(RepairStatus.CANCELLED));

        assertTrue(job.canTransitionTo(RepairStatus.DIAGNOSING));
        assertTrue(job.canTransitionTo(RepairStatus.CANCELLED));
        assertFalse(job.canTransitionTo(RepairStatus.COLLECTED));
        assertFalse(job.canTransitionTo(RepairStatus.REPAIRING));
    }

    @Test
    @DisplayName("Verify approval is required to transition from WAITING_FOR_APPROVAL to REPAIRING")
    void testApprovalRequirementForRepairing() {
        RepairJob job = new RepairJob(1, 1, "Water damage");
        job.setCurrentStatus(RepairStatus.WAITING_FOR_APPROVAL);
        job.setApprovalStatus("PENDING");

        // Without approval, transition to REPAIRING must fail
        InvalidStatusTransitionException ex = assertThrows(
                InvalidStatusTransitionException.class,
                () -> job.validateTransition(RepairStatus.REPAIRING)
        );
        assertTrue(ex.getMessage().contains("without recorded customer approval"));

        // With approval, transition must succeed
        job.setApprovalStatus("APPROVED");
        assertDoesNotThrow(() -> job.validateTransition(RepairStatus.REPAIRING));
    }

    @Test
    @DisplayName("Verify waiting for parts loop: REPAIRING -> WAITING_FOR_PARTS -> REPAIRING")
    void testWaitingForPartsLoop() {
        RepairJob job = new RepairJob(1, 1, "Bad logic board");
        job.setCurrentStatus(RepairStatus.REPAIRING);

        assertTrue(job.canTransitionTo(RepairStatus.WAITING_FOR_PARTS));
        assertDoesNotThrow(() -> job.validateTransition(RepairStatus.WAITING_FOR_PARTS));

        job.setCurrentStatus(RepairStatus.WAITING_FOR_PARTS);
        assertTrue(job.canTransitionTo(RepairStatus.REPAIRING));
        assertDoesNotThrow(() -> job.validateTransition(RepairStatus.REPAIRING));
    }

    @Test
    @DisplayName("Verify terminal states COLLECTED and CANCELLED cannot transition further")
    void testTerminalStatesCannotTransition() {
        RepairJob collectedJob = new RepairJob(1, 1, "Fixed phone");
        collectedJob.setCurrentStatus(RepairStatus.COLLECTED);
        assertTrue(collectedJob.getCurrentStatus().isTerminal());
        assertTrue(collectedJob.getValidNextStatuses().isEmpty());

        for (RepairStatus status : RepairStatus.values()) {
            assertFalse(collectedJob.canTransitionTo(status));
            assertThrows(InvalidStatusTransitionException.class, () -> collectedJob.validateTransition(status));
        }

        RepairJob cancelledJob = new RepairJob(1, 1, "Declined quote");
        cancelledJob.setCurrentStatus(RepairStatus.CANCELLED);
        assertTrue(cancelledJob.getCurrentStatus().isTerminal());
        assertTrue(cancelledJob.getValidNextStatuses().isEmpty());

        for (RepairStatus status : RepairStatus.values()) {
            assertFalse(cancelledJob.canTransitionTo(status));
            assertThrows(InvalidStatusTransitionException.class, () -> cancelledJob.validateTransition(status));
        }
    }

    @Test
    @DisplayName("Verify invalid direct jump (e.g. RECEIVED to COLLECTED) throws exception")
    void testInvalidDirectJumps() {
        RepairJob job = new RepairJob(1, 1, "Broken speaker");
        job.setCurrentStatus(RepairStatus.RECEIVED);

        assertThrows(InvalidStatusTransitionException.class, () -> job.validateTransition(RepairStatus.COLLECTED));
        assertThrows(InvalidStatusTransitionException.class, () -> job.validateTransition(RepairStatus.READY_FOR_COLLECTION));
        assertThrows(InvalidStatusTransitionException.class, () -> job.validateTransition(RepairStatus.WAITING_FOR_PARTS));
    }
}

