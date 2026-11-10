package com.fixflow.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class BasePanelTest {

    static class TestablePanel extends BasePanel {
        final AtomicBoolean shownTriggered = new AtomicBoolean(false);

        public TestablePanel(String title, String description) {
            super(title, description);
        }

        @Override
        public void onPanelShown() {
            shownTriggered.set(true);
        }
    }

    @Test
    @DisplayName("Verify BasePanel initializes title and description correctly")
    void testBasePanelProperties() {
        TestablePanel panel = new TestablePanel("Test Title", "Test Description");
        assertEquals("Test Title", panel.getTitle());
        assertEquals("Test Description", panel.getDescription());
        assertNotNull(panel.contentArea);
    }

    @Test
    @DisplayName("Verify polymorphic onPanelShown hook executes")
    void testPolymorphicHook() {
        TestablePanel panel = new TestablePanel("Hook Test", "Desc");
        assertFalse(panel.shownTriggered.get());

        panel.onPanelShown();
        assertTrue(panel.shownTriggered.get(), "onPanelShown must execute overridden implementation");
    }
}

