/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

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
    public static final int AGENTE = 1;
    public static final int EMISSARIO = 2;
    public static final int MAGO = 3;
    private static final Log log = LogFactory.getLog(CenarioFacade.class);
    private static final BundleManager label = SettingsManager.getInstance().getBundleManager();

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
        int indice = (int) Math.floor(vlPericia / 10);
        if (indice > 10) {
            indice = 10;
        } else if (indice < 0) {
            indice = 0;
        }
        return (cenario.getTituloPericia(classe, indice));
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

    public Collection<TipoTropa> getTipoTropas(Cenario cenario) {
        return cenario.getTipoTropas().values();
    }

    public boolean isTropaRecruitable(Cenario cenario, Raca racaCidade, Raca racaPersonagem, TipoTropa tipoTropa) {
        final Collection<TipoTropa> tipoTropas = getTipoTropas(cenario, racaCidade, racaPersonagem);
        return tipoTropas.contains(tipoTropa);
    }

    public Collection<TipoTropa> getTipoTropas(Cenario cenario, Raca racaCidade, Raca racaPersonagem) {
        Set<TipoTropa> tropas = new TreeSet<TipoTropa>();
        if (racaCidade == racaPersonagem) {
            //se racaCidade == racaPersonagem, entao lista especial da raca (=todos)
            tropas.addAll(racaPersonagem.getTropas().keySet());
        } else {
            //senao, lista a combinacao/intersecao das tropas regulares das duas racas
            final SortedMap<TipoTropa, Integer> tropasCidade = racaCidade.getTropas();
            for (TipoTropa tpTropa : tropasCidade.keySet()) {
                if (tropasCidade.get(tpTropa) == 0) {
                    //regular
                    tropas.add(tpTropa);
                }
            }
            final SortedMap<TipoTropa, Integer> tropasPersonagem = racaPersonagem.getTropas();
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
        return !cenario.isSW();
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
}
