package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationHandlerTest {

    @Mock private Application mockApplication;
    @Mock private ControllerHost mockHost;

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new ApplicationHandler(mockApplication, mockHost).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersTwentyMethods() {
        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("app/undo"));
        assertTrue(methods.contains("app/redo"));
        assertTrue(methods.contains("app/getState"));
        assertTrue(methods.contains("app/activateEngine"));
        assertTrue(methods.contains("app/deactivateEngine"));
        assertTrue(methods.contains("app/showNotification"));
        assertTrue(methods.contains("app/setPanelLayout"));
        assertTrue(methods.contains("app/toggleInspector"));
        assertTrue(methods.contains("app/toggleDevices"));
        assertTrue(methods.contains("app/toggleMixer"));
        assertTrue(methods.contains("app/toggleNoteEditor"));
        assertTrue(methods.contains("app/toggleAutomationEditor"));
        assertTrue(methods.contains("app/toggleBrowser"));
        assertTrue(methods.contains("app/toggleFullScreen"));
        assertTrue(methods.contains("app/previousSubPanel"));
        assertTrue(methods.contains("app/nextSubPanel"));
        assertTrue(methods.contains("app/zoomIn"));
        assertTrue(methods.contains("app/zoomOut"));
        assertTrue(methods.contains("app/zoomToFit"));
        assertTrue(methods.contains("app/zoomToSelection"));
        assertEquals(20, methods.size());
    }

    // --- showNotification validation ---

    @Test
    void showNotification_missingText_returnsError() {
        String response = dispatcher.handle(rpc("app/showNotification", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "text");
    }

    // --- setPanelLayout validation ---

    @Test
    void setPanelLayout_missingLayout_returnsError() {
        String response = dispatcher.handle(rpc("app/setPanelLayout", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "layout");
    }

    @Test
    void setPanelLayout_invalidLayout_returnsError() {
        String response = dispatcher.handle(rpc("app/setPanelLayout",
            "{\"layout\": \"INVALID\"}"));
        assertContains(response, "-32602");
        assertContains(response, "INVALID");
    }

    // --- Behavioral tests (Mockito) ---

    @Test
    void undo_callsApplicationUndo() {
        dispatcher.handle(rpc("app/undo", "{}"));

        verify(mockApplication).undo();
    }

    @Test
    void redo_callsApplicationRedo() {
        dispatcher.handle(rpc("app/redo", "{}"));

        verify(mockApplication).redo();
    }

    @Test
    void activateEngine_callsApplicationActivateEngine() {
        dispatcher.handle(rpc("app/activateEngine", "{}"));

        verify(mockApplication).activateEngine();
    }

    @Test
    void deactivateEngine_callsApplicationDeactivateEngine() {
        dispatcher.handle(rpc("app/deactivateEngine", "{}"));

        verify(mockApplication).deactivateEngine();
    }

    @Test
    void showNotification_callsHostShowPopupNotification() {
        dispatcher.handle(rpc("app/showNotification", "{\"text\":\"Hello\"}"));

        verify(mockHost).showPopupNotification("Hello");
    }

    @Test
    void setPanelLayout_callsApplicationSetPanelLayout() {
        dispatcher.handle(rpc("app/setPanelLayout", "{\"layout\":\"ARRANGE\"}"));

        verify(mockApplication).setPanelLayout("ARRANGE");
    }

    // --- Panel toggle behavioral tests ---

    @Test
    void toggleInspector_callsApplicationToggleInspector() {
        dispatcher.handle(rpc("app/toggleInspector", "{}"));
        verify(mockApplication).toggleInspector();
    }

    @Test
    void toggleDevices_callsApplicationToggleDevices() {
        dispatcher.handle(rpc("app/toggleDevices", "{}"));
        verify(mockApplication).toggleDevices();
    }

    @Test
    void toggleMixer_callsApplicationToggleMixer() {
        dispatcher.handle(rpc("app/toggleMixer", "{}"));
        verify(mockApplication).toggleMixer();
    }

    @Test
    void toggleNoteEditor_callsApplicationToggleNoteEditor() {
        dispatcher.handle(rpc("app/toggleNoteEditor", "{}"));
        verify(mockApplication).toggleNoteEditor();
    }

    @Test
    void toggleAutomationEditor_callsApplicationToggleAutomationEditor() {
        dispatcher.handle(rpc("app/toggleAutomationEditor", "{}"));
        verify(mockApplication).toggleAutomationEditor();
    }

    @Test
    void toggleBrowser_callsApplicationToggleBrowserVisibility() {
        dispatcher.handle(rpc("app/toggleBrowser", "{}"));
        verify(mockApplication).toggleBrowserVisibility();
    }

    @Test
    void toggleFullScreen_callsApplicationToggleFullScreen() {
        dispatcher.handle(rpc("app/toggleFullScreen", "{}"));
        verify(mockApplication).toggleFullScreen();
    }

    @Test
    void previousSubPanel_callsApplicationPreviousSubPanel() {
        dispatcher.handle(rpc("app/previousSubPanel", "{}"));
        verify(mockApplication).previousSubPanel();
    }

    @Test
    void nextSubPanel_callsApplicationNextSubPanel() {
        dispatcher.handle(rpc("app/nextSubPanel", "{}"));
        verify(mockApplication).nextSubPanel();
    }

    // --- Zoom behavioral tests ---

    @Test
    void zoomIn_callsApplicationZoomIn() {
        dispatcher.handle(rpc("app/zoomIn", "{}"));
        verify(mockApplication).zoomIn();
    }

    @Test
    void zoomOut_callsApplicationZoomOut() {
        dispatcher.handle(rpc("app/zoomOut", "{}"));
        verify(mockApplication).zoomOut();
    }

    @Test
    void zoomToFit_callsApplicationZoomToFit() {
        dispatcher.handle(rpc("app/zoomToFit", "{}"));
        verify(mockApplication).zoomToFit();
    }

    @Test
    void zoomToSelection_callsApplicationZoomToSelection() {
        dispatcher.handle(rpc("app/zoomToSelection", "{}"));
        verify(mockApplication).zoomToSelection();
    }

    // --- Helpers ---

    private String rpc(String method, String params) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"" + method + "\",\"params\":" + params + ",\"id\":1}";
    }

    private void assertContains(String actual, String expected) {
        assertTrue(actual.contains(expected),
            "Expected '" + expected + "' in: " + actual);
    }
}
