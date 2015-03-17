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
    private String nacaoCodigo, personagemCodigo, ordemCodigo, ordemNome, tpActor = "P";
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

    public String getOrdemDisplay() {
        return this.personagemCodigo + "-" + this.getOrdemNome() + "[" + parametroDisplay + "]";
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
        try {
            return tpActor == null || tpActor.equalsIgnoreCase("P");
        } catch (NullPointerException ex) {
            return true;
        }
    }

    public boolean isActorNacao() {
        try {
            return tpActor.equalsIgnoreCase("N");
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isActorNpc() {
        try {
            return tpActor.equalsIgnoreCase("G");
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isActorCidade() {
        try {
            return tpActor.equalsIgnoreCase("C");
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isActorExercito() {
        try {
            return tpActor.equalsIgnoreCase("E");
        } catch (NullPointerException ex) {
            return false;
        }
    }
}
