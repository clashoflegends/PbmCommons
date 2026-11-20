package business.facade;

import java.util.SortedMap;
import model.TipoTropa;

/** Resolves a troop key within an already selected roster, without loading additional types. */
public final class TroopKeyResolver {
    private TroopKeyResolver() { }

    /**
     * Preserve legacy selection: trimmed exact key first, then the first case-insensitive exact
     * or case-sensitive substring match in roster order. In particular, this is not a new
     * prefix-only rule or a longest-match rule; existing ambiguous keys retain their selection.
     */
    public static TipoTropa resolve(String code, SortedMap<String, TipoTropa> roster) {
        if (code == null) {
            return null;
        }
        final String key = code.trim();
        final TipoTropa exact = roster.get(key);
        if (exact != null) {
            return exact;
        }
        for (TipoTropa troop : roster.values()) {
            if (troop.getCodigo().equalsIgnoreCase(key) || troop.getCodigo().contains(key)) {
                return troop;
            }
        }
        return null;
    }
}
