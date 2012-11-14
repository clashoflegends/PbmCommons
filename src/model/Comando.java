/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author gurgel
 */
public class Comando implements Serializable {

    private String jogadorNome;
    private int jogadorId;
    private int serial;
    private String partidaCod;
    private int partidaId;
    private int turno;
    private List<ComandoDetail> comandos = new ArrayList();
    //nacao,personagem, ordem, parametros

    public void addComando(Personagem personagem, Ordem ordem, List<String> parametrosId, List<String> parametrosDisplay) {
        if (ordem != null) {
            ComandoDetail comDet = new ComandoDetail(personagem, ordem, parametrosId, parametrosDisplay);
            this.comandos.add(comDet);
        }
    }

    public int size() {
        return this.comandos.size();
    }

    public int getJogadorId() {
        return jogadorId;
    }

    public int getPartidaId() {
        return partidaId;
    }

    public List<ComandoDetail> getOrdens() {
        return this.comandos;
    }

    public String getJogadorNome() {
        return jogadorNome;
    }

    public String getPartidaCod() {
        return partidaCod;
    }

    public int getTurno() {
        return turno;
    }

    public void setInfos(Partida partida) {
        this.partidaCod = partida.getCodigo();
        this.partidaId = partida.getId();
        this.turno = partida.getTurno();
        this.jogadorId = partida.getJogadorAtivo().getId();
        this.jogadorNome = partida.getJogadorAtivo().getNome();
        this.serial = 1234 + this.getPartidaId() + this.getJogadorId();
    }

    public boolean isSerial() {
        return this.serial == 1234 + this.getJogadorId() + this.getPartidaId();
    }
}
