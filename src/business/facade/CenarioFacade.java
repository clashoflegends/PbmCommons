/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import business.converter.ConverterFactory;
import java.io.Serializable;
import java.util.*;
import model.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author gurgel
 */
public class CenarioFacade implements Serializable {

    public static final int COMANDANTE = 0;
    public static final int ROGUE = 1;
    public static final int DIPLOMAT = 2;
    public static final int WIZARD = 3;
    private static final Log log = LogFactory.getLog(CenarioFacade.class);
    private static final BundleManager label = SettingsManager.getInstance().getBundleManager();
    private int[][] bonusTatica = new int[6][6];

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
        return cenario.getTaticas();
    }

    public int getTaticaBonus(Cenario cenario, String taticaA, String taticaB) {
        return getTaticaBonus(cenario, ConverterFactory.taticaToInt(taticaA), ConverterFactory.taticaToInt(taticaB));
    }

    public int getTaticaBonus(Cenario cenario, int taticaA, int taticaB) {
        if (this.bonusTatica[0][0] == 0) {
            setBonusTatica();
        }
        return this.bonusTatica[taticaA][taticaB];
    }

    private void setBonusTatica() {
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

    public Collection<TipoTropa> getTipoTropas(Cenario cenario) {
        return cenario.getTipoTropas().values();
    }

    public boolean isTropaRecruitable(Cenario cenario, Raca racaCidade, Raca racaPersonagem, TipoTropa tipoTropa) {
        final Collection<TipoTropa> tipoTropas = getTipoTropas(cenario, racaCidade, racaPersonagem);
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
        return !cenario.hasHabilidade(";PW;");
    }

    public boolean hasOrdensNacao(Partida partida) {
        return partida.getTurno()==0 && partida.isNationPackages();
    }
}
