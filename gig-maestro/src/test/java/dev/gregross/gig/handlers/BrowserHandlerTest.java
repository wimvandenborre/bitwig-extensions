package dev.gregross.gig.handlers;

import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.InsertionPoint;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableIntegerValue;
import dev.gregross.gig.extension.StateCache;
import dev.gregross.gig.rpc.JsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrowserHandlerTest {

    @Mock private PopupBrowser mockPopupBrowser;
    @Mock private CursorDevice mockCursorDevice;

    // Chain mocks
    @Mock private InsertionPoint mockInsertionPoint;
    @Mock private SettableIntegerValue mockContentTypeIndex;
    @Mock private SettableBooleanValue mockShouldAudition;

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();
        new BrowserHandler(mockPopupBrowser, mockCursorDevice, new StateCache()).register(dispatcher);
    }

    // --- Registration ---

    @Test
    void registersTwentyOneMethods() {
        var methods = dispatcher.getRegisteredMethods();
        // Phase 17 — 11 methods
        assertTrue(methods.contains("browser/browsePresets"));
        assertTrue(methods.contains("browser/browseInsertDevice"));
        assertTrue(methods.contains("browser/selectNextFile"));
        assertTrue(methods.contains("browser/selectPreviousFile"));
        assertTrue(methods.contains("browser/selectFirstFile"));
        assertTrue(methods.contains("browser/selectLastFile"));
        assertTrue(methods.contains("browser/commit"));
        assertTrue(methods.contains("browser/cancel"));
        assertTrue(methods.contains("browser/setContentType"));
        assertTrue(methods.contains("browser/setShouldAudition"));
        assertTrue(methods.contains("browser/getState"));
        // Phase 18 — 10 methods
        assertTrue(methods.contains("browser/filterSelectNext"));
        assertTrue(methods.contains("browser/filterSelectPrevious"));
        assertTrue(methods.contains("browser/filterSelectFirst"));
        assertTrue(methods.contains("browser/filterSelectLast"));
        assertTrue(methods.contains("browser/filterSelectParent"));
        assertTrue(methods.contains("browser/filterSelectFirstChild"));
        assertTrue(methods.contains("browser/filterReset"));
        assertTrue(methods.contains("browser/getFilters"));
        assertTrue(methods.contains("browser/getResults"));
        assertTrue(methods.contains("browser/scrollResults"));
        assertEquals(21, methods.size());
    }

    // --- setContentType validation ---

    @Test
    void setContentType_missingIndex_returnsError() {
        String response = dispatcher.handle(rpc("browser/setContentType", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "index");
    }

    // --- setShouldAudition validation ---

    @Test
    void setShouldAudition_missingEnabled_returnsError() {
        String response = dispatcher.handle(rpc("browser/setShouldAudition", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "enabled");
    }

    // --- Filter validation ---

    @Test
    void filterSelectNext_missingColumn_returnsError() {
        String response = dispatcher.handle(rpc("browser/filterSelectNext", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "column");
    }

    @Test
    void filterSelectNext_invalidColumn_returnsError() {
        String response = dispatcher.handle(rpc("browser/filterSelectNext",
            "{\"column\": \"invalid\"}"));
        assertContains(response, "-32602");
        assertContains(response, "invalid");
    }

    @Test
    void filterReset_missingColumn_returnsError() {
        String response = dispatcher.handle(rpc("browser/filterReset", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "column");
    }

    // --- scrollResults validation ---

    @Test
    void scrollResults_missingDirection_returnsError() {
        String response = dispatcher.handle(rpc("browser/scrollResults", "{}"));
        assertContains(response, "-32602");
        assertContains(response, "direction");
    }

    @Test
    void scrollResults_invalidDirection_returnsError() {
        String response = dispatcher.handle(rpc("browser/scrollResults",
            "{\"direction\": \"sideways\"}"));
        assertContains(response, "-32602");
        assertContains(response, "sideways");
    }

    // --- Behavioral tests (Mockito) — Browser opening ---

    @Test
    void browsePresets_callsCursorDeviceReplaceInsertionPointBrowse() {
        when(mockCursorDevice.replaceDeviceInsertionPoint()).thenReturn(mockInsertionPoint);
        dispatcher.handle(rpc("browser/browsePresets", "{}"));
        verify(mockInsertionPoint).browse();
    }

    @Test
    void browseInsertDevice_callsCursorDeviceAfterInsertionPointBrowse() {
        when(mockCursorDevice.afterDeviceInsertionPoint()).thenReturn(mockInsertionPoint);
        dispatcher.handle(rpc("browser/browseInsertDevice", "{}"));
        verify(mockInsertionPoint).browse();
    }

    // --- Behavioral tests (Mockito) — Result navigation ---

    @Test
    void selectNextFile_callsPopupBrowserSelectNextFile() {
        dispatcher.handle(rpc("browser/selectNextFile", "{}"));
        verify(mockPopupBrowser).selectNextFile();
    }

    @Test
    void selectPreviousFile_callsPopupBrowserSelectPreviousFile() {
        dispatcher.handle(rpc("browser/selectPreviousFile", "{}"));
        verify(mockPopupBrowser).selectPreviousFile();
    }

    @Test
    void selectFirstFile_callsPopupBrowserSelectFirstFile() {
        dispatcher.handle(rpc("browser/selectFirstFile", "{}"));
        verify(mockPopupBrowser).selectFirstFile();
    }

    @Test
    void selectLastFile_callsPopupBrowserSelectLastFile() {
        dispatcher.handle(rpc("browser/selectLastFile", "{}"));
        verify(mockPopupBrowser).selectLastFile();
    }

    // --- Behavioral tests (Mockito) — Commit / cancel ---

    @Test
    void commit_callsPopupBrowserCommit() {
        dispatcher.handle(rpc("browser/commit", "{}"));
        verify(mockPopupBrowser).commit();
    }

    @Test
    void cancel_callsPopupBrowserCancel() {
        dispatcher.handle(rpc("browser/cancel", "{}"));
        verify(mockPopupBrowser).cancel();
    }

    // --- Behavioral tests (Mockito) — Settings ---

    @Test
    void setContentType_callsPopupBrowserSelectedContentTypeIndexSet() {
        when(mockPopupBrowser.selectedContentTypeIndex()).thenReturn(mockContentTypeIndex);
        dispatcher.handle(rpc("browser/setContentType", "{\"index\":2}"));
        verify(mockContentTypeIndex).set(2);
    }

    @Test
    void setShouldAudition_callsPopupBrowserShouldAuditionSet() {
        when(mockPopupBrowser.shouldAudition()).thenReturn(mockShouldAudition);
        dispatcher.handle(rpc("browser/setShouldAudition", "{\"enabled\":true}"));
        verify(mockShouldAudition).set(true);
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
