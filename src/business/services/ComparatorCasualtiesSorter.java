/*
 * To change este template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.services;

import java.util.Comparator;
import java.util.SortedMap;
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
     Standard	%	%	%	%	Id
     Charge	Attack	Fast	Defense	Cost	Id
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
            return compareToByTactic(((Pelotao) a).getTipoTropa(), ((Pelotao) b).getTipoTropa());
        } else {
            return compareToByTactic((TipoTropa) a, (TipoTropa) b);
        }
    }

    private int compareToByTactic(TipoTropa este, TipoTropa outro) {
        if (tatica == 0) {
            //charge
            return compareToByTacticCharge(este, outro);
        } else if (tatica == 1) {
            //flank
            return compareToByTacticFlank(este, outro);
        } else if (tatica == 2) {
            //standard
            return compareToByTacticStandard(este, outro);
        } else if (tatica == 3) {
            //surround
            return compareToByTacticSurround(este, outro);
        } else if (tatica == 4) {
            //guerrila
            return compareToByTacticGuerrilla(este, outro);
        } else if (tatica == 5) {
            //ambush
            return compareToByTacticAmbush(este, outro);
        } else if (tatica == 6) {
            //Barrage
            return compareToByTacticBarrage(este, outro);
        } else if (tatica == 7) {
            //Shield wall
            return compareToByTacticShieldwall(este, outro);
        } else if (tatica == 8) {
            //Stand firm
            return compareToByTacticStandfirm(este, outro);
        } else if (tatica == 9) {
            //Swarm
            return compareToByTacticSwarm(este, outro);
        } else {
            //standard
            return compareToByTacticCharge(este, outro);
        }
    }

    private int compareToByTacticCharge(TipoTropa este, TipoTropa outro) {
        //agressive 10>1
        final int attack = getAtaqueTerrenoDesc(este, outro, terreno);
        if (attack != 0) {
            return attack;
        }
        //fast
        final int movement = getMovimentoTerrenoDesc(este, outro, terreno);
        if (movement != 0) {
            return movement;
        }
        //defensive/tank 10>1
        final int defense = getDefesaTerrenoDesc(este, outro, terreno);
        if (defense != 0) {
            return defense;
        }
        //Gold
        final int gold = getCostDesc(este, outro);
        if (gold != 0) {
            return gold;
        }
        //Id
        return (este.getId() - outro.getId());
    }

    private int compareToByTacticFlank(TipoTropa este, TipoTropa outro) {
        //defensive/tank 10>1
        final int defense = getDefesaTerrenoDesc(este, outro, terreno);
        if (defense != 0) {
            return defense;
        }
        //agressive 10>1
        final int attack = getAtaqueTerrenoDesc(este, outro, terreno);
        if (attack != 0) {
            return attack;
        }
        //Gold
        final int gold = getCostDesc(este, outro);
        if (gold != 0) {
            return gold;
        }
        //fast
        final int movement = getMovimentoTerrenoDesc(este, outro, terreno);
        if (movement != 0) {
            return movement;
        }
        //Id
        return (este.getId() - outro.getId());
    }

    private int compareToByTacticAmbush(TipoTropa este, TipoTropa outro) {
        //Gold
        final int gold = getCostDesc(este, outro);
        if (gold != 0) {
            return gold;
        }
        //agressive 10>1
        final int attack = getAtaqueTerrenoDesc(este, outro, terreno);
        if (attack != 0) {
            return attack;
        }
        //fast
        final int movement = getMovimentoTerrenoDesc(este, outro, terreno);
        if (movement != 0) {
            return movement;
        }
        //defensive/tank 10>1
        final int defense = getDefesaTerrenoDesc(este, outro, terreno);
        if (defense != 0) {
            return defense;
        }
        //Id
        return (este.getId() - outro.getId());
    }

    private int compareToByTacticSurround(TipoTropa este, TipoTropa outro) {
        //fast
        final int movement = getMovimentoTerrenoDesc(este, outro, terreno);
        if (movement != 0) {
            return movement;
        }
        //Gold
        final int gold = getCostDesc(este, outro);
        if (gold != 0) {
            return gold;
        }
        //defensive/tank 10>1
        final int defense = getDefesaTerrenoDesc(este, outro, terreno);
        if (defense != 0) {
            return defense;
        }

        //agressive 10>1
        final int attack = getAtaqueTerrenoDesc(este, outro, terreno);
        if (attack != 0) {
            return attack;
        }
        //Id
        return (este.getId() - outro.getId());
    }

    private int compareToByTacticGuerrilla(TipoTropa este, TipoTropa outro) {
        //agressive 10>1
        final int attack = getAtaqueTerrenoAsc(este, outro, terreno);
        if (attack != 0) {
            return attack;
        }
        //slow
        final int movement = getMovimentoTerrenoAsc(este, outro, terreno);
        if (movement != 0) {
            return movement;
        }
        //defensive/tank 10>1
        final int defense = getDefesaTerrenoAsc(este, outro, terreno);
        if (defense != 0) {
            return defense;
        }
        //Gold
        final int gold = getCostAsc(este, outro);
        if (gold != 0) {
            return gold;
        }
        //Id
        return (este.getId() - outro.getId());
    }

    private int compareToByTacticBarrage(TipoTropa este, TipoTropa outro) {
        //Gold
        final int gold = getCostAsc(este, outro);
        if (gold != 0) {
            return gold;
        }
        //agressive 10>1
        final int attack = getAtaqueTerrenoDesc(este, outro, terreno);
        if (attack != 0) {
            return attack;
        }
        //fast
        final int movement = getMovimentoTerrenoDesc(este, outro, terreno);
        if (movement != 0) {
            return movement;
        }
        //defensive/tank 10>1
        final int defense = getDefesaTerrenoAsc(este, outro, terreno);
        if (defense != 0) {
            return defense;
        }
        //Id
        return (este.getId() - outro.getId());
    }

    private int compareToByTacticShieldwall(TipoTropa este, TipoTropa outro) {
        //defensive/tank 10>1
        final int defense = getDefesaTerrenoAsc(este, outro, terreno);
        if (defense != 0) {
            return defense;
        }
        //agressive 10>1
        final int attack = getAtaqueTerrenoAsc(este, outro, terreno);
        if (attack != 0) {
            return attack;
        }
        //Gold
        final int gold = getCostAsc(este, outro);
        if (gold != 0) {
            return gold;
        }
        //fast
        final int movement = getMovimentoTerrenoAsc(este, outro, terreno);
        if (movement != 0) {
            return movement;
        }
        //Id
        return (este.getId() - outro.getId());
    }

    private int compareToByTacticStandfirm(TipoTropa este, TipoTropa outro) {
        //fast
        final int movement = getMovimentoTerrenoAsc(este, outro, terreno);
        if (movement != 0) {
            return movement;
        }
        //Gold
        final int gold = getCostAsc(este, outro);
        if (gold != 0) {
            return gold;
        }
        //defensive/tank 10>1
        final int defense = getDefesaTerrenoAsc(este, outro, terreno);
        if (defense != 0) {
            return defense;
        }
        //agressive 10>1
        final int attack = getAtaqueTerrenoAsc(este, outro, terreno);
        if (attack != 0) {
            return attack;
        }
        //Id
        return (este.getId() - outro.getId());
    }

    private int compareToByTacticSwarm(TipoTropa este, TipoTropa outro) {
        //agressive 10>1
        final int attack = getAtaqueTerrenoAsc(este, outro, terreno);
        if (attack != 0) {
            return attack;
        }
        //fast
        final int movement = getMovimentoTerrenoAsc(este, outro, terreno);
        if (movement != 0) {
            return movement;
        }
        //defensive/tank 10>1
        final int defense = getDefesaTerrenoAsc(este, outro, terreno);
        if (defense != 0) {
            return defense;
        }
        //Gold
        final int gold = getCostAsc(este, outro);
        if (gold != 0) {
            return gold;
        }
        //Id
        return (este.getId() - outro.getId());
    }

    /**
     * Standard	%	%	%	%	Id
     */
    private int compareToByTacticStandard(TipoTropa este, TipoTropa outro) {
        //Id
        return (este.getId() - outro.getId());
    }

    private static int getAtaqueTerrenoDesc(TipoTropa este, TipoTropa outro, Terreno terreno) {
        return outro.getAtaqueTerreno().get(terreno) - este.getAtaqueTerreno().get(terreno);
    }

    private static int getAtaqueTerrenoAsc(TipoTropa este, TipoTropa outro, Terreno terreno) {
        return este.getAtaqueTerreno().get(terreno) - outro.getAtaqueTerreno().get(terreno);
    }

    private static int getDefesaTerrenoDesc(TipoTropa este, TipoTropa outro, Terreno terreno) {
        return outro.getDefesaTerreno().get(terreno) - este.getDefesaTerreno().get(terreno);
    }

    private static int getDefesaTerrenoAsc(TipoTropa este, TipoTropa outro, Terreno terreno) {
        return este.getDefesaTerreno().get(terreno) - outro.getDefesaTerreno().get(terreno);
    }

    private static int getMovimentoTerrenoDesc(TipoTropa este, TipoTropa outro, Terreno terreno) {
        try {
            return outro.getMovimentoTerreno().get(terreno) - este.getMovimentoTerreno().get(terreno);
        } catch (NullPointerException e) {
            final SortedMap<Terreno, Integer> OmovimentoTerreno = outro.getMovimentoTerreno();
            final SortedMap<Terreno, Integer> EmovimentoTerreno = este.getMovimentoTerreno();
            return OmovimentoTerreno.get(terreno) - EmovimentoTerreno.get(terreno);
            
        }
    }

    private static int getMovimentoTerrenoAsc(TipoTropa este, TipoTropa outro, Terreno terreno) {
        return este.getMovimentoTerreno().get(terreno) - outro.getMovimentoTerreno().get(terreno);
    }

    private static int getCostDesc(TipoTropa este, TipoTropa outro) {
        final Integer custoThis = este.getRecruitCostMoney() + este.getUpkeepMoney() * 5;
        final Integer custoOutro = outro.getRecruitCostMoney() + outro.getUpkeepMoney() * 5;
        return (custoThis - custoOutro);
    }

    private static int getCostAsc(TipoTropa este, TipoTropa outro) {
        final Integer custoThis = este.getRecruitCostMoney() + este.getUpkeepMoney() * 5;
        final Integer custoOutro = outro.getRecruitCostMoney() + outro.getUpkeepMoney() * 5;
        return (custoOutro - custoThis);
    }
}
