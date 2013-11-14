/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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
//    private String timeStamp; //deprecated
    private Date creationTime;
    private List<ComandoDetail> comandos = new ArrayList();
    //nacao,personagem, ordem, parametros

    public Comando() {
        setCreationTime();
    }

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
        this.serial = 1234 + partidaId + jogadorId;
    }

    public boolean isSerial() {
        return this.serial == 1234 + this.getJogadorId() + this.getPartidaId();
    }

    private Date getCreationTime() {
        return creationTime;
    }

    public String getCreationTimeStamp() {
        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss Z");
        formatter.setTimeZone(TimeZone.getTimeZone("Europe/London"));//GMT+13
        return formatter.format(getCreationTime());
    }

    private void setCreationTime() {
        this.creationTime = new Date();
    }
}
