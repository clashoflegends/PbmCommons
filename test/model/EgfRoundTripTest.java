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

    /**
     * BaseModel.habilidadeParameter must be EGF-safe: lazy-null so it is omitted from the serialized
     * form until used (old EGFs / old clients see no new element), survive a round-trip when set, and
     * be omitted again once removed (null-when-empty in remHabilidade). Local is the first consumer.
     */
    @Test
    void habilidadeParameterIsLazyNullAndRoundTrips() {
        XmlManager xm = XmlManager.getInstance();
        Local local = new Local();

        // (1) lazy-null: no parameter -> field absent from the XML
        assertFalse(xm.toXml(local).contains("habilidadeParameter"),
                "an unused habilidadeParameter must be omitted (else old EGFs/clients break)");

        // (2) set -> emitted and survives a round-trip
        local.setHabilidadeParametro(";LIP;", "40");
        String xmlSet = xm.toXml(local);
        assertTrue(xmlSet.contains("habilidadeParameter"), "a set parameter must serialize");
        Local back = (Local) xm.fromXml(xmlSet);
        assertEquals("40", back.getHabilidadeParametro(";LIP;"), "parameter must round-trip");

        // (3) null-when-empty: removing the ability drops the param and re-omits the field
        local.remHabilidade(";LIP;");
        assertNull(local.getHabilidadeParametro(";LIP;"), "param must be gone after remHabilidade");
        assertFalse(xm.toXml(local).contains("habilidadeParameter"),
                "after removing the last parameter the field must be omitted again");
    }
}
