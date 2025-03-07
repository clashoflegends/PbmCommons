/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author gurgel
 */
public class PersonagemOrdem extends BaseModel {

    private static final Log log = LogFactory.getLog(PersonagemOrdem.class);

    private BaseModel personagem; //mantem o nome por compatibilidade com turnos velhos que jogadores podem ter salvo.
    private Ordem ordem;
    private int index;
    private String updateTime;
    private List<String> parametrosId = new ArrayList();
    private List<String> parametrosDisplay = new ArrayList();

    public PersonagemOrdem() {
        setUpdateTime();
    }

    public Ordem getOrdem() {
        return ordem;
    }

    public void setOrdem(Ordem ordem) {
        this.ordem = ordem;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public List<String> getParametrosId() {
        return parametrosId;
    }

    public void setParametrosId(List<String> parametrosId) {
        this.parametrosId = parametrosId;
    }

    public List<String> getParametrosDisplay() {
        return parametrosDisplay;
    }

    public void setParametrosDisplay(List<String> parametrosDisplay) {
        this.parametrosDisplay = parametrosDisplay;
    }

    @Override
    public String getNome() {
        if (super.getNome() == null && personagem == null) {
            //compatibilidade com turnos velhos.
            setNome(ActorAction.ACTION_DISABLED);
        } else if (super.getNome() == null) {
            //compatibilidade com turnos velhos.
            setNome(personagem.getNome());
        }
        return super.getNome();
    }

    public int compareToByNumber(Object o) {
        PersonagemOrdem outro = (PersonagemOrdem) o;
        try {
//            log.info(String.format("%s %s %s %s %s ", (this.getOrdem().getNumero() - outro.getOrdem().getNumero()), this.getOrdem().toString(), this.getOrdem().getNumero(), outro.getOrdem().toString(), outro.getOrdem().getNumero()));
            return (this.getOrdem().getNumero() - outro.getOrdem().getNumero());
        } catch (NullPointerException ex) {
            //zzz: 497/498
            if (outro.getOrdem() == null && this.getOrdem() == null) {
//                log.info("both nulls! ");
                return 0;
            } else if (outro.getOrdem() == null) {
//                log.info(String.format("0 %s %s ", this.getOrdem().toString(), this.getOrdem().getNumero()));
                return 0;
            } else if (this.getOrdem() == null) {
//                log.info(String.format("0 %s %s ", outro.getOrdem().toString(), outro.getOrdem().getNumero()));
                return (497 - outro.getOrdem().getNumero());
            } else {
//                log.info("both nulls? ");
                return (this.getOrdem().getNumero() - 497);
            }
        }
    }

    @Override
    public String toString() {
        return this.getNome() + "-" + getOrdem().getNome() + "-" + getParametrosId().toString() + "-" + getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    public Instant getUpdateTime() {
        return Instant.parse(updateTime);
    }

    public void setUpdateTime(Instant updateTime) {
        this.updateTime = updateTime.toString();
    }

    private void setUpdateTime() {
        this.updateTime = Instant.now().toString();
    }
}
