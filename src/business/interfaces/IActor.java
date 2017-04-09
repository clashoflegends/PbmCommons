/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.interfaces;

import java.util.SortedMap;
import model.Nacao;
import model.PersonagemOrdem;

/**
 *
 * @author jmoura
 */
public interface IActor {

    public void addAcaoExecutada(PersonagemOrdem po);

    public SortedMap<Integer, PersonagemOrdem> getAcaoExecutadas();

    public PersonagemOrdem getAcao(int index);

    public void setAcao(int index, PersonagemOrdem pOrdem);

    public int getAcaoSize();

    public SortedMap<Integer, PersonagemOrdem> getAcoes();

    public void remAcoes();

    public boolean isActorActive();

    public Nacao getNacao();

}
