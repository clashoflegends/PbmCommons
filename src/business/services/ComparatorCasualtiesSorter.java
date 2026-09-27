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
     THE ORDER IN WHICH PLATOONS DIE. Index 0 takes casualties first.

     Rewritten 2026-09-27 to match the code. The old table had Guerrilla as "Slow, Defense, Attack,
     Cost" and the implementation has always checked ATTACK first, which is the opposite end of the
     one key that matters most - and it is the key the catapult rule players rely on turns on. The
     legend underneath it ("Cost 1 10", "Attack 10 1") did not say which direction either, so each
     key now carries its own. No behaviour was changed: this is the comment catching up with the
     code, not the other way round.

     Every comparator is the same shape - a list of keys, first difference wins, platoon id as the
     final tie-break. DESC means the HIGHEST value dies first; ASC means the lowest does.

     tactic          1st            2nd            3rd            4th           last
     ------------------------------------------------------------------------------------
     0 Charge        attack DESC    movement DESC  defence DESC   cost DESC     id
     1 Flank         defence DESC   attack DESC    cost DESC      movement DESC id
     2 Standard      (none)         -              -              -             id
     3 Surround      movement DESC  cost DESC      defence DESC   attack DESC   id
     4 Guerrilla     attack ASC     movement ASC   defence ASC    cost ASC      id
     5 Ambush        cost DESC      attack DESC    movement DESC  defence DESC  id
     6 Barrage       cost ASC       attack DESC    movement DESC  defence ASC   id
     7 Shieldwall    defence ASC    attack ASC     cost ASC       movement ASC  id
     8 Stand firm    movement ASC   cost ASC       defence ASC    attack ASC    id
     9 Swarm         attack ASC     movement ASC   defence ASC    cost ASC      id

     0-5 are the traditional set; 6-9 belong to the ;ST2; set, and 0, 1 and 3 appear in both. An
     index from the wrong family sorts by the default branch (Charge) - see T-822, which is about
     the matching hole in the tactic BONUS table.

     Attack, defence and movement are read PER TERRAIN, so the same tactic orders the same army
     differently on different ground. Cost is recruit money plus five turns of upkeep.

     Two consequences worth knowing before reading any of this as strategy advice:

     - CHARGE AND GUERRILLA ARE A MIRROR PAIR on attack. Charge spends your hardest hitters first,
       Guerrilla spends them last. That is why an army that needs its siege engines alive at the
       walls picks Guerrilla: high-attack, and therefore last in the queue.
     - STANDARD HAS NO SEQUENCE AT ALL. Its comparator is the id tie-break alone, and
       CasualtyMode.TATICA_STANDARD treats it as an even spread rather than a ranking. Choosing
       Standard is choosing not to have a casualty order.
     - SWARM AND GUERRILLA ARE IDENTICAL here. They differ only in the bonus table.
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
