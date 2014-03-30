/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author gurgel
 */
public class ComandoDetail implements Serializable {

    //nacao,personagem, ordem, parametros
    private String nacaoCodigo, personagemCodigo, ordemCodigo, ordemNome, tpActor;
    private List<String> parametroId;
    private List<String> parametroDisplay;

    public ComandoDetail(Personagem personagem, Ordem ordem, List<String> parametrosId, List<String> parametrosDisplay) {
        this.nacaoCodigo = personagem.getNacao().getId() + "";
        this.personagemCodigo = personagem.getCodigo();
        this.tpActor = personagem.getTpActor();
        try {
            this.ordemCodigo = ordem.getCodigo() + "";
            this.ordemNome = ordem.getNome();
        } catch (NullPointerException ex) {
            //ordem vazia. just skipping
        }
        this.parametroId = parametrosId;
        this.parametroDisplay = parametrosDisplay;
    }

    public ComandoDetail(BaseModel actor, Ordem ordem, List<String> parametrosId, List<String> parametrosDisplay) {
        this.nacaoCodigo = actor.getNacao().getId() + "";
        this.personagemCodigo = actor.getCodigo();
        this.tpActor = actor.getTpActor();
        try {
            this.ordemCodigo = ordem.getCodigo() + "";
            this.ordemNome = ordem.getNome();
        } catch (NullPointerException ex) {
            //ordem vazia. just skipping
        }
        this.parametroId = parametrosId;
        this.parametroDisplay = parametrosDisplay;
    }

    public String getNacaoCodigo() {
        return nacaoCodigo;
    }

    public String getActorCodigo() {
        return personagemCodigo;
    }

    public String getOrdemCodigo() {
        return ordemCodigo;
    }

    public String getOrdemNome() {
        return ordemNome;
    }

    public List<String> getParametroId() {
        return parametroId;
    }

    public List<String> getParametroDisplay() {
        return parametroDisplay;
    }

    @Override
    public String toString() {
        return this.personagemCodigo + "-" + this.getOrdemNome() + "-" + getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    public boolean isActorPersonagem() {
        return tpActor == null || tpActor.equalsIgnoreCase("P");
    }

    public boolean isActorNacao() {
        return tpActor.equalsIgnoreCase("N");
    }

    public boolean isActorNpc() {
        return tpActor.equalsIgnoreCase("G");
    }

    public boolean isActorCidade() {
        return tpActor.equalsIgnoreCase("C");
    }

    public boolean isActorExercito() {
        return tpActor.equalsIgnoreCase("E");
    }
}
