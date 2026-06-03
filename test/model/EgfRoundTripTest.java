package model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistenceCommons.XmlManager;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Synthetic round-trip: creates a World in memory, serializes it to a .egf,
 * deserializes it back, and verifies structural integrity.
 * This catches regressions in XmlManager.save() / get() without needing
 * real game files.
 */
class EgfRoundTripTest {

    @TempDir
    Path tempDir;

    @Test
    void roundTripEmptyWorld() throws Exception {
        World original = new World();
        File egf = tempDir.resolve("roundtrip-test.egf").toFile();

        XmlManager.getInstance().save(original, egf);

        assertTrue(egf.exists(), "save() did not create the file");
        assertTrue(egf.length() > 0, "saved file is empty");

        World loaded = (World) XmlManager.getInstance().get(egf);

        assertNotNull(loaded);
        // All collection fields must be non-null — either from the XML or from readResolve
        assertNotNull(loaded.getNacoes());
        assertNotNull(loaded.getCidades());
        assertNotNull(loaded.getExercitos());
        assertNotNull(loaded.getLocais());
        assertNotNull(loaded.getPersonagens());
        assertNotNull(loaded.getPackages());
        // Empty world — all collections should be empty
        assertTrue(loaded.getNacoes().isEmpty());
        assertTrue(loaded.getCidades().isEmpty());
    }
}
