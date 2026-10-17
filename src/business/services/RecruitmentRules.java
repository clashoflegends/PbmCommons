package business.services;

import baseLib.BaseModel;
import model.TipoTropa;
import model.Partida;

/** Pure recruitment inputs shared by execution and the read-only order preview. */
public final class RecruitmentRules {
    private RecruitmentRules() { }

    public static int citySlots(int size, boolean sameCulture, BaseModel nation) {
        int slots = size * 100;
        float base = size * 100;
        if (sameCulture && nation.hasHabilidade(";PRT;")) slots += nation.getHabilidadeValor(";PRT;");
        if (sameCulture && nation.hasHabilidade(";NRE;")) slots += nation.getHabilidadeValor(";NRE;");
        if (nation.hasHabilidade(";TCS3;")) {
            slots += base * (float) nation.getHabilidadeValor(";TCS3;") / 100f - base;
        } else if (nation.hasHabilidade(";TCS2;")) {
            slots += base * (float) nation.getHabilidadeValor(";TCS2;") / 100f - base;
        }
        return slots;
    }

    /** Per-order ceiling is separate from the shared city's remaining slot pool. */
    public static boolean hasRule(Partida game, String token) {
        return game.hasHabilidade(token) || game.getCenario() != null && game.getCenario().hasHabilidade(token);
    }

    public static int ruleValue(Partida game, String token) {
        return game.hasHabilidade(token) ? game.getHabilidadeValor(token)
                : game.getCenario() == null ? 0 : game.getCenario().getHabilidadeValor(token);
    }

    public static int orderSlots(int remaining, Partida game, BaseModel nation) {
        if (!hasRule(game, ";GRL100;")) return remaining;
        return ruleValue(game, ";GRL100;")
                + (nation.hasHabilidade(";NRE;") ? nation.getHabilidadeValor(";NRE;") : 0);
    }

    public record Materials(int cavalryLeather, int extraLeather, int bronze, int iron,
            int steel, int food, int wood, int mounts) {
        /** Resource indices follow the existing product converter, not display names. */
        public long[] demand(int quantity) {
            return new long[]{quantity * ((long) cavalryLeather + extraLeather),
                (long) quantity * bronze, (long) quantity * iron, (long) quantity * steel,
                (long) quantity * food, (long) quantity * wood, (long) quantity * mounts};
        }
    }

    private static int first(BaseModel model, String... tokens) {
        for (String token : tokens) {
            if (model.hasHabilidade(token)) return model.getHabilidadeValor(token);
        }
        return 0;
    }

    public static boolean waivesLeatherMounts(TipoTropa troop, BaseModel nation,
            boolean capital, boolean bigCity, boolean desert) {
        return nation.hasHabilidade(";PCR;") && capital
                || nation.hasHabilidade(";PBT;") && bigCity && troop.isFastMovement()
                || nation.hasHabilidade(";PDR;") && desert && troop.isFastMovement();
    }

    public static boolean usesWood(TipoTropa troop) {
        return troop.hasHabilidade(";TTW50;") || troop.hasHabilidade(";TTW100;")
                || troop.hasHabilidade(";TTW2;") || troop.hasHabilidade(";TTW;");
    }

    public static Materials materials(TipoTropa troop, BaseModel nation, boolean newRules,
            boolean capital, boolean bigCity, boolean desert,
            boolean legacyShipHalf, boolean legacyShipThird) {
        boolean waived = waivesLeatherMounts(troop, nation, capital, bigCity, desert);
        int mounts = waived ? 0 : troop.hasHabilidade(";TTC;") || troop.hasHabilidade(";TCM;") ? 1 : 0;
        int leather = waived ? 0 : troop.hasHabilidade(";TTL;") ? 2 : troop.hasHabilidade(";TTC;") ? 1 : 0;
        int extra = waived ? 0 : first(troop, ";TEL2;", ";TEL3;");
        int wood;
        if (troop.hasHabilidade(";TTW50;") || troop.hasHabilidade(";TTW100;") || troop.hasHabilidade(";TTW2;")) {
            wood = first(troop, ";TTW50;", ";TTW100;", ";TTW2;");
        } else if (troop.hasHabilidade(";TTW;") && newRules && "wm".equalsIgnoreCase(troop.getCodigo())) {
            wood = 50;
        } else {
            wood = first(troop, ";TTW;");
        }
        // Division precedence and integer truncation are part of the rules.
        if (!usesWood(troop)) return new Materials(leather, extra, first(troop, ";TEB50;"),
                first(troop, ";TEI1000;", ";TEI200;", ";TEI10;"), first(troop, ";TES10;"),
                first(troop, ";TEF100;", ";TEF200;", ";TEF1000;", ";TEF2000;"), 0, mounts);
        if (troop.isSiege() && nation.hasHabilidade(";PCW;")) wood /= nation.getHabilidadeValor(";PCW;");
        else if (troop.isBarcos() && nation.hasHabilidade(";PSW;")) wood /= nation.getHabilidadeValor(";PSW;");
        else if (troop.isBarcos() && nation.hasHabilidade(";PSW3;")) wood /= nation.getHabilidadeValor(";PSW3;");
        else if (troop.isBarcos() && legacyShipHalf) wood /= 2;
        else if (troop.isBarcos() && legacyShipThird) wood /= 3;
        return new Materials(leather, extra, first(troop, ";TEB50;"),
                first(troop, ";TEI1000;", ";TEI200;", ";TEI10;"), first(troop, ";TES10;"),
                first(troop, ";TEF100;", ";TEF200;", ";TEF1000;", ";TEF2000;"), wood, mounts);
    }
}
