package business.facade;

import model.Partida;

/**
 * Reading a game's flags the way the Judge reads them: the game OR its scenario.
 *
 * <h3>Why this exists</h3>
 *
 * {@code Partida.hasHabilidade} is flat - it looks only in the game's own map. The Judge never asks
 * it that way: {@code PartidaControl.hasHabilidade} is
 * {@code partida.hasHabilidade(cd) || partida.getCenario().hasHabilidade(cd)}, so a flag declared by
 * the SCENARIO counts as if the game declared it. A scenario is where most of these actually live.
 *
 * Asking the model directly therefore answers "no" for a flag that is really on, silently, and the
 * client then behaves differently from the Judge for the same game. Live example, GoT12c_901: the
 * game's own flags are {@code ;GF1; ;GLA; ;GND; ;GSP;} while its scenario carries thirty-odd more.
 *
 * <h3>Values follow the same rule, game first</h3>
 *
 * {@code PartidaControl.getHabilidadeValor} gives the GAME priority and falls back to the scenario,
 * so a game can override a scenario's number. Mirrored here rather than reinvented.
 */
public class PartidaFacade {

    /** Is this flag on, in the game or in its scenario? */
    public boolean hasHabilidade(Partida partida, String cdHabilidade) {
        if (partida == null) {
            return false;
        }
        if (partida.hasHabilidade(cdHabilidade)) {
            return true;
        }
        return partida.getCenario() != null && partida.getCenario().hasHabilidade(cdHabilidade);
    }

    /** The flag's value, game first then scenario, or 0 when neither carries it. */
    public int getHabilidadeValor(Partida partida, String cdHabilidade) {
        if (partida == null) {
            return 0;
        }
        if (partida.hasHabilidade(cdHabilidade)) {
            return partida.getHabilidadeValor(cdHabilidade);
        }
        return partida.getCenario() == null ? 0
                : partida.getCenario().getHabilidadeValor(cdHabilidade);
    }

    /**
     * Death Match: every nation is hostile to every other by construction and diplomacy is disabled.
     *
     * Deliberately NOT {@code Partida.isDeathMatch()}, which reads the game's own map only and would
     * miss a scenario that declares {@code ;GDM;}.
     */
    public boolean isDeathMatch(Partida partida) {
        return hasHabilidade(partida, ";GDM;");
    }

    /** Locked teams: alliances are fixed for the game and cannot be renegotiated. */
    public boolean isTeamLocked(Partida partida) {
        return hasHabilidade(partida, ";GLA;");
    }

    /**
     * Teams with a Lord: locked teams where one nation of each team outranks the others.
     *
     * Its own type rather than a variation on {@link #isTeamLocked}, because the Judge treats it as
     * one: {@code NacaoControl.doCarregaRelacionamentosFresh} tests it FIRST and its comment says it
     * "superseeds LockedAlliances and BattleRoyale". Inside a team it grades the relationship
     * Vassal/Lord instead of a flat Ally, and the grade is the number
     * {@code NacaoFacade.getBonusRelacionamento} reads.
     */
    public boolean isTeamWithLord(Partida partida) {
        return hasHabilidade(partida, ";GSL;");
    }

    /**
     * Diplomatic actions are disabled, so whatever relationships the game started with are the
     * relationships it ends with.
     *
     * This is the flag that turns a starting arrangement into a RULE. Every closed game type carries
     * it - {@code ConverterFactory.getGameType} emits {@code ;GLA;;GND;} for Team, Hidden and Iron,
     * and {@code ;GDM;;GND;} for Death Match and Gun Boat - and the open ones (FFA, Battle Royale)
     * deliberately do not. Without it a team flag describes only turn zero and the table can drift
     * away from it.
     */
    public boolean isDiplomacyDisabled(Partida partida) {
        return hasHabilidade(partida, ";GND;");
    }

    /** Free for all: diplomacy floats, and this is the game type that leaves the most unknown. */
    public boolean isFreeForAll(Partida partida) {
        return hasHabilidade(partida, ";FFA;");
    }

    public boolean isBattleRoyal(Partida partida) {
        return hasHabilidade(partida, ";GBR;");
    }
}
