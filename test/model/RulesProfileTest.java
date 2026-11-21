package model;

import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;
import persistenceCommons.XmlManager;
import static org.junit.jupiter.api.Assertions.*;

class RulesProfileTest {
    private Partida game(String... codes) {
        Partida game = new Partida();
        for (String code : codes) {
            Habilidade power = new Habilidade(); power.setCodigo(code); power.setValor(1);
            game.addHabilidade(power);
        }
        return game;
    }

    @Test void legacyGamesKeepTheirExistingMechanics() {
        assertFalse(game().isNewRules());
        assertFalse(game(";GAG;", ";GAF;", ";GGF;").isNewRules());
        assertTrue(game(";GNR;").isNewRules());
    }

    @Test void trialKeepsStandardMechanicsWithoutNeedingStandardMarker() {
        assertTrue(game(";GTR;").isNewRules());
        assertTrue(game(";GNR;", ";GTR;").isNewRules());
        assertFalse(game(";GTR;").hasHabilidade(";GNR;"));
    }

    @Test void profileMarkersSurviveEgfSerialization() {
        XmlManager xml = XmlManager.getInstance();
        for (String marker : new String[]{";GNR;", ";GTR;"}) {
            Partida restored = (Partida) xml.fromXml(xml.toXml(game(marker)));
            assertTrue(restored.isNewRules());
            assertTrue(restored.hasHabilidade(marker));
        }
    }

    @Test void settingsLabelsAreDistinctInEverySupportedLocale() {
        for (String lang : new String[]{"en", "pt", "es", "it", "ca"}) {
            ResourceBundle labels = ResourceBundle.getBundle("rulesandhelpothers", Locale.forLanguageTag(lang));
            assertEquals("Standard Rules", labels.getString("DB.POWER.GNR"));
            assertEquals("New Trial Rules", labels.getString("DB.POWER.GTR"));
        }
    }
}
