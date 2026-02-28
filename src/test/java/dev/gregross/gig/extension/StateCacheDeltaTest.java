package dev.gregross.gig.extension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StateCacheDeltaTest {

    private StateCache cache;

    @BeforeEach
    void setUp() {
        cache = new StateCache();
    }

    @Test
    void firstCallReportsAllSectionsChanged() {
        List<String> changed = cache.getChangedSections();
        // On first call, hashes are all 0 (default int), but the serialized
        // JSON of default state will have a non-zero hash, so all sections
        // should be reported as changed.
        assertEquals(9, changed.size());
        assertTrue(changed.contains("transport"));
        assertTrue(changed.contains("tracks"));
        assertTrue(changed.contains("scenes"));
        assertTrue(changed.contains("device"));
        assertTrue(changed.contains("clip"));
        assertTrue(changed.contains("master"));
        assertTrue(changed.contains("application"));
        assertTrue(changed.contains("arranger"));
        assertTrue(changed.contains("arrangement"));
    }

    @Test
    void secondCallWithNoChangesReportsEmpty() {
        // First call initializes hashes
        cache.getChangedSections();
        // Second call with no state changes
        List<String> changed = cache.getChangedSections();
        assertTrue(changed.isEmpty(), "Expected no changes but got: " + changed);
    }

    @Test
    void snapshotStillWorksAfterDeltaDetection() {
        cache.getChangedSections();
        var snapshot = cache.getSnapshot();
        assertNotNull(snapshot);
        assertTrue(snapshot.has("transport"));
        assertTrue(snapshot.has("tracks"));
        assertTrue(snapshot.has("scenes"));
        assertTrue(snapshot.has("device"));
        assertTrue(snapshot.has("clip"));
        assertTrue(snapshot.has("master"));
        assertTrue(snapshot.has("application"));
        assertTrue(snapshot.has("arranger"));
        assertTrue(snapshot.has("arrangement"));
    }

    @Test
    void repeatedCallsWithNoChangesAlwaysEmpty() {
        cache.getChangedSections(); // init
        for (int i = 0; i < 5; i++) {
            List<String> changed = cache.getChangedSections();
            assertTrue(changed.isEmpty(), "Iteration " + i + " reported changes: " + changed);
        }
    }
}
