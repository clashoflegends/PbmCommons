package model;

import baseLib.BaseModel;

public class PersonagemFeitico extends BaseModel {

    private Feitico feitico;
    private int habilidade;
    private Personagem personagem;

    public PersonagemFeitico(Feitico feitico, int habilidade, Personagem pers) {
        this.personagem = pers;
        this.setFeitico(feitico);
        this.setHabilidade(habilidade);
    }

    public Feitico getFeitico() {
        return feitico;
    }

    public void setFeitico(Feitico feitico) {
        this.feitico = feitico;
    }

    public int getHabilidade() {
        return habilidade;
    }

    public void setHabilidade(int habilidade) {
        this.habilidade = habilidade;
    }

    @Override
    public String getComboDisplay() {
        return String.format("%s (%d) [%s]", feitico.getNome(), habilidade, feitico.getLivroFeitico());
    }

    @Override
    public String getComboId() {
        return String.format("%d", feitico.getNumero());
    }

    @Override
    public String getNome() {
        return String.format("%s (%d) [%s]", feitico.getNome(), habilidade, feitico.getLivroFeitico());
    }
}
