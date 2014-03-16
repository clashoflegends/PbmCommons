/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import baseLib.BaseModel;
import business.services.ComparatorFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import model.Cenario;
import model.Cidade;
import model.Jogador;
import model.Ordem;
import model.Personagem;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author gurgel
 */
public class OrdemFacade implements Serializable {

    private static final Log log = LogFactory.getLog(OrdemFacade.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final PersonagemFacade personagemFacade = new PersonagemFacade();
    private static final CidadeFacade cidadeFacade = new CidadeFacade();
    private static final LocalFacade localFacade = new LocalFacade();
    private static final String[] ACTIONDISABLED = new String[]{"-", ""};
    private static final String[] ACTIONBLANK = new String[]{" ", ""};

    public Ordem[] getOrdensDisponiveis(SortedMap<String, Ordem> ordens, Cidade cidade, int indexOrdemAtiva, boolean isAllOrders) {
        //cria vetor e garante tamanho minimo
        List<Ordem> ordensActor = new ArrayList<Ordem>(ordens.size() + 1);
        //lista as ordens que o personagem pode executar
        for (Ordem ordem : ordens.values()) {
            //se o personagem tem a pericia para dar a ordem
            if (isOrdemPersonagem(cidade, ordem)) {
                //ou sao todas as ordens, ou sao as ordens que ele pode no momento
                if (isAllOrders) {
                    ordensActor.add(ordem);
                } else if (isOrdemRequisitos(cidade, ordem)) {
                    if (isOrdemAllowed(cidade, indexOrdemAtiva, ordem)) {
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

    public Ordem[] getOrdensDisponiveis(SortedMap<String, Ordem> ordens, Personagem personagem, int indexOrdemAtiva, boolean isAllOrders) {
        //cria vetor e garante tamanho minimo
        List<Ordem> ordensActor = new ArrayList<Ordem>(ordens.size() + 1);
        //lista as ordens que o personagem pode executar
        for (Ordem ordem : ordens.values()) {
            //se o personagem tem a pericia para dar a ordem
            //TODO: artefatos que permitem ordens mesmo sem pericia (descobrir segredos)
            //TODO: nacoes com habilidades especiais para dar ordens.(descobrir segredos)
            if (isOrdemPersonagem(personagem, ordem)) {
                //ou sao todas as ordens, ou sao as ordens que ele pode no momento
                if (isAllOrders) {
                    ordensActor.add(ordem);
                } else if (isOrdemRequisitos(personagem, ordem)) {
                    if (isOrdemAllowed(personagem, indexOrdemAtiva, ordem)) {
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
            if (ordem.isMago() && ordemOutra.isMago() && ordem != ordemOutra && (ordem.isFeitico() || ordemOutra.isFeitico())) {
                return true;
            } else {
                return false;
            }
        }
        //personagem em exercito ou grupo?
        return true;
    }

    /**
     * Verifica se o personagem pode realizar a ordem, de acordo com o tipo do
     * personagem.
     *
     * @param personagem
     * @param ordem
     * @return
     */
    private boolean isOrdemPersonagem(Personagem personagem, Ordem ordem) {
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

    private boolean isOrdemPersonagem(Cidade cidade, Ordem ordem) {
        return ordem.isCidade();
    }

    private boolean isOrdemRequisitos(Cidade cidade, Ordem ordem) {
        final String requisitos = ordem.getRequisito().toLowerCase();
        //criticas por tipo de chave
        if (requisitos.contains("none")) {
            //none nao devia estar com mais nenhum parametro.
            return true;
        }
        if (requisitos.contains("capital") && !cidadeFacade.isCapital(cidade)) {
            return false;
        }
        return true;
    }

    /**
     * Verifica se o personagem pode realizar a ordem, de acordo com os
     * requisitos
     *
     * @param personagem
     * @param ordem
     * @return
     */
    private boolean isOrdemRequisitos(Personagem personagem, Ordem ordem) {
        final String requisitos = ordem.getRequisito().toLowerCase();
        //criticas por tipo de chave
        if (requisitos.contains("none")) {
            //none nao devia estar com mais nenhum parametro.
            return true;
        }
        if (requisitos.contains("capital") && !personagemFacade.isInCapital(personagem)) {
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
            //nao pode estar sitiado, mas nao fazendo a critica pq o flag de sitiado sobrevive ao turno.
            return false;
        }
        if (requisitos.contains("cpv") && !personagemFacade.isInCidadeVassalo(personagem)) {
            //nao pode estar sitiado, mas nao fazendo a critica pq o flag de sitiado sobrevive ao turno.
            return false;
        }
        if (requisitos.contains("cppr") && (!personagemFacade.isPodeMoverCidade(personagem) || !personagemFacade.isInCidadePropriaRaca(personagem))) {
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
        return cenario.getNumOrdens() + actor.getOrdensExtraQt();
    }

    public int getOrdemMax(Cidade actor, Cenario cenario) {
        return actor.getTamanho();
    }

    public String[] getOrdemDisplay(Personagem actor, int index, Cenario cenario, Jogador owner) {
        if (!isAtivo(actor)) {
            return (ACTIONDISABLED);
        }
        if (!owner.isNacao(actor.getNacao())) {
            return (ACTIONDISABLED);
        }
        return getOrdemDisplay(actor, index, getOrdemMax(actor, cenario));
    }

    public String[] getOrdemDisplay(Cidade actor, int index, Cenario cenario, Jogador owner) {
        if (actor.getTamanho() <= 0) {
            return (ACTIONDISABLED);
        }
        if (!owner.isNacao(actor.getNacao())) {
            return (ACTIONDISABLED);
        }
        return getOrdemDisplay(actor, index, getOrdemMax(actor, cenario));
    }

    private String[] getOrdemDisplay(BaseModel actor, int index, int acoesMax) {
        try {
            Ordem ordem = actor.getAcao(index).getOrdem();
            List parametros = actor.getAcao(index).getParametrosDisplay();
            String[] ret = new String[2];
            ret[0] = ordem.getDescricao();
            ret[1] = parametros.toString();
            return ret;
        } catch (NullPointerException ex) {
            if (index >= acoesMax) {
                return (ACTIONDISABLED);
            } else {
                return (ACTIONBLANK);
            }
        }
    }

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

    public PersonagemOrdem getPersonagemOrdem(Cidade cidade, int index) {
        try {
            return cidade.getAcao(index);
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public PersonagemOrdem getPersonagemOrdem(Personagem personagem, int index) {
        try {
            return personagem.getAcao(index);
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public SortedMap<Integer, PersonagemOrdem> getOrdensExecutadas(Personagem personagem) {
        try {
            return personagem.getAcaoExecutadas();
        } catch (NullPointerException ex) {
            return null;
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
        } else {
            return false;
        }
    }
}
