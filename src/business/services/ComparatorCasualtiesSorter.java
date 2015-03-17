/*
 * To change este template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.services;

import java.util.Comparator;
import model.Pelotao;
import model.Terreno;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public class ComparatorCasualtiesSorter implements Comparator {

    private static final Log log = LogFactory.getLog(ComparatorCasualtiesSorter.class);
    private final int tatica;
    private final Terreno terreno;

    /*
     Charge	Attack	Fast	Defense	Cost	Id
     Standard	%	%	%	%	Id
     Flank	Defense	Attack	Cost	Fast	Id
     Ambush	Cost	Attack	Fast	Defense	Id
     Surround	Fast	Cost	Defense	Attack	Id
     Guerrilla	Slow	Defense	Attack	Cost	Id

     Cost	1	10	Upkeep or recruit?
     Attack	10	1	
     Defense	10	1	
     Fast	10	1	
     Slow	1	10	
     */
    public ComparatorCasualtiesSorter(int aTatica, Terreno aTerreno) {
        this.tatica = aTatica;
        this.terreno = aTerreno;
    }

    @Override
    public int compare(Object a, Object b) {
        if (a instanceof Pelotao) {
            return compareToByTactic(((Pelotao) a).getTipoTropa(), ((Pelotao) b).getTipoTropa(), tatica, terreno);
        } else {
            return compareToByTactic((TipoTropa) a, (TipoTropa) b, tatica, terreno);
        }
    }

    private int compareToByTactic(TipoTropa este, TipoTropa outro, int tatica, Terreno terreno) {
        if (tatica == 0) {
            //charge
            return compareToByTacticCharge(este, outro, terreno);
        } else if (tatica == 1) {
            //flank
            return compareToByTacticFlank(este, outro, terreno);
        } else if (tatica == 2) {
            //standard
            return compareToByTacticStandard(este, outro, terreno);
        } else if (tatica == 3) {
            //surround
            return compareToByTacticSurround(este, outro, terreno);
        } else if (tatica == 4) {
            //guerrila
            return compareToByTacticGuerrilla(este, outro, terreno);
        } else if (tatica == 5) {
            //ambush
            return compareToByTacticAmbush(este, outro, terreno);
        } else {
            //standard
            return compareToByTacticStandard(este, outro, terreno);
        }
    }

    /**
     * Charge	Attack	Fast	Defense	Cost	Id
     */
    private int compareToByTacticCharge(TipoTropa este, TipoTropa outro, Terreno terreno) {
        //agressive 10>1
        if (este.getAtaqueTerreno().get(terreno) - outro.getAtaqueTerreno().get(terreno) != 0) {
            return (outro.getAtaqueTerreno().get(terreno) - este.getAtaqueTerreno().get(terreno));
        }
        //fast
        if (este.getMovimentoTerreno().get(terreno) - outro.getMovimentoTerreno().get(terreno) != 0) {
            return (outro.getMovimentoTerreno().get(terreno) - este.getMovimentoTerreno().get(terreno));
        }
        //defensive/tank 10>1
        if (este.getDefesaTerreno().get(terreno) - outro.getDefesaTerreno().get(terreno) != 0) {
            return (outro.getDefesaTerreno().get(terreno) - este.getDefesaTerreno().get(terreno));
        }
        //Gold
        final Integer custoThis = este.getRecruitCostMoney() + este.getUpkeepMoney() * 5;
        final Integer custoOutro = outro.getRecruitCostMoney() + outro.getUpkeepMoney() * 5;
        if (custoThis - custoOutro != 0) {
            return (custoThis - custoOutro);
        }
        //Id
        return (este.getId() - outro.getId());
    }

    /**
     * Flank	Defense	Attack	Cost	Fast	Id
     */
    private int compareToByTacticFlank(TipoTropa este, TipoTropa outro, Terreno terreno) {
        //defensive/tank 10>1
        if (este.getDefesaTerreno().get(terreno) - outro.getDefesaTerreno().get(terreno) != 0) {
            return (outro.getDefesaTerreno().get(terreno) - este.getDefesaTerreno().get(terreno));
        }
        //agressive 10>1
        if (este.getAtaqueTerreno().get(terreno) - outro.getAtaqueTerreno().get(terreno) != 0) {
            return (outro.getAtaqueTerreno().get(terreno) - este.getAtaqueTerreno().get(terreno));
        }
        //Gold
        final Integer custoThis = este.getRecruitCostMoney() + este.getUpkeepMoney() * 5;
        final Integer custoOutro = outro.getRecruitCostMoney() + outro.getUpkeepMoney() * 5;
        if (custoThis - custoOutro != 0) {
            return (custoThis - custoOutro);
        }
        //fast
        if (este.getMovimentoTerreno().get(terreno) - outro.getMovimentoTerreno().get(terreno) != 0) {
            return (outro.getMovimentoTerreno().get(terreno) - este.getMovimentoTerreno().get(terreno));
        }
        //Id
        return (este.getId() - outro.getId());
    }

    /**
     * Ambush	Cost	Attack	Fast	Defense	Id
     */
    private int compareToByTacticAmbush(TipoTropa este, TipoTropa outro, Terreno terreno) {
        //Gold
        final Integer custoThis = este.getRecruitCostMoney() + este.getUpkeepMoney() * 5;
        final Integer custoOutro = outro.getRecruitCostMoney() + outro.getUpkeepMoney() * 5;
        if (custoThis - custoOutro != 0) {
            return (custoThis - custoOutro);
        }
        //agressive 10>1
        if (este.getAtaqueTerreno().get(terreno) - outro.getAtaqueTerreno().get(terreno) != 0) {
            return (outro.getAtaqueTerreno().get(terreno) - este.getAtaqueTerreno().get(terreno));
        }
        //fast
        if (este.getMovimentoTerreno().get(terreno) - outro.getMovimentoTerreno().get(terreno) != 0) {
            return (outro.getMovimentoTerreno().get(terreno) - este.getMovimentoTerreno().get(terreno));
        }
        //defensive/tank 10>1
        if (este.getDefesaTerreno().get(terreno) - outro.getDefesaTerreno().get(terreno) != 0) {
            return (outro.getDefesaTerreno().get(terreno) - este.getDefesaTerreno().get(terreno));
        }
        //Id
        return (este.getId() - outro.getId());
    }

    /**
     * Surround	Fast	Cost	Defense	Attack	Id
     */
    private int compareToByTacticSurround(TipoTropa este, TipoTropa outro, Terreno terreno) {
        //fast
        if (este.getMovimentoTerreno().get(terreno) - outro.getMovimentoTerreno().get(terreno) != 0) {
            return (outro.getMovimentoTerreno().get(terreno) - este.getMovimentoTerreno().get(terreno));
        }
        //Gold
        final Integer custoThis = este.getRecruitCostMoney() + este.getUpkeepMoney() * 5;
        final Integer custoOutro = outro.getRecruitCostMoney() + outro.getUpkeepMoney() * 5;
        if (custoThis - custoOutro != 0) {
            return (custoThis - custoOutro);
        }
        //defensive/tank 10>1
        if (este.getDefesaTerreno().get(terreno) - outro.getDefesaTerreno().get(terreno) != 0) {
            return (outro.getDefesaTerreno().get(terreno) - este.getDefesaTerreno().get(terreno));
        }
        //agressive 10>1
        if (este.getAtaqueTerreno().get(terreno) - outro.getAtaqueTerreno().get(terreno) != 0) {
            return (outro.getAtaqueTerreno().get(terreno) - este.getAtaqueTerreno().get(terreno));
        }
        //Id
        return (este.getId() - outro.getId());
    }

    /**
     * Standard	%	%	%	%	Id
     */
    private int compareToByTacticStandard(TipoTropa este, TipoTropa outro, Terreno terreno) {
        //Id
        return (este.getId() - outro.getId());
    }

    /**
     * Guerrilla	Slow	Defense	Attack	Cost	Id
     */
    private int compareToByTacticGuerrilla(TipoTropa este, TipoTropa outro, Terreno terreno) {
        //Slow   Slow  Slow
        if (este.getMovimentoTerreno().get(terreno) - outro.getMovimentoTerreno().get(terreno) != 0) {
            return (este.getMovimentoTerreno().get(terreno) - outro.getMovimentoTerreno().get(terreno));
        }
        //defensive/tank 10>1
        if (este.getDefesaTerreno().get(terreno) - outro.getDefesaTerreno().get(terreno) != 0) {
            return (outro.getDefesaTerreno().get(terreno) - este.getDefesaTerreno().get(terreno));
        }
        //agressive 10>1
        if (este.getAtaqueTerreno().get(terreno) - outro.getAtaqueTerreno().get(terreno) != 0) {
            return (outro.getAtaqueTerreno().get(terreno) - este.getAtaqueTerreno().get(terreno));
        }
        //Gold
        final Integer custoThis = este.getRecruitCostMoney() + este.getUpkeepMoney() * 5;
        final Integer custoOutro = outro.getRecruitCostMoney() + outro.getUpkeepMoney() * 5;
        if (custoThis - custoOutro != 0) {
            return (custoThis - custoOutro);
        }
        //Id
        return (este.getId() - outro.getId());
    }

    private int compareToByCost(TipoTropa este, TipoTropa outro, Terreno terreno) {
        //Gold
        final Integer custoThis = este.getRecruitCostMoney() + este.getUpkeepMoney() * 5;
        final Integer custoOutro = outro.getRecruitCostMoney() + outro.getUpkeepMoney() * 5;
        if (custoThis - custoOutro != 0) {
            return (custoThis - custoOutro);
        }
        //Id
        return (este.getId() - outro.getId());
    }
}
