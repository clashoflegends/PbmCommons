package business;

import business.converter.ConverterFactory;
import business.facade.LocalFacade;
import model.Local;

/** Naval admission only. Terrain costs and the ship roster are not changed here. */
public final class NavalAccess {
    private static final LocalFacade locals = new LocalFacade();

    public enum Entry {
        BLOCKED, NAVIGABLE, LANDING_STOP;
        public boolean isAllowed() { return this != BLOCKED; }
    }

    private NavalAccess() { }

    public static boolean hasLanding(Local local) {
        if (local == null || local.getTerreno() == null || local.getTerreno().isAgua()) {
            return false;
        }
        for (int side = 1; side <= 6; side++) {
            if (local.isLanding(side)) return true;
        }
        return false;
    }

    /** The destination's port boolean is supplied by adapters with a different city model. */
    public static Entry entry(Local from, Local to, int direction, int previous, boolean port) {
        if (from == null || to == null || from.getTerreno() == null || to.getTerreno() == null) {
            return Entry.BLOCKED;
        }
        int side = ConverterFactory.getDirecao(direction);
        if (legacyEntry(from, to, side, previous, port)) return Entry.NAVIGABLE;
        if (from.getTerreno().isAgua() && hasLanding(to)
                && to.isLanding(ConverterFactory.getDirecao(side + 3))) {
            return Entry.LANDING_STOP;
        }
        return Entry.BLOCKED;
    }

    private static boolean legacyEntry(Local from, Local to, int side, int previous, boolean port) {
        if (previous > 6) previous = ConverterFactory.getDirecao(previous);
        if (previous == 0 && to.getTerreno().isAgua()) return true;
        if (previous == 0 && locals.isRioLado(to, side + 3)) return true;
        if (from.getTerreno().isAgua() && (to.getTerreno().isLitoral()
                || to.getTerreno().isAncoravel() || port || to.getTerreno().isAgua()
                || locals.isRioLado(to, side + 3))) return true;
        if (locals.isRioLado(from, side) && to.getTerreno().isAgua()) return true;
        if (!locals.isRioLado(from, side) || !locals.isRioLado(to, side + 3)) return false;
        int min = Math.min(side, previous), max = Math.max(side, previous);
        if (min == max) return true;
        boolean connected = true;
        for (int i = min + 1; connected && i < max; i++) {
            connected = from.isRio(ConverterFactory.getDirecao(i));
        }
        if (!connected) {
            connected = true;
            for (int i = max + 1; connected && i < min + 6; i++) {
                connected = from.isRio(ConverterFactory.getDirecao(i));
            }
        }
        return connected;
    }
}
