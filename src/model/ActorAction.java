/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;

/**
 *
 * @author gurgel
 */
public final class ActorAction extends BaseModel {

    public static final String ACTION_DISABLED = "-";
    public static final String ACTION_BLANK = " ";
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_BLANK = 1;
    public static final int STATUS_VALID = 2;
    public static final int STATUS_READONLY = 3;
    private int status = 0;
    private PersonagemOrdem personagemOrdem;

    private ActorAction() {
        setStatus(STATUS_DISABLED);
        setNome(ACTION_DISABLED);
        setCodigo(getNome());
    }

    public ActorAction(int status) {
        if (status == STATUS_BLANK) {
            setStatus(STATUS_BLANK);
            setNome(ACTION_BLANK);
        } else {
            setStatus(STATUS_DISABLED);
            setNome(ACTION_DISABLED);
        }
        setCodigo(getNome());
    }

    public ActorAction(PersonagemOrdem po) {
        this.setPersonagemOrdem(po);
        setStatus(STATUS_VALID);
        this.setNome(po.getOrdem().getDescricao() + po.getParametrosDisplay().toString());
        setCodigo(getNome());
    }

    public ActorAction(PersonagemOrdem po, int status) {
        this.setPersonagemOrdem(po);
        setStatus(STATUS_READONLY);
        this.setNome(po.getOrdem().getDescricao() + po.getParametrosDisplay().toString());
        setCodigo(getNome());
    }

    public PersonagemOrdem getPersonagemOrdem() {
        return personagemOrdem;
    }

    private void setPersonagemOrdem(PersonagemOrdem personagemOrdem) {
        this.personagemOrdem = personagemOrdem;
    }

    @Override
    public String toString() {
        return getNome();
    }

    private int getStatus() {
        return status;
    }

    private void setStatus(int type) {
        this.status = type;
    }

    public boolean isDisabled() {
        return getStatus() == STATUS_DISABLED;
    }

    public boolean isBlank() {
        return getStatus() == STATUS_BLANK;
    }

    public boolean isValid() {
        return getStatus() == STATUS_VALID;
    }

    public boolean isReadonly() {
        return getStatus() == STATUS_READONLY;
    }

    public String getOrdemDescricao() {
        try {
            return getPersonagemOrdem().getOrdem().getDescricao();
        } catch (NullPointerException e) {
            return " ";
        }
    }

    public Ordem getOrdem() {
        try {
            return getPersonagemOrdem().getOrdem();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public String getOrdemParameters() {
        try {
            return getPersonagemOrdem().getParametrosDisplay().toString().replace('[', ' ').replace(']', ' ').trim();
        } catch (NullPointerException e) {
            return " ";
        }
    }

}
