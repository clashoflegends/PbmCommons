/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import baseLib.BaseModel;
import baseLib.IBaseModel;
import business.converter.ConverterFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import model.Cenario;
import model.Cidade;
import model.Local;
import model.Partida;
import model.Produto;
import model.Raca;
import model.TipoTropa;
import msgs.BaseMsgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class CenarioFacade implements Serializable {

    public static final int MINIMUM_LOYALTY = 50;
    public static final int COMANDANTE = 0;
    public static final int ROGUE = 1;
    public static final int DIPLOMAT = 2;
    public static final int WIZARD = 3;
    private static final Log log = LogFactory.getLog(CenarioFacade.class);
    private static final BundleManager label = SettingsManager.getInstance().getBundleManager();
    private final int[][] bonusTatica = new int[10][10];
    private final SortedMap<Integer, String> taticas = new TreeMap<Integer, String>();
    private String typeTatica = "-";

    public static boolean isGrecia(Cenario cenario) {
        return cenario.isGrecia();
    }

    public static boolean isPrintNome(Cenario cenario, Cidade cidade) {
        return (cenario.isSW() && cidade.isCapital());
    }

    public static boolean isPrintGoldMine(Cenario cenario, Local local) {
        try {
            return (cenario.isSW() && local.getCidade().getTamanho() == 1);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isSw(Cenario cenario) {
        return cenario.isSW();
    }

    public String getTituloPericia(Cenario cenario, int classe, int vlPericia) {
        int indice = (int) Math.floor(vlPericia / 10) + 1;
        if (vlPericia <= 0) {
            indice = 0;
        } else if (indice > 11) {
            indice = 11;
        }
        return (cenario.getTituloPericia(classe, indice));
    }

    public SortedMap<Integer, String[]> getTituloPericiaAll(Cenario cenario) {
        SortedMap<Integer, String[]> ret = new TreeMap<Integer, String[]>();
        final String[][] tituloPericia = cenario.getTituloPericiaAll();
        ret.put(COMANDANTE, tituloPericia[COMANDANTE]);
        ret.put(ROGUE, tituloPericia[ROGUE]);
        if (hasDiplomat(cenario)) {
            ret.put(DIPLOMAT, tituloPericia[DIPLOMAT]);
        }
        if (hasWizard(cenario)) {
            ret.put(WIZARD, tituloPericia[WIZARD]);
        }
        return ret;
    }

    /**
     * 0 = comandante 1 = agente 2 = emissario 3 = mago
     */
    public String getTituloClasse(Cenario cenario, int classe) {
        String ret[];
        if (cenario.isGrecia()) {
            ret = new String[]{"COMANDANTE", "AGENTE", "EMISSARIO", "MAGO"};
        } else if (cenario.isSW()) {
            ret = new String[]{"SW.KNIGHT", "DESCONHECIDO", "DESCONHECIDO", "SW.MAGO"};
        } else {
            ret = new String[]{"COMANDANTE", "AGENTE", "EMISSARIO", "MAGO"};
        }
        return ret[classe];
    }

    /**
     * 0 = all 1 = all but Money 2 = weapons 3 = armor
     *
     * @return
     */
    public Produto[] listProdutos(Cenario cenario, int filtro) {
        List<Produto> produtos = new ArrayList();
        if (filtro == 0) {
            produtos.addAll(cenario.getProdutos().values());
        } else if (filtro == 1) {
            produtos.addAll(cenario.getProdutos().values());
            produtos.remove(cenario.getMoney());
        } else if (filtro == 2) {
            produtos.addAll(getProdutosArmor(cenario));
        } else if (filtro == 3) {
            produtos.addAll(getProdutosWeapon(cenario));
        }
        return (Produto[]) produtos.toArray(new Produto[0]);
    }

    public String[][] listTaticas(Cenario cenario) {
        if (cenario.hasHabilidade(";ST1;")) {
            return BaseMsgs.taticasGb;
        } else if (cenario.hasHabilidade(";ST2;")) {
            return BaseMsgs.taticasTk;
        } else {
            return BaseMsgs.taticasGb;
        }
    }

    public IBaseModel[] listTerrains(Cenario cenario) {
        final List<IBaseModel> values = new ArrayList<IBaseModel>(cenario.getTerrenos().values());
        return values.toArray(new IBaseModel[0]);
    }

    public SortedMap<Integer, String> listTaticasAsList(Cenario cenario) {
        doLoadTaticas(cenario);
        return taticas;
    }

    public int getTaticaBonus(Cenario cenario, String taticaA, String taticaB) {
        return getTaticaBonus(cenario, ConverterFactory.taticaToInt(taticaA), ConverterFactory.taticaToInt(taticaB));
    }

    public int getTaticaBonus(Cenario cenario, int taticaA, int taticaB) {
        doLoadTaticas(cenario);
        return this.bonusTatica[taticaA][taticaB];
    }

    private void doLoadTaticas(Cenario cenario) {
        if (cenario.hasHabilidade(typeTatica)) {
            return;
        }
        if (cenario.hasHabilidade(";ST2;")) {
            setBonusTaticaBCFSSSS();
            typeTatica = ";ST2;";
        } else {
            setBonusTaticaCFSSGA();
            typeTatica = ";ST1;";
        }
        for (String[] aTatica : listTaticas(cenario)) {
            this.taticas.put(ConverterFactory.taticaToInt(aTatica[1]), aTatica[0]);
        }
    }

    private void setBonusTaticaCFSSGA() {
        //Traditional (Charge, Flank, Standard, Surround, Guerrilla, Ambush)
        this.bonusTatica[0][0] = 100;
        this.bonusTatica[0][1] = 100;
        this.bonusTatica[0][2] = 110;
        this.bonusTatica[0][3] = 100;
        this.bonusTatica[0][4] = 120;
        this.bonusTatica[0][5] = 80;

        this.bonusTatica[1][0] = 100;
        this.bonusTatica[1][1] = 100;
        this.bonusTatica[1][2] = 90;
        this.bonusTatica[1][3] = 80;
        this.bonusTatica[1][4] = 110;
        this.bonusTatica[1][5] = 120;

        this.bonusTatica[2][0] = 80;
        this.bonusTatica[2][1] = 120;
        this.bonusTatica[2][2] = 100;
        this.bonusTatica[2][3] = 100;
        this.bonusTatica[2][4] = 100;
        this.bonusTatica[2][5] = 100;

        this.bonusTatica[3][0] = 100;
        this.bonusTatica[3][1] = 120;
        this.bonusTatica[3][2] = 100;
        this.bonusTatica[3][3] = 100;
        this.bonusTatica[3][4] = 80;
        this.bonusTatica[3][5] = 100;

        this.bonusTatica[4][0] = 90;
        this.bonusTatica[4][1] = 80;
        this.bonusTatica[4][2] = 100;
        this.bonusTatica[4][3] = 120;
        this.bonusTatica[4][4] = 100;
        this.bonusTatica[4][5] = 100;

        this.bonusTatica[5][0] = 120;
        this.bonusTatica[5][1] = 80;
        this.bonusTatica[5][2] = 100;
        this.bonusTatica[5][3] = 100;
        this.bonusTatica[5][4] = 100;
        this.bonusTatica[5][5] = 100;
    }

    private void setBonusTaticaBCFSSSS() {
        //new (Barrage, Shieldwall, StandFirm, Swarm)
        this.bonusTatica[6][6] = 100;
        this.bonusTatica[6][0] = 80;
        this.bonusTatica[6][1] = 100;
        this.bonusTatica[6][7] = 110;
        this.bonusTatica[6][8] = 120;
        this.bonusTatica[6][3] = 100;
        this.bonusTatica[6][9] = 90;

        this.bonusTatica[0][6] = 120;
        this.bonusTatica[0][0] = 100;
        this.bonusTatica[0][1] = 100;
        this.bonusTatica[0][7] = 90;
        this.bonusTatica[0][8] = 80;
        this.bonusTatica[0][3] = 100;
        this.bonusTatica[0][9] = 110;

        this.bonusTatica[1][6] = 100;
        this.bonusTatica[1][0] = 100;
        this.bonusTatica[1][1] = 100;
        this.bonusTatica[1][7] = 120;
        this.bonusTatica[1][8] = 90;
        this.bonusTatica[1][3] = 110;
        this.bonusTatica[1][9] = 80;

        this.bonusTatica[7][6] = 90;
        this.bonusTatica[7][0] = 110;
        this.bonusTatica[7][1] = 80;
        this.bonusTatica[7][7] = 100;
        this.bonusTatica[7][8] = 100;
        this.bonusTatica[7][3] = 120;
        this.bonusTatica[7][9] = 100;

        this.bonusTatica[8][6] = 80;
        this.bonusTatica[8][0] = 120;
        this.bonusTatica[8][1] = 110;
        this.bonusTatica[8][7] = 100;
        this.bonusTatica[8][8] = 100;
        this.bonusTatica[8][3] = 90;
        this.bonusTatica[8][9] = 100;

        this.bonusTatica[3][6] = 100;
        this.bonusTatica[3][0] = 100;
        this.bonusTatica[3][1] = 90;
        this.bonusTatica[3][7] = 80;
        this.bonusTatica[3][8] = 110;
        this.bonusTatica[3][3] = 100;
        this.bonusTatica[3][9] = 120;

        this.bonusTatica[9][6] = 110;
        this.bonusTatica[9][0] = 90;
        this.bonusTatica[9][1] = 120;
        this.bonusTatica[9][7] = 100;
        this.bonusTatica[9][8] = 100;
        this.bonusTatica[9][3] = 80;
        this.bonusTatica[9][9] = 100;

    }

    public Collection<TipoTropa> getTipoTropas(Cenario cenario) {
        return cenario.getTipoTropas().values();
    }

    public boolean isTropaRecruitable(Cenario cenario, Raca racaCidade, Raca racaActor, TipoTropa tipoTropa) {
        final Collection<TipoTropa> tipoTropas = getTipoTropas(cenario, racaCidade, racaActor);
        return tipoTropas.contains(tipoTropa);
    }

    public Collection<TipoTropa> getTipoTropas(Cenario cenario, Raca racaCidade, Raca racaNacao) {
        Set<TipoTropa> tropas = new TreeSet<TipoTropa>();
        if (racaCidade == racaNacao) {
            //se racaCidade == racaPersonagem, entao lista especial da raca (=todos)
            tropas.addAll(racaNacao.getTropas().keySet());
        } else {
            //senao, lista a combinacao/intersecao das tropas regulares das duas racas
            final SortedMap<TipoTropa, Integer> tropasCidade = racaCidade.getTropas();
            for (TipoTropa tpTropa : tropasCidade.keySet()) {
                if (tropasCidade.get(tpTropa) == 0) {
                    //regular
                    tropas.add(tpTropa);
                }
            }
            final SortedMap<TipoTropa, Integer> tropasPersonagem = racaNacao.getTropas();
            for (TipoTropa tpTropa : tropasPersonagem.keySet()) {
                if (tropasPersonagem.get(tpTropa) == 0) {
                    //regular
                    tropas.add(tpTropa);
                }
            }
        }
        return tropas;
    }

    private List getProdutosArmor(Cenario cenario) {
        List<Produto> ret = new ArrayList();
        ret.add(this.getProdutoArmorDefault());
        for (Produto produto : cenario.getProdutos().values()) {
            if (produto.isArmor()) {
                ret.add(produto);
            }
        }
        return ret;
    }

    private List getProdutosWeapon(Cenario cenario) {
        List ret = new ArrayList();
        ret.add(this.getProdutoWeaponDefault());
        for (Produto produto : cenario.getProdutos().values()) {
            if (produto.isWeapon()) {
                ret.add(produto);
            }
        }
        return ret;
    }

    private Produto getProdutoArmorDefault() {
        Produto ret = new Produto();
        ret.setArmor(true);
        ret.setCodigo("no");
        ret.setNome(label.getString("NENHUM"));
        return ret;
    }

    private Produto getProdutoWeaponDefault() {
        Produto ret = new Produto();
        ret.setWeapon(true);
        ret.setCodigo("no");
        ret.setNome(label.getString("IMPROVISADA"));
        return ret;
    }

    public boolean hasOrdensCidade(Cenario cenario) {
        return cenario.hasHabilidade(";SOC;");
    }

    public boolean hasRenamePersonagens(Cenario cenario) {
        return cenario.hasHabilidade(";SRP;");
    }

    public boolean hasRenameCities(Cenario cenario) {
        return cenario.hasHabilidade(";SRC;");
    }

    public boolean hasResourceManagement(Cenario cenario) {
        return !cenario.hasHabilidade(";SNR;");
    }

    public boolean hasShips(Cenario cenario) {
        return cenario.hasHabilidade(";TB;");
    }

    public boolean hasLockedAlliances(Cenario cenario) {
        return cenario.hasHabilidade(";LOA;");
    }

    public boolean hasLordVasalAlliances(Cenario cenario) {
        return cenario.hasHabilidade(";SVS;");
    }

    public boolean hasDiplomat(Cenario cenario) {
        return cenario.hasHabilidade(";PE;");
    }

    public boolean hasWizard(Cenario cenario) {
        return cenario.hasHabilidade(";PW;");
    }

    public boolean hasOrdensNacao(Partida partida) {
        return partida.getTurno() == 0 && partida.isNationPackages();
    }

    public boolean hasOrdens(Partida partida, BaseModel actor) {
        if (actor.isPersonagem()) {
            return true;
        } else if (actor.isCidade() && hasOrdensCidade(partida.getCenario())) {
            return true;
        } else if (actor.isNacao() && hasOrdensNacao(partida)) {
            return true;
        }
        return false;
    }
}
