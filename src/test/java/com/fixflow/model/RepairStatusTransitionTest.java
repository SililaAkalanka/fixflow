package com.fixflow.model;

import com.fixflow.enums.RepairStatus;
import com.fixflow.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RepairStatusTransitionTest {

    @Test
    @DisplayName("Verify valid transitions from RECEIVED: DIAGNOSING and CANCELLED")
    void testTransitionsFromReceived() {
        RepairJob job = new RepairJob(1, 1, "Screen cracked");
        assertEquals(RepairStatus.RECEIVED, job.getCurrentStatus());

        Set<RepairStatus> next = job.getValidNextStatuses();
        assertEquals(2, next.size());
        assertTrue(next.contains(RepairStatus.DIAGNOSING));
        assertTrue(next.contains(RepairStatus.CANCELLED));

        assertTrue(job.canTransitionTo(RepairStatus.DIAGNOSING));
        assertTrue(job.canTransitionTo(RepairStatus.CANCELLED));
        assertFalse(job.canTransitionTo(RepairStatus.IN_PROGRESS));
        assertFalse(job.canTransitionTo(RepairStatus.COMPLETED));
        assertFalse(job.canTransitionTo(RepairStatus.COLLECTED));
    }

    @Test
    @DisplayName("Verify valid transitions from DIAGNOSING: IN_PROGRESS and CANCELLED")
    void testTransitionsFromDiagnosing() {
        RepairJob job = new RepairJob(1, 1, "Water damage");
        job.setCurrentStatus(RepairStatus.DIAGNOSING);

        assertTrue(job.canTransitionTo(RepairStatus.IN_PROGRESS));
        assertTrue(job.canTransitionTo(RepairStatus.CANCELLED));
        assertFalse(job.canTransitionTo(RepairStatus.COMPLETED));
        assertFalse(job.canTransitionTo(RepairStatus.COLLECTED));
        assertFalse(job.canTransitionTo(RepairStatus.RECEIVED));
    }

    @Test
    @DisplayName("Verify valid transitions from IN_PROGRESS: COMPLETED and CANCELLED")
    void testTransitionsFromInProgress() {
        RepairJob job = new RepairJob(1, 1, "Bad logic board");
        job.setCurrentStatus(RepairStatus.IN_PROGRESS);

        assertTrue(job.canTransitionTo(RepairStatus.COMPLETED));
        assertTrue(job.canTransitionTo(RepairStatus.CANCELLED));
        assertFalse(job.canTransitionTo(RepairStatus.COLLECTED));
        assertFalse(job.canTransitionTo(RepairStatus.DIAGNOSING));
    }

    @Test
    @DisplayName("Verify valid transitions from COMPLETED: COLLECTED and CANCELLED")
    void testTransitionsFromCompleted() {
        RepairJob job = new RepairJob(1, 1, "Fixed phone");
        job.setCurrentStatus(RepairStatus.COMPLETED);

        assertTrue(job.canTransitionTo(RepairStatus.COLLECTED));
        assertTrue(job.canTransitionTo(RepairStatus.CANCELLED));
        assertFalse(job.canTransitionTo(RepairStatus.IN_PROGRESS));
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

        RepairJob cancelledJob = new RepairJob(1, 1, "Declined repair");
        cancelledJob.setCurrentStatus(RepairStatus.CANCELLED);
        assertTrue(cancelledJob.getCurrentStatus().isTerminal());
        assertTrue(cancelledJob.getValidNextStatuses().isEmpty());

        for (RepairStatus status : RepairStatus.values()) {
            assertFalse(cancelledJob.canTransitionTo(status));
            assertThrows(InvalidStatusTransitionException.class, () -> cancelledJob.validateTransition(status));
        }
    }

    @Test
    @DisplayName("Verify invalid direct jumps throw exception")
    void testInvalidDirectJumps() {
        RepairJob job = new RepairJob(1, 1, "Broken speaker");
        job.setCurrentStatus(RepairStatus.RECEIVED);

        assertThrows(InvalidStatusTransitionException.class, () -> job.validateTransition(RepairStatus.COLLECTED));
        assertThrows(InvalidStatusTransitionException.class, () -> job.validateTransition(RepairStatus.COMPLETED));
        assertThrows(InvalidStatusTransitionException.class, () -> job.validateTransition(RepairStatus.IN_PROGRESS));
    }
}
