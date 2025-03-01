/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import baseLib.BaseModel;
import business.converter.ConverterFactory;
import business.interfaces.IActor;
import business.services.ComparatorFactory;
import business.converter.TitleFactory;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import model.ActorAction;
import model.Cenario;
import model.Cidade;
import model.Jogador;
import model.Nacao;
import model.Ordem;
import model.Partida;
import model.Personagem;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 *
 * @author gurgel
 */
public class OrdemFacade implements Serializable {

    private static final Log log = LogFactory.getLog(OrdemFacade.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final PersonagemFacade personagemFacade = new PersonagemFacade();
    private static final CidadeFacade cidadeFacade = new CidadeFacade();
    private static final NacaoFacade nacaoFacade = new NacaoFacade();
    private static final LocalFacade localFacade = new LocalFacade();
    private static final CenarioFacade cenarioFacade = new CenarioFacade();
    private static final AcaoFacade acaoFacade = new AcaoFacade();
    private static final String[] ACTIONDISABLEDARRAY = new String[]{ActorAction.ACTION_DISABLED, "", ""};
    private static final String[] ACTIONBLANK = new String[]{ActorAction.ACTION_BLANK, "", ""};

    public Ordem[] getOrdensDisponiveis(SortedMap<String, Ordem> ordens, BaseModel actor, int indexOrdemAtiva, boolean isAllOrders, boolean isStartupPackages) {
        //cria vetor e garante tamanho minimo
        List<Ordem> ordensActor = new ArrayList<>(ordens.size() + 1);
        //lista as ordens que o personagem pode executar
        for (Ordem ordem : ordens.values()) {
            //se o actor tem a pericia para dar a ordem
            if (isOrdemActor(actor, ordem)) {
                //ou sao todas as ordens, ou sao as ordens que ele pode no momento
                if (isAllOrders) {
                    ordensActor.add(ordem);
                } else if (isOrdemRequisitos(actor, ordem, isStartupPackages)) {
                    if (isOrdemAllowed(actor, indexOrdemAtiva, ordem)) {
                        ordensActor.add(ordem);
                    }
                } else {
                    //else nao pode fazer a ordem.
                }
            }
        }
        ComparatorFactory.getComparatorOrdemSorter(ordensActor);
        //Collections.sort(ordensPersonagem, ComparatorFactory.getComparatorOrdemSorter());
        return (Ordem[]) ordensActor.toArray(new Ordem[0]);
    }

    private boolean isOrdemCompativel(Ordem ordem, Ordem ordemOutra) {
        if (ordem == null || ordemOutra == null) {
            return true;
        }
        //nao pode duas de movimentacao
        if (ordem.isTipoMovimentacao() && ordemOutra.isTipoMovimentacao()) {
            return false;
        }
        //nao pode pericia duplicada (mesma pericia)
        if (ordem.isTipoPericia() && ordemOutra.isTipoPericia()
                && ordem.getTipoPersonagem().equals(ordemOutra.getTipoPersonagem())) {
            //FIXME: mago pode fazer duas ordens de pericia se pelo menos uma for feitico
            return ordem.isMago() && ordemOutra.isMago() && ordem != ordemOutra && (ordem.isFeitico() || ordemOutra.isFeitico());
        }
        //personagem em exercito ou grupo?
        return isOrdemRequisitosMulti(ordem, ordem, ordemOutra);
    }

    /**
     * Verifica se o personagem pode realizar a ordem, de acordo com o tipo do actor.
     *
     * @param personagem
     * @param ordem
     * @return
     */
    private boolean isOrdemActor(BaseModel actor, Ordem ordem) {
        if (actor instanceof Personagem) {
            return isOrdemActorPersonagem((Personagem) actor, ordem);
        } else if (actor instanceof Cidade) {
            return isOrdemActorCidade((Cidade) actor, ordem);
        } else if (actor instanceof Nacao) {
            return isOrdemActorNacao((Nacao) actor, ordem);
        } else {
            return false;
        }
    }

    private boolean isOrdemActorPersonagem(Personagem personagem, Ordem ordem) {
        if (ordem.isGeral()) {
            return true;
        } else if (ordem.isComandante() && personagem.isComandante()) {
            return true;
        } else if (ordem.isAgente() && personagem.isAgente()) {
            return true;
        } else if (ordem.isEmissario() && personagem.isEmissario()) {
            return true;
        } else if (ordem.isMago() && personagem.isMago()) {
            return true;
        }
        return false;
    }

    private boolean isOrdemActorCidade(Cidade cidade, Ordem ordem) {
        return ordem.isCidadeOrdem();
    }

    private boolean isOrdemActorNacao(Nacao nacao, Ordem ordem) {
        return ordem.isNacaoOrdem();
    }

    /**
     * Verifica se o actor pode realizar a ordem, de acordo com os requisitos
     *
     * @param actor
     * @param ordem
     * @return
     */
    private boolean isOrdemRequisitos(BaseModel actor, Ordem ordem, boolean isStartupPackages) {
        final String requisitos = ordem.getRequisito().toLowerCase();
        //criticas por tipo de chave
        if (requisitos.contains("none")) {
            //none nao devia estar com mais nenhum parametro.
            return true;
        }
        if (actor instanceof Personagem) {
            return isOrdemRequisitosPersonagem((Personagem) actor, ordem);
        } else if (actor instanceof Cidade) {
            return isOrdemRequisitosCidade((Cidade) actor, ordem);
        } else if (actor instanceof Nacao) {
            return isOrdemRequisitosNacao((Nacao) actor, ordem, isStartupPackages);
        }
        return true;
    }

    private boolean isOrdemRequisitosCidade(Cidade cidade, Ordem ordem) {
        final String requisitos = ordem.getRequisito().toLowerCase();
        //criticas por tipo de chave
        if (requisitos.contains("capital") && !cidadeFacade.isCapital(cidade)) {
            return false;
        }
        if (requisitos.contains("bigcity") && !cidadeFacade.isBigCity(cidade)) {
            return false;
        }
        if (requisitos.contains("cs1") && cidadeFacade.getTamanho(cidade) != 1) {
            return false;
        }
        if (requisitos.contains("cs2") && cidadeFacade.getTamanho(cidade) != 2) {
            return false;
        }
        if (requisitos.contains("cs3") && cidadeFacade.getTamanho(cidade) != 3) {
            return false;
        }
        if (requisitos.contains("cs4") && cidadeFacade.getTamanho(cidade) != 4) {
            return false;
        }
        if (requisitos.contains("cs5") && cidadeFacade.getTamanho(cidade) != 5) {
            return false;
        }
        if (requisitos.contains("csn5") && cidadeFacade.getTamanho(cidade) == 5) {
            return false;
        }
        if (requisitos.contains("cf0") && cidadeFacade.getFortificacao(cidade) != 0) {
            return false;
        }
        if (requisitos.contains("cf1") && cidadeFacade.getFortificacao(cidade) != 1) {
            return false;
        }
        if (requisitos.contains("cf2") && cidadeFacade.getFortificacao(cidade) != 2) {
            return false;
        }
        if (requisitos.contains("cf3") && cidadeFacade.getFortificacao(cidade) != 3) {
            return false;
        }
        if (requisitos.contains("cf4") && cidadeFacade.getFortificacao(cidade) != 4) {
            return false;
        }
        if (requisitos.contains("cf5") && cidadeFacade.getFortificacao(cidade) != 5) {
            return false;
        }
        if (requisitos.contains("cfn5") && cidadeFacade.getFortificacao(cidade) == 5) {
            return false;
        }
        return true;
    }

    private boolean isOrdemRequisitosMulti(BaseModel actor, Ordem ordem, Ordem ordemOutra) {
        final int nn;
        if (ordem == ordemOutra) {
            //can re=use this slot
            nn = 1;
        } else {
            //
            nn = 0;
        }
        final String requisitos = ordem.getRequisito().toLowerCase();
        if (requisitos.contains("multi2") && getOrdensOpenSlots(actor) < 1 + nn) {
            return false;
        }
        if (requisitos.contains("multi3") && getOrdensOpenSlots(actor) < 2 + nn) {
            return false;
        }
        if (requisitos.contains("multi4") && getOrdensOpenSlots(actor) < 3 + nn) {
            return false;
        }
        if (requisitos.contains("multi5") && getOrdensOpenSlots(actor) < 4 + nn) {
            return false;
        }
        return true;
    }

    private boolean isOrdemRequisitosNacao(Nacao nacao, Ordem ordem, boolean isStartupPackages) {
        final String requisitos = ordem.getRequisito().toLowerCase();
        //criticas por tipo de chave
        return !(requisitos.contains("setup") && !isStartupPackages);
    }

    private boolean isOrdemRequisitosPersonagem(Personagem personagem, Ordem ordem) {
        final String requisitos = ordem.getRequisito().toLowerCase();
        //criticas por tipo de chave
        if (requisitos.contains("capital") && !personagemFacade.isInCapital(personagem)) {
            return false;
        }
        if (requisitos.contains("bigcity") && !cidadeFacade.isBigCity(personagemFacade.getCidade(personagem))) {
            return false;
        }
        if (requisitos.contains("terra") && !personagemFacade.isInTerra(personagem)) {
            return false;
        }
        if (requisitos.contains("2ex") && !personagemFacade.isInExercito(personagem)) {
            return false;
        }
        if (requisitos.contains("comex") && !personagemFacade.isComandaExercito(personagem)) {
            return false;
        }
        if (requisitos.contains("comesq") && !personagemFacade.isComandaEsquadra(personagem)) {
            return false;
        }
        if (requisitos.contains("comgrp") && !personagemFacade.isComandaGrupo(personagem)) {
            return false;
        }
        if (requisitos.contains("nogrp") && personagemFacade.isInGrupo(personagem)) {
            return false;
        }
        if (requisitos.contains("noex") && personagemFacade.isInExercito(personagem)) {
            return false;
        }
        if (requisitos.contains("ancoravel")) {
            //se é esquadra e ancoravel
            if (!personagemFacade.isInEsquadra(personagem)) {
                //return true;
            } else if (localFacade.isAncoravel(personagem.getLocal())) {
                //return true;
            } else {
                return false;
            }
        }
        if (requisitos.contains("cpp") && !personagemFacade.isInCidadePropria(personagem)) {
            /* 
             nao pode estar sitiado, mas nao fazendo a critica pq o flag de sitiado sobrevive ao turno. 
             nao pode ser usado no Judge!!!
             */
            return false;
        }
        if (requisitos.contains("ccn") && !personagemFacade.isInCidadeRaca(personagem)) {
            //city and nation have same culture
            return false;
        }
        if (requisitos.contains("clm") && !personagemFacade.isInCidadeLealdade(personagem)) {
            //city loyalty above minimum
            return false;
        }
        if (requisitos.contains("cpv") && !personagemFacade.isInCidadeVassalo(personagem)) {
            //nao pode estar sitiado, mas nao fazendo a critica pq o flag de sitiado sobrevive ao turno.
            return false;
        }
        if (requisitos.contains("cppr") && (!personagemFacade.isPodeMoverCidade(personagem) || !personagemFacade.isInCidadePropria(personagem))) {
            //nao pode estar sitiado, mas nao fazendo a critica pq o flag de sitiado sobrevive ao turno.
            return false;
        }
        if (requisitos.contains("cps") && !personagemFacade.isInCidadePropria(personagem)) {
            //pode estar sitiado
            return false;
        }
        if (requisitos.contains("cpx") && !personagemFacade.isInCidadeAlheio(personagem)) {
            return false;
        }
        if (requisitos.contains("cpany") && !personagemFacade.isInCidade(personagem)) {
            return false;
        }
        if (requisitos.contains("cpn") && personagemFacade.isInCidade(personagem)) {
            return false;
        }
        if (requisitos.contains("cpally") && !personagemFacade.isInCidadeAliada(personagem)) {
            //CIDADE considera PERSONAGEM amigo !this.criticaCpAmigavel(true)
            return false;
        }
        if (requisitos.contains("cpb") && !personagemFacade.isInCidadeAliada(personagem)) {
            //PERSONAGEM considera CIDADE amigo !this.criticaCpInimiga(false)
            return false;
        }
        if (requisitos.contains("cpi") && !personagemFacade.isInCidadeInimiga(personagem)) {
            //ALVO considera PERSONAGEM amigo !this.criticaCpInimiga(false)
            return false;
        }
        if (requisitos.contains("artefato") && !personagemFacade.isPersonagemHasItem(personagem, "Any")) {
            return false;
        }
        if (requisitos.contains("artscry") && !personagemFacade.isPersonagemHasItem(personagem, "Scry")) {
            return false;
        }
        if (requisitos.contains("artsumm") && !personagemFacade.isPersonagemHasItem(personagem, "Summon")) {
            return false;
        }
        if (requisitos.contains("artdragonegg") && !personagemFacade.isPersonagemHasItem(personagem, "DragonEgg")) {
            return false;
        }
        if (requisitos.contains("cs1") && personagemFacade.getCidadeTamanho(personagem) != 1) {
            return false;
        }
        if (requisitos.contains("cs2") && personagemFacade.getCidadeTamanho(personagem) != 2) {
            return false;
        }
        if (requisitos.contains("cs3") && personagemFacade.getCidadeTamanho(personagem) != 3) {
            return false;
        }
        if (requisitos.contains("cs4") && personagemFacade.getCidadeTamanho(personagem) != 4) {
            return false;
        }
        if (requisitos.contains("cs5") && personagemFacade.getCidadeTamanho(personagem) != 5) {
            return false;
        }
        if (requisitos.contains("csn5") && personagemFacade.getCidadeTamanho(personagem) == 5) {
            return false;
        }
        if (requisitos.contains("cf0") && personagemFacade.getCidadeFortificacao(personagem) != 0) {
            return false;
        }
        if (requisitos.contains("cf1") && personagemFacade.getCidadeFortificacao(personagem) != 1) {
            return false;
        }
        if (requisitos.contains("cf2") && personagemFacade.getCidadeFortificacao(personagem) != 2) {
            return false;
        }
        if (requisitos.contains("cf3") && personagemFacade.getCidadeFortificacao(personagem) != 3) {
            return false;
        }
        if (requisitos.contains("cf4") && personagemFacade.getCidadeFortificacao(personagem) != 4) {
            return false;
        }
        if (requisitos.contains("cf5") && personagemFacade.getCidadeFortificacao(personagem) != 5) {
            return false;
        }
        if (requisitos.contains("cfn5") && personagemFacade.getCidadeFortificacao(personagem) == 5) {
            return false;
        }
        if (requisitos.contains("lhf") && !personagemFacade.isTerrainLandmark(personagem)) {
            return false;
        }
        if (requisitos.contains("lhn") && personagemFacade.isTerrainLandmark(personagem)) {
            return false;
        }
        if (requisitos.contains("lhs") && !personagemFacade.isTerrainLandmarkSpire(personagem)) {
            return false;
        }
        return true;
    }

    public Ordem getOrdem(BaseModel actor, int index) {
        try {
            return actor.getAcao(index).getOrdem();
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public int getOrdemMax(Personagem actor, Cenario cenario) {
        return getOrdemMax(actor);
    }

    public int getOrdemMax(Cidade actor, Cenario cenario) {
        return getOrdemMax(actor);
    }

    public int getOrdemMax(Nacao actor, Cenario cenario) {
        return getOrdemMax(actor);
    }

    public int getOrdemMax(BaseModel actor, Partida game) {
        if (cenarioFacade.hasOrdens(game, actor)) {
            if (actor.isNacaoClass()) {
                return (int) Math.ceil((float) cenarioFacade.getStartupPackagesLimit(game) / ConverterFactory.POINTS_TO_ACTION_CONVERSION);
            } else {
                return getOrdemMax(actor);
            }
        } else {
            return 0;
        }
    }

    public int getOrdemMax(BaseModel actor) {
//        "add filters to see if there are orders for nation / city / army";
        return actor.getOrdensQt();
    }

    public String[] getOrdemDisplay(Personagem actor, int index, Cenario cenario, Jogador owner) {
        if (!isAtivo(actor)) {
            return (ACTIONDISABLEDARRAY);
        }
        return getOrdemDisplayGeneric(actor, index, getOrdemMax(actor, cenario), owner);
    }

    public String[] getOrdemDisplay(Cidade actor, int index, Cenario cenario, Jogador owner) {
        if (actor.getTamanho() <= 0) {
            return (ACTIONDISABLEDARRAY);
        }
        return getOrdemDisplayGeneric(actor, index, getOrdemMax(actor, cenario), owner);
    }

    public String[] getOrdemDisplay(Nacao actor, int index, Cenario cenario, Jogador owner) {
        if (!actor.isAtiva()) {
            return (ACTIONDISABLEDARRAY);
        }
        return getOrdemDisplayGeneric(actor, index, getOrdemMax(actor, cenario), owner);
    }

    private String[] getOrdemDisplayGeneric(BaseModel actor, int index, int acoesMax, Jogador owner) {
        try {
            if (SettingsManager.getInstance().isConfig("LoadActionsOtherNations", "deny", "deny") && !owner.isNacao(actor.getNacao())) {
                return (ACTIONDISABLEDARRAY);
            }
            Ordem ordem = actor.getAcao(index).getOrdem();
            List parametros = actor.getAcao(index).getParametrosDisplay();
            String[] ret = new String[3];
            ret[0] = ordem.getDescricao();
            ret[1] = parametros.toString();
            ret[2] = TitleFactory.getTipoOrdem(ordem);
            return ret;
        } catch (NullPointerException ex) {
            if (index >= acoesMax) {
                return (ACTIONDISABLEDARRAY);
            } else if (!owner.isNacao(actor.getNacao())) {
                return (ACTIONDISABLEDARRAY);
            } else {
                return (ACTIONBLANK);
            }
        }
    }

    public ActorAction getActorAction(PersonagemOrdem po) {
        return new ActorAction(po);

    }

    public ActorAction getActorActionBlank() {
        return new ActorAction(ActorAction.STATUS_BLANK);

    }

    public ActorAction getActorAction(IActor actor, int index, int acoesMax, Jogador owner) {
        if (SettingsManager.getInstance().isConfig("LoadActionsOtherNations", "deny", "deny") && !owner.isNacao(actor.getNacao())) {
            return new ActorAction(ActorAction.STATUS_DISABLED);
        }
        if (index >= acoesMax) {
            return new ActorAction(ActorAction.STATUS_DISABLED);
        }
        if (actor == null) {
            return new ActorAction(ActorAction.STATUS_DISABLED);
        }
        if (!actor.isActorActive()) {
            return new ActorAction(ActorAction.STATUS_DISABLED);
        }
        if (owner.isNacao(actor.getNacao())) {
            //owned, full visibility
            if (actor.getAcao(index) != null) {
                return new ActorAction(actor.getAcao(index));
            } else {
                //return black
                return new ActorAction(ActorAction.STATUS_BLANK);
            }
//        } else if (!owner.isNacao(actor.getNacao())) {
        } else if (nacaoFacade.isAliado(actor.getNacao(), owner.getNacoes().values())) {
            //ally, partial visibility
            //enemies, neutrals and others
            if (actor.getAcao(index) == null) {
                return new ActorAction(ActorAction.STATUS_BLANK_ALLY);
            } else {
                return new ActorAction(actor.getAcao(index), ActorAction.STATUS_READONLY);
            }
        } else {
            //enemies, neutrals and others
            if (actor.getAcao(index) == null) {
                return new ActorAction(ActorAction.STATUS_DISABLED);
            } else {
                return new ActorAction(actor.getAcao(index), ActorAction.STATUS_READONLY);
            }
        }
    }
//    public ActorAction getActorAction(IActor actor, int index, int acoesMax, Jogador owner) {
//        if (SettingsManager.getInstance().isConfig("LoadActionsOtherNations", "deny", "deny") && !owner.isNacao(actor.getNacao())) {
//            return new ActorAction(ActorAction.STATUS_DISABLED);
//        }
//        if (index >= acoesMax) {
//            return new ActorAction(ActorAction.STATUS_DISABLED);
//        }
//        if (actor == null) {
//            return new ActorAction(ActorAction.STATUS_DISABLED);
//        }
//        if (!actor.isActorActive()) {
//            return new ActorAction(ActorAction.STATUS_DISABLED);
//        }
//        if (actor.getAcao(index) == null && !owner.isNacao(actor.getNacao())) {
//            return new ActorAction(ActorAction.STATUS_DISABLED);
//        }
//        if (actor.getAcao(index) != null && !owner.isNacao(actor.getNacao())) {
//            return new ActorAction(actor.getAcao(index), ActorAction.STATUS_READONLY);
//        }
//        if (actor.getAcao(index) != null) {
//            return new ActorAction(actor.getAcao(index));
//        }
//        //return black
//        return new ActorAction(ActorAction.STATUS_BLANK);
//    }

    private boolean isAtivo(Personagem actor) {
        if (actor.isPersonagem()) {
            return personagemFacade.isAtivo((Personagem) actor);
        } else {
            return actor.isAtivo();
        }
    }

    public void setOrdem(BaseModel actor, int index, PersonagemOrdem personagemOrdem) {
        actor.setAcao(index, personagemOrdem);
    }

    public String getParametroDisplay(BaseModel actor, int indexOrdem, int indexParametro) {
        try {
            return actor.getAcao(indexOrdem).getParametrosDisplay().get(indexParametro);
        } catch (Exception ex) {
            return "";
        }
    }

    public PersonagemOrdem getPersonagemOrdem(BaseModel actor, int index) {
        try {
            return actor.getAcao(index);
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public SortedMap<Integer, PersonagemOrdem> getOrdensExecutadas(BaseModel actor) {
        try {
            return actor.getAcaoExecutadas();
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public Instant getTimeLastChange(BaseModel actor, int index) {
        try {
            return actor.getAcao(index).getUpdateTime();
        } catch (NullPointerException ex) {
            return Instant.now();
        }
    }

    public List<String> getParametrosId(BaseModel actor, int index) {
        try {
            return actor.getAcao(index).getParametrosId();
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public List<String> getParametrosDisplay(BaseModel actor, int index) {
        try {
            return actor.getAcao(index).getParametrosDisplay();
        } catch (NullPointerException ex) {
            return null;
        }
    }

    private boolean isOrdemAllowed(BaseModel actor, int indexOrdemAtiva, Ordem ordem) {
        //percorre todas as ordens do personagem;
        Ordem ordemOutra;
        boolean add = true;
        for (int ii = 0; ii < actor.getAcaoSize(); ii++) {
            //se for a ordem ativa, ignora;
            if (ii != indexOrdemAtiva) {
                try {
                    ordemOutra = actor.getAcao(ii).getOrdem();
                } catch (NullPointerException ex) {
                    ordemOutra = null;
                }
                //testa se ordem compativel;
                if (isOrdemCompativel(ordem, ordemOutra)) {
                    //if yes, add to the return array;
                    add = true;
                } else {
                    //if not, break;
                    add = false;
                    break;
                }
            }
        }
        return add;
    }

    public boolean isAtivo(Jogador jogadorAtivo, BaseModel actor) {
        if (actor instanceof Personagem) {
            Personagem personagem = (Personagem) actor;
            return (jogadorAtivo.isNacao(personagem.getNacao()) && personagemFacade.isAtivo(personagem));
        } else if (actor instanceof Cidade) {
            Cidade cidade = (Cidade) actor;
            return (jogadorAtivo.isNacao(cidade.getNacao()) && cidadeFacade.isAtivo(cidade));
        } else if (actor instanceof Nacao) {
            Nacao nacao = (Nacao) actor;
            return (jogadorAtivo.isNacao(nacao) && nacaoFacade.isAtiva(nacao));
        } else {
            return false;
        }
    }

    public String getResultado(BaseModel actor) {
        if (actor == null) {
            return "";
        }
        //ordens:
        if (actor.getResultados() != null && !actor.getResultados().equals("")) {
            return SysApoio.stringParse(actor.getResultados(), labels);
        }
        return "";
    }

    public int getRequirementsMultiLevel(Ordem ordem) {
        final String requisitos = ordem.getRequisito().toLowerCase();
        if (requisitos.contains("multi2")) {
            return 1;
        }
        if (requisitos.contains("multi3")) {
            return 2;
        }
        if (requisitos.contains("multi4")) {
            return 3;
        }
        if (requisitos.contains("multi5")) {
            return 4;
        }
        return 0;
    }

    public int getOrdensOpenSlots(BaseModel actor) {
        int ret = 0;
        for (int index = 0; index < actor.getOrdensQt(); index++) {
            if (this.getOrdem(actor, index) == null) {
                ret++;
            }
        }
        return ret;
    }

    public boolean isOpenOrdem(IActor actor) {
        //FIXME: specialize for nations by counting points, not slots.
        return actor.isActorActive() && actor.getOrdensQt() > actor.getAcaoSize();
    }

    public int getActionCount(BaseModel actor) {
        if (actor.isNacaoClass()) {
            return (int) (acaoFacade.getPointsSetup(actor.getNacao()) / ConverterFactory.POINTS_TO_ACTION_CONVERSION);
        } else {
            return actor.getAcaoSize();
        }
    }
}
