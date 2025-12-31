/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import business.converter.ColorFactory;
import java.awt.Color;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import model.Cenario;
import model.Cidade;
import model.Jogador;
import model.Local;
import model.Mercado;
import model.Nacao;
import model.Personagem;
import model.Produto;
import model.Raca;
import msgs.BaseMsgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;
import utils.StringRet;

/**
 *
 * @author gurgel
 */
public class CidadeFacade implements Serializable {

    private static final Log log = LogFactory.getLog(CidadeFacade.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final NacaoFacade nacaoFacade = new NacaoFacade();
    private static final LocalFacade localFacade = new LocalFacade();
    private static final CenarioFacade cenarioFacade = new CenarioFacade();
    private static final MercadoFacade mercadoFacade = new MercadoFacade();
    private static final BattleSimFacade combatSimFacade = new BattleSimFacade();
    public static final int[] ForneceComida = {0, 100, 200, 1000, 2500, 5000};
    private final int[] producaoFator = {100, 100, 80, 60, 40, 20};

    public int getArrecadacaoImpostos(Cidade city) {
        try {
            int base = 2500;
            if (city.getNacao().hasHabilidade(";NSP;") && city.getLocal().getTerreno().isPlanicie()) {
                base = base * (100 + city.getNacao().getHabilidadeValor(";NSP;")) / 100;
            }
            if (city.getNacao().hasHabilidade(";NST;")) {
                base = base * (100 + city.getNacao().getHabilidadeValor(";NST;")) / 100;
            }
            if (city.getNacao().hasHabilidade(";NTL;")) {
                base = base * (100 + city.getNacao().getHabilidadeValor(";NTL;")) / 100;
            }
            if (city.getNacao().hasHabilidade(";NTH;") && isHeroPresent(city)) {
                base = base * (100 + city.getNacao().getHabilidadeValor(";NTH;")) / 100;
            }
            return (city.getNacao().getImpostos() * base * (Math.max(city.getTamanho() - 1, 0))) / 100;
        } catch (NullPointerException ex) {
            return 0;
        }
    }

    public boolean isHeroPresent(Cidade city) {
        Nacao nationCity = city.getNacao();
        for (Personagem pc : city.getLocal().getPersonagens().values()) {
            if (pc.getNacao() != nationCity) {
                //not same nation
                continue;
            }
            if (!pc.isHero()) {
                //not hero
                continue;
            }
            return true;
        }
        return false;
    }

    public String getCoordenadas(Cidade cidade) {
        return LocalFacade.getCoordenadas(cidade.getLocal());
    }

    public int getDocas(Cidade cidade) {
        return cidade.getDocas();
    }

    public String getDocasNome(Cidade cidade) {
        try {
            return BaseMsgs.cidadeDocas[cidade.getDocas()];
        } catch (NullPointerException e) {
            return BaseMsgs.cidadeDocas[0];
        }
    }

    public Jogador getJogador(Cidade cidade) {
        Jogador jogador = null;
        try {
            jogador = cidade.getNacao().getOwner();
        } catch (NullPointerException ex) {
            //faz nada, so retorna null
        }
        return jogador;
    }

    public int getLealdade(Cidade cidade) {
        return cidade.getLealdade();
    }

    public int getLealdadeDelta(Cidade cidade) {
        return (cidade.getLealdade() - cidade.getLealdadeAnterior());
    }

    public Local getLocal(Cidade cidade) {
        return cidade.getLocal();
    }

    public String getNacaoNome(Cidade cidade) {
        return nacaoFacade.getNome(cidade.getNacao());
    }

    public String getNome(Cidade cidade) {
        try {
            return cidade.getNome();
        } catch (NullPointerException e) {
            return "-";
        }
    }

    public String getNome(Local hex) {
        try {
            return hex.getCidade().getNome();
        } catch (NullPointerException e) {
            return "-";
        }
    }

    public String getNomeCoordenada(Cidade cidade) {
        try {
            return cidade.getComboDisplay();
        } catch (NullPointerException e) {
            return "-";
        }
    }

    public String getNomeHex(Cidade city) {
        try {
            return (city.getNome() + " (" + city.getLocal().getCoordenadas() + ")");
        } catch (NullPointerException e) {
            return "-";
        }
    }

    public int getFortificacao(Cidade cidade) {
        return cidade.getFortificacao();
    }

    public String getFortificacaoNome(Cidade cidade) {
        try {
            return BaseMsgs.cidadeFortificacao[cidade.getFortificacao()];
        } catch (Exception e) {
            return BaseMsgs.cidadeFortificacao[0];
        }
    }

    public String getFortificacaoNome(int fortificacao) {
        try {
            return BaseMsgs.cidadeFortificacao[fortificacao];
        } catch (Exception e) {
            return BaseMsgs.cidadeFortificacao[0];
        }
    }

    public int getTamanho(Cidade cidade) {
        return cidade.getTamanho();
    }

    public int getTamanho(Local local) {
        try {
            return local.getCidade().getTamanho();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    public String getTamanhoNome(Cidade cidade) {
        try {
            return BaseMsgs.cidadeTamanho[cidade.getTamanho()];
        } catch (Exception e) {
            return BaseMsgs.cidadeTamanho[0];
        }
    }

    public String getTamanhoNome(int tamanho) {
        return BaseMsgs.cidadeTamanho[tamanho];
    }

    public String getTamanhoFortificacao(Cidade cidade) {
        String ret = BaseMsgs.cidadeTamanho[cidade.getTamanho()];
        if (cidade.isFortificado()) {
            ret += "/" + BaseMsgs.cidadeFortificacao[cidade.getFortificacao()];
        }
        return ret;
    }

    public boolean isCapital(Cidade cidade) {
        return cidade.isCapital();
    }

    public boolean isFortificacao(Cidade cidade) {
        return cidade != null && cidade.getFortificacao() > 0;
    }

    public boolean isOculto(Cidade cidade) {
        return cidade.isOculto();
    }

    public boolean isSitiado(Cidade cidade) {
        return cidade.isSitiado();
    }

    public List listaPresencas(Cidade city) {
        return localFacade.listaPresencas(city.getLocal());
    }

    public Color getNacaoColorFill(Cidade cidade) {
        //FIXME: move para nacaoFacade
        try {
            if (cidade.getNacao().getFillColor() == null) {
                return ColorFactory.colorFill[SysApoio.parseInt(cidade.getNacao().getCodigo())];
            } else {
                return cidade.getNacao().getFillColor();
            }
        } catch (Exception ex) {
            return ColorFactory.colorFill[0];
        }
    }

    public Color getNacaoColorBorder(Cidade cidade) {
        //FIXME: move para nacaoFacade
        try {
            if (cidade.getNacao().getBorderColor() == null) {
                return ColorFactory.colorBorder[SysApoio.parseInt(cidade.getNacao().getCodigo())];
            } else {
                return cidade.getNacao().getBorderColor();
            }
        } catch (Exception ex) {
            return ColorFactory.colorBorder[0];
        }
    }

    public String getOculto(Cidade cidade) {
        return SysApoio.iif(cidade.isOculto(), labels.getString("SIM"), labels.getString("NAO"));
    }

    public String getCapital(Cidade cidade) {
        return SysApoio.iif(cidade.isCapital(), labels.getString("SIM"), labels.getString("NAO"));
    }

    public String getSitiado(Cidade cidade) {
        return SysApoio.iif(cidade.isSitiado(), labels.getString("SIM"), labels.getString("NAO"));
    }

    public Nacao getNacao(Cidade cidade) {
        return cidade.getNacao();
    }

    public String getRacaNome(Cidade cidade) {
        try {
            return cidade.getRaca().getNome();
        } catch (NullPointerException e) {
            try {
                return cidade.getNacao().getRaca().getNome();
            } catch (NullPointerException ex) {
                return labels.getString("NENHUM");
            }
        }
    }

    public Raca getRaca(Cidade cidade) {
        try {
            return cidade.getRaca();
        } catch (NullPointerException e) {
            try {
                return cidade.getNacao().getRaca();
            } catch (NullPointerException ex) {
                return null;
            }
        }
    }

    public int getResourceSell(Cidade city, Produto product, Mercado market, Cenario scenario, int turn) {
        final int qtProduct = getEstoque(city, product) + getProducao(city, product, scenario, turn);
        return mercadoFacade.getSell(product, qtProduct, market);
    }

    public int getResourceBestSell(Cidade cidade, Mercado mercado, Cenario cenario, int turno) {
        return mercadoFacade.getBestSellProductionAndStorage(cidade.getLocal(), mercado, cenario, turno);
    }

    public int getResourceBestSell(Local local, Mercado mercado, Cenario cenario, int turno) {
        return mercadoFacade.getBestSellProductionAndStorage(local, mercado, cenario, turno);
    }

    public int getProducao(Cidade city, Produto product, Cenario scenario, int turn) {
        return getProducaoAndBonus(city, product, scenario, turn)[0];
    }

    /**
     *
     * @param city
     * @param product
     * @param scenario
     * @param turn
     * @return {Total produced, how much of the total was a bonus}
     */
    public int[] getProducaoAndBonus(Cidade city, Produto product, Cenario scenario, int turn) {
        final Local local = city.getLocal();
        if (city.getLocal().hasHabilidade(";LIP;")) {
            //no production on plague
            return new int[]{0, 0};
        }
        int totalBonuses = 0;
        int producao = 0;
        final Nacao nation = city.getNacao();
        try {
            //base production adjusted for weather and winter
            producao = localFacade.getProductionBase(city.getLocal(), product, scenario, turn);

            if (!product.isMoney()) {
                //first reduce according to city size for everything except gold
                producao = producao * this.producaoFator[city.getTamanho()] / 100;
            }
            //calculations before minimum
            if (nation.hasHabilidade(";NSW;") && local.getClima() >= 5) {
                //Summer Production: 50% production bonus in warm or better climate
                totalBonuses += producao * nation.getHabilidadeValor(";NSW;") / 100;
                producao += producao * nation.getHabilidadeValor(";NSW;") / 100;
            }
            if (product.isWood() && nation.hasHabilidade(";NWP;")) {
                //woods increase prod by 50%
                producao += producao * nation.getHabilidadeValor(";NWP;") / 100;
            }
            if (product.isMoney() && nation.hasHabilidade(";NWG;") && localFacade.isTerrenoFloresta(local.getTerreno()) && localFacade.isCidadeFortificada(local)) {
                //fortified on woods increase gold by 50%
                producao += producao * nation.getHabilidadeValor(";NWG;") / 100;
            }
            //these are guaranteed minimum, do not stack
            if (product.isMoney() && nation.hasHabilidade(";PGH;") && localFacade.isTerrenoMontanhaColina(local.getTerreno())) {
                //se em montanha/colina e com habilidade, entao garante minimo de 500
                producao = Math.max(producao, nation.getHabilidadeValor(";PGH;"));
            } else if (product.isMoney() && nation.hasHabilidade(";PGH2;") && localFacade.isTerrenoMontanhaColina(local.getTerreno())) {
                //se em montanha/colina e com habilidade, entao garante minimo de 500
                producao = Math.max(producao, nation.getHabilidadeValor(";PGH2;"));
            } else if (product.isMoney() && nation.hasHabilidade(";PGM;") && nacaoFacade.isSameRaceCulture(nation, city)) {
                //se mesma cultura e com habilidade, entao garante minimo de 250
                producao = Math.max(producao, nation.getHabilidadeValor(";PGM;"));
            }

            //other powers that impact minimum
            if (nation.hasHabilidade(";NTR;") && isHeroPresent(city)) {
                //Epic hero presence boosts resource production by 3x
                totalBonuses += producao * nation.getHabilidadeValor(";NTR;") / 100;
                producao += producao * nation.getHabilidadeValor(";NTR;") / 100;
            } else if (nation.hasHabilidade(";NTR5;") && isHeroPresent(city)) {
                //Epic hero presence boosts resource production by 3x
                totalBonuses += producao * nation.getHabilidadeValor(";NTR5;") / 100;
                producao += producao * nation.getHabilidadeValor(";NTR5;") / 100;
            }
        } catch (NullPointerException ex) {
            producao = producao * this.producaoFator[city.getTamanho()] / 100;
        }
        return new int[]{producao, totalBonuses};
    }

    public SortedMap<Produto, Integer> getEstoques(Cidade cidade) {
        return cidade.getEstoques();
    }

    public int getEstoque(Cidade cidade, Produto produto) {
        try {
            return cidade.getEstoque(produto);
        } catch (NullPointerException e) {
            return 0;
        }
    }

    public int getDefesa(Cidade cidade) {
        return combatSimFacade.getCityDefenseBase(cidade);
    }

    public int getDefesa(int tamanho, int fortificacao, int lealdade) {
        return combatSimFacade.getCityDefense(tamanho, fortificacao, lealdade);
    }

    public boolean isAtivo(Cidade cidade) {
        return cidade.getTamanho() > 0;
    }

    public Raca getNacaoRaca(Cidade cidade) {
        return getNacao(cidade).getRaca();
    }

    public List<String> getInfoTitle(Cidade cidade) {
        StringRet ret = new StringRet();
        if (cidade == null) {
            return ret.getList();
        }
        ret.add(String.format(labels.getString("CIDADE.CAPITAL.DA.NACAO"),
                cidade.getComboDisplay(),
                SysApoio.iif(cidade.isCapital(), labels.getString("(CAPITAL)"), ""),
                nacaoFacade.getNome(cidade.getNacao())));
        ret.addTab(String.format("%s: %s", labels.getString("TAMANHO"), BaseMsgs.cidadeTamanho[cidade.getTamanho()]));
        return ret.getList();
    }

    public boolean isBigCity(Cidade cidade) {
        if (cidade == null) {
            return false;
        }
        return cidade.getTamanho() >= 4;
    }

    public int getUpkeepMoney(Cidade cidade, Cenario scenario) {
        if (scenario.hasHabilidade(";SUC;")) {
            return 0;
        } else {
            return (cidade.getDocas() * 250 + cidade.getFortificacao() * 500);
        }
    }

    public int getFoodGiven(Cidade cidade) {
        return CidadeFacade.ForneceComida[cidade.getTamanho()];
    }

    public int getCustoAmpliacao(Cidade city, int baseCost) {
        int custo = 2000;
        if (city.getNacao().hasHabilidade(";PPB;")) {
            custo = custo / city.getNacao().getHabilidadeValor(";PPB;");
        }
        return (city.getTamanho() * custo + baseCost);
    }

    public boolean isKeyCity(Cidade city) {
        try {
            return city.hasHabilidade(";LCO;");
        } catch (NullPointerException e) {
            return false;
        }
    }

    public boolean isKeyLocal(Cidade city) {
        try {
            return localFacade.isKeyLocal(city.getLocal());
        } catch (NullPointerException e) {
            return false;
        }
    }

    public int getPointsKeyCity(Cidade city) {
        if (isKeyCity(city) || isKeyLocal(city)) {
            return 1;
        } else {
            return 0;
        }
    }

    public int getPointsDomination(Local local) {
        try {
            return getPointsDomination(local.getCidade());
        } catch (NullPointerException e) {
            return 0;
        }
    }

    public int getPointsDomination(Cidade city) {
        if (!isPointsDomination(city)) {
            return 0;
        } else if (city.hasHabilidade(";LCP5;")) {
            return city.getHabilidadeValor(";LCP5;");
        } else if (city.hasHabilidade(";LCP2;")) {
            return city.getHabilidadeValor(";LCP2;");
        } else if (city.hasHabilidade(";LCP1;")) {
            return city.getHabilidadeValor(";LCP1;");
        } else {
            return 0;
        }
    }

    public boolean isPointsDomination(Cidade city) {
        return city.hasHabilidade(";LCP;");
    }

    public boolean isPorto(Cidade city) {
        return (city.getDocas() == 2);
    }

    public boolean isDocas(Cidade city) {
        return (city.getDocas() == 1);
    }

    public boolean isDocasPorto(Cidade city) {
        return (city.getDocas() > 0);
    }

    public boolean isBreeedingpit(Cidade city) {
        return city.hasHabilidade(";CTRB;");
    }

    public boolean isEaglenest(Cidade city) {
        return city.hasHabilidade(";CTRE;");
    }

    public boolean isDragonpit(Cidade city) {
        return city.hasHabilidade(";CTRR;");
    }

    /* not able to improve a town to a burg if there is already a burg 2 hex away 
     * not able to improve a burg to a metropolis if there is already a 3 hex
     * metropolis
     */
    public Local isCheckCitySizeCapToUpgrade(Cidade city, SortedMap<String, Local> listLocalCity) {
        if (city.getTamanho() < 3 || city.getTamanho() > 4) {
            //These sizes don't apply. Can upgrade. 
            return null;
        }
        if (city.isCapital()) {
            //Capitals are excluded, can always be upgraded.
            return null;
        }
        if (city.getLocal().hasHabilidade(";LKW;") && city.getNacao().hasHabilidade(";PKW;")) {
            //Night Watch can always upgrade the wall
            return null;
        }
        //Only Towns and burghs from this point on, as candidates
        int citySize;
        int range;
        if (city.getTamanho() == 3) {
            //for towns
            range = 2;
            citySize = 4;
        } else {
            //increase if burgh
            range = 3;
            citySize = 5;
        }
        //check greyjoy bonus of reduced range
        if (city.getNacao().hasHabilidade(";PIC;")) {
            range -= city.getNacao().getHabilidadeValor(";PIC;");
        }
        //check if there's another city in range
        final Set<Local> listLocalRange = localFacade.listLocalRange(city.getLocal(), range, listLocalCity);
        for (Local local : listLocalRange) {
            if (getTamanho(local) < citySize) {
                //double check if it is ok
                continue;
            }
            //at this point, it found at least one. It is enough to block.
            return local;
        }
        //found no reasons to deny.
        return null;
    }
}
