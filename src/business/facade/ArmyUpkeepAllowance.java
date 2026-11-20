package business.facade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import model.Pelotao;

/** Allocates troop upkeep allowances across a nation's armies without changing their order. */
public final class ArmyUpkeepAllowance {
    private ArmyUpkeepAllowance() { }

    /**
     * Cheapest upkeep per unit first; recruitment cost and input order are irrelevant.
     * Code, then quantity break ties, including the legacy partial-platoon precedence case.
     * Callers supply only their nation's platoons and zero for allowances the nation lacks.
     * The legacy Bloodrider allowance retains precedence: a platoon using it is not also
     * covered by the fast-troop allowance, even when only partly covered (historical rule).
     */
    public static int discount(Collection<Pelotao> platoons, int freeBloodriders, int freeFastTroops) {
        if (freeBloodriders <= 0 && freeFastTroops <= 0) {
            return 0;
        }
        List<Pelotao> ordered = new ArrayList<>(platoons);
        ordered.sort(Comparator.comparingInt((Pelotao p) -> p.getTipoTropa().getUpkeepMoney())
                .thenComparing(Pelotao::getCodigo).thenComparingInt(Pelotao::getQtd));
        int discount = 0;
        for (Pelotao platoon : ordered) {
            if (freeBloodriders <= 0 && freeFastTroops <= 0) {
                break;
            }
            if (platoon.getQtd() <= 0) {
                continue;
            }
            if (freeBloodriders > 0 && platoon.getCodigo().contains("cpblood")) {
                int covered = Math.min(freeBloodriders, platoon.getQtd());
                discount += covered * platoon.getTipoTropa().getUpkeepMoney();
                freeBloodriders -= covered;
                continue;
            }
            if (freeFastTroops > 0 && platoon.getTipoTropa().isFastMovement()) {
                int covered = Math.min(freeFastTroops, platoon.getQtd());
                discount += covered * platoon.getTipoTropa().getUpkeepMoney();
                freeFastTroops -= covered;
            }
        }
        return discount;
    }
}
