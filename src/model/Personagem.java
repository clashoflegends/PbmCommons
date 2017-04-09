/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import baseLib.BaseModel;
import business.interfaces.IActor;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author gurgel
 */
public class Personagem extends BaseModel implements IActor {
    // PENDING: alguns metodos deveriam sr transferidos ao Facade. Como soma de pericias. Talvez adicionar artefato.

    private int vida = 100, dueloBonus = 0, duelo = 0, ordensExtraQt = 0;
    private int periciaComandante = 0, periciaAgente = 0, periciaEmissario = 0, periciaMago = 0;
    private int periciaComandanteNatural = 0, periciaAgenteNatural = 0;
    private int periciaEmissarioNatural = 0, periciaMagoNatural = 0;
    private int periciaFurtividade = 0, periciaFurtividadeNatural = 0;
    private boolean refem = false, npc = false;
    private String portraiFilename = "blank.jpg";
    private Local local;
    private Local localOrigem;
    private Exercito exercito = null;
    private Nacao nacao, nacaoSubordinada;
    private Personagem lider;
    private Artefato artefatoCombateAtivo;
    private final SortedMap<String, Artefato> artefatos = new TreeMap();
    private final SortedMap<Integer, PersonagemFeitico> feiticos = new TreeMap();
    private final SortedMap<String, Personagem> liderados = new TreeMap();
    private final SortedMap<Integer, PersonagemOrdem> ordens = new TreeMap();  //kept for backwards compatibility
    private final SortedMap<Integer, PersonagemOrdem> ordensExecutadas = new TreeMap(); //kept for backwards compatibility

    public int getPericiaNaturalTotal() {
        return (periciaComandanteNatural + periciaAgenteNatural
                + periciaEmissarioNatural + periciaMagoNatural);
    }

    /**
     * Se o personagem possui o artefato
     *
     * @param artefato
     * @return
     */
    public boolean isArtefato(Artefato artefato) {
        return (this.artefatos.get(artefato.getNome()) != null);
    }

    public boolean isComandante() {
        return (this.getPericiaComandanteNatural() > 0);
    }

    public boolean isAgente() {
        return (this.getPericiaAgenteNatural() > 0);
    }

    public boolean isEmissario() {
        return (this.getPericiaEmissarioNatural() > 0);
    }

    /**
     * Se o personagem conhece o feitico
     *
     * @param feitico
     * @return
     */
    public boolean isFeitico(Feitico feitico) {
        return (this.feiticos.get(feitico.getNumero()) != null);
    }

    public boolean isMago() {
        return (this.getPericiaMagoNatural() > 0);
    }

    @Override
    public Nacao getNacao() {
        return nacao;
    }

    public void setNacao(Nacao nacao) {
        this.nacao = nacao;
        if (nacao != null) {
            nacao.addPersonagens(this);
        }
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
        try {
            local.addPersonagem(this);
        } catch (NullPointerException ex) {
            local.addPersonagem(this);
        }
    }

    public Exercito getExercito() {
        return exercito;
    }

    public void setExercito(Exercito exercito) {
        this.exercito = exercito;
    }

    public void addArtefato(Artefato artefato) {
        artefato.setOwner(this);
        this.artefatos.put(artefato.getNome(), artefato);
        doUsaArtefato(artefato, true);
    }

    public void remArtefato(Artefato artefato) {
        this.artefatos.remove(artefato.getNome());
        doUsaArtefato(artefato, false);
    }

    /*
     * Ajusta as pericias de acordo com o artefato<br>
     * retorna se o artefato pode ser utilizado pelo personagem.<br>
     * Vestindo define se o artefato esta sendo colocado ou retirado, ajustando as pericias de acordo
     */
    private boolean doUsaArtefato(Artefato artefato, Boolean vestindo) {
        int ajuste = 1;
        if (!vestindo) {
            ajuste = -1;
        }

        //PENDING: verificar se 'e da mesma alianca e permitir o uso.
        if (!this.isPodeUsar(artefato)) {
            return true;
        }
        if (artefato.isComandante() && this.isComandante()) {
            this.sumPericiaComandante(artefato.getValor() * ajuste);
        }
        if (artefato.isAgente() && this.isAgente()) {
            this.sumPericiaAgente(artefato.getValor() * ajuste);
        }
        if (artefato.isEmissario() && this.isEmissario()) {
            this.sumPericiaEmissario(artefato.getValor() * ajuste);
        }
        if (artefato.isMago() && this.isMago()) {
            this.sumPericiaMago(artefato.getValor() * ajuste);
        }
        if (artefato.isFurtividade()) {
            this.sumPericiaFurtividade(artefato.getValor() * ajuste);
        }
        if (artefato.isCombate() && !this.isArtefatoCombateAtivo()) {
            if (vestindo) {
                this.setArtefatoCombateAtivo(artefato);
            } else {
                // PENDING: transformar outro artefato em ativo, se disponivel
                this.setArtefatoCombateAtivo(null);
            }
        }
        doCalculaDuelo();
        return true;
    }

    private boolean isPodeUsar(Artefato artefato) {
        //fixme: move this stack to Facade
        //se artefato é neutro, liberado.
        boolean ret = false;
        if (artefato.isAlinhamentoNeutro()) {
            ret = true;
        } else if (artefato.getAlinhamento().equalsIgnoreCase(this.getNacao().getAlianca().getCodigo())) {
            ret = true;
        }
        return ret;
    }

    public int getPericiaComandante() {
        return periciaComandante;
    }

    public void setPericiaComandante(int periciaComandante) {
        this.periciaComandante = periciaComandante;
    }

    public int getPericiaAgente() {
        return periciaAgente;
    }

    public void setPericiaAgente(int periciaAgente) {
        this.periciaAgente = periciaAgente;
    }

    public int getPericiaEmissario() {
        return periciaEmissario;
    }

    public void setPericiaEmissario(int periciaEmissario) {
        this.periciaEmissario = periciaEmissario;
    }

    public int getPericiaMago() {
        return periciaMago;
    }

    public void setPericiaMago(int periciaMago) {
        this.periciaMago = periciaMago;
    }

    public int getPericiaComandanteNatural() {
        return periciaComandanteNatural;
    }

    public void setPericiaComandanteNatural(int periciaComandanteNatural) {
        this.periciaComandante += periciaComandanteNatural - this.periciaComandanteNatural;
        this.periciaComandanteNatural = periciaComandanteNatural;
    }

    public int getPericiaAgenteNatural() {
        return periciaAgenteNatural;
    }

    public void setPericiaAgenteNatural(int periciaAgenteNatural) {
        this.periciaAgente += periciaAgenteNatural - this.periciaAgenteNatural;
        this.periciaAgenteNatural = periciaAgenteNatural;
    }

    public int getPericiaEmissarioNatural() {
        return periciaEmissarioNatural;
    }

    public void setPericiaEmissarioNatural(int periciaEmissarioNatural) {
        this.periciaEmissario += periciaEmissarioNatural - this.periciaEmissarioNatural;
        this.periciaEmissarioNatural = periciaEmissarioNatural;
    }

    public int getPericiaMagoNatural() {
        return periciaMagoNatural;
    }

    public void setPericiaMagoNatural(int periciaMagoNatural) {
        this.periciaMago += periciaMagoNatural - this.periciaMagoNatural;
        this.periciaMagoNatural = periciaMagoNatural;
    }

    public Artefato getArtefatoCombateAtivo() {
        return this.artefatoCombateAtivo;
    }

    public int getDueloBonus() {
        return this.dueloBonus;
    }

    public void setDueloBonus(int bonus) {
        this.dueloBonus = bonus;
    }

    public boolean isArtefatoCombateAtivo() {
        return this.artefatoCombateAtivo != null;
    }

    public void setArtefatoCombateAtivo(Artefato artefato) {
        this.artefatoCombateAtivo = artefato;
        //ajusta duelo
        this.doCalculaDuelo();
    }

    /**
     * Soma a pericia alterada pelo artefato, mas nao altera pericia natural
     *
     * @param valor
     */
    private void sumPericiaAgente(int valor) {
        this.periciaAgente += valor;
    }

    /**
     * Soma a pericia alterada pelo artefato, mas nao altera pericia natural
     *
     * @param valor
     */
    private void sumPericiaComandante(int valor) {
        this.periciaComandante += valor;
    }

    /**
     * Soma a pericia alterada pelo artefato, mas nao altera pericia natural
     *
     * @param valor
     */
    private void sumPericiaEmissario(int valor) {
        this.periciaEmissario += valor;
    }

    /**
     * Soma a pericia alterada pelo artefato, mas nao altera pericia natural
     *
     * @param valor
     */
    private void sumPericiaMago(int valor) {
        this.periciaMago += valor;
    }

    /**
     * Soma a pericia alterada pelo artefato, mas nao altera pericia natural
     *
     * @param valor
     */
    private void sumPericiaFurtividade(int valor) {
        this.setPericiaFurtividade(this.getPericiaFurtividade() + valor);
    }

    /**
     * recalcula o duelo do personagem
     */
    public void doCalculaDuelo() {
        /**
         * calcular o duelo: definir maior duelo por pericia, com artefatos ai somar 25% dos duelos das demais pericias. ai somar bonus de duelo e
         * bonus de artefato de combate
         */
        float dueloNew;
        dueloNew = this.getMaiorDuelo();
        dueloNew += ((this.getPericiaComandante() + this.getPericiaAgente() * 0.75F + this.getPericiaEmissario() * 0.50F + this.getPericiaMago()) * 0.25F);
        dueloNew -= (this.getMaiorDuelo() * 0.25F);
        if (this.isArtefatoCombateAtivo()) {
            dueloNew += this.getArtefatoCombateAtivo().getValor() / 50;
        }
        dueloNew += this.getDueloBonus();
        this.setDuelo((int) dueloNew);
    }

    /**
     * retorna pericia com maior duelo, contando atributos, que o personagem tem
     */
    public int getMaiorDuelo() {
        double[] duelos = new double[4];
        duelos[0] = this.getPericiaComandante();
        duelos[1] = this.getPericiaAgente() * 0.75F;
        duelos[2] = this.getPericiaEmissario() * 0.50F;
        duelos[3] = this.getPericiaMago();
        Arrays.sort(duelos);
        return (int) duelos[3];
    }

    public int getVida() {
        return vida;
    }

    public void setVida(int vida) {
        this.vida = vida;
    }

    public int getPericiaFurtividade() {
        return periciaFurtividade;
    }

    public void setPericiaFurtividade(int furtividade) {
        this.periciaFurtividade = furtividade;
    }

    public int getDuelo() {
        return duelo;
    }

    public void setDuelo(int duelo) {
        this.duelo = duelo;
    }

    public SortedMap<String, Artefato> getArtefatos() {
        return this.artefatos;
    }

    public boolean addFeitico(Feitico feitico, int habilidade) {
        boolean ret = true;
        if (this.isMago()) {
            this.feiticos.put(feitico.getNumero(), new PersonagemFeitico(feitico, habilidade, this));
        } else {
            ret = false;
        }
        return ret;
    }

    public SortedMap<Integer, PersonagemFeitico> getFeiticos() {
        return this.feiticos;
    }

    public boolean isRefem() {
        return this.refem;
    }

    public void setRefem(boolean refem) {
        this.refem = refem;
    }

    public Local getLocalOrigem() {
        return localOrigem;
    }

    public void setLocalOrigem(Local localOrigem) {
        this.localOrigem = localOrigem;
    }

    public int getPericiaFurtividadeNatural() {
        return periciaFurtividadeNatural;
    }

    public void setPericiaFurtividadeNatural(int furtividadeNatural) {
        this.periciaFurtividade += furtividadeNatural - this.periciaFurtividadeNatural;
        this.periciaFurtividadeNatural = furtividadeNatural;
    }

    public boolean isNpc() {
        return npc;
    }

    public void setNpc(boolean npc) {
        this.npc = npc;
    }

    public Personagem getLider() {
        return lider;
    }

    public void setLider(Personagem viajandoCom) {
        this.lider = viajandoCom;
    }

    public SortedMap<String, Personagem> getLiderados() {
        return liderados;
    }

    public void addLiderados(Personagem liderado) {
        this.liderados.put(liderado.getCodigo(), liderado);
    }

    public boolean isComandaGrupo() {
        return (this.getLiderados().size() > 0 && this.getExercito() == null);
    }

    public boolean isComandaExercito() {
        return (this.getExercito() != null);
    }

    /**
     * @return the nacaoSubordinada
     */
    public Nacao getNacaoSubordinada() {
        return nacaoSubordinada;
    }

    /**
     * @param nacaoSubordinada the nacaoSubordinada to set
     */
    public void setNacaoSubordinada(Nacao nacaoSubordinada) {
        this.nacaoSubordinada = nacaoSubordinada;
    }

    public boolean isDoubleAgent() {
        return nacaoSubordinada != null;
    }

    /**
     * @return the extraOrdens
     */
    public int getOrdensExtraQt() {
        return ordensExtraQt;
    }

    /**
     * @param extraOrdens the extraOrdens to set
     */
    public void setOrdensExtraQt(int extraOrdens) {
        this.ordensExtraQt = extraOrdens;
    }

    /**
     * @return the portraiFilename
     */
    public String getPortraitFilename() {
        return portraiFilename;
    }

    /**
     * @param portraiFilename the portraiFilename to set
     */
    public void setPortraiFilename(String portraiFilename) {
        this.portraiFilename = portraiFilename;
    }

    public boolean isAtivo() {
        throw new UnsupportedOperationException("Not supported yet. Use PersonagemFacade");
    }

    public boolean isHero() {
        return getOrdensExtraQt() > 0;
    }

    @Override
    public boolean isPersonagem() {
        return true;
    }

    @Override
    public int getOrdensQt() {
        return 2 + getOrdensExtraQt();
    }

    @Override
    public String getTpActor() {
        return "P";
    }

    @Override
    public boolean isActorActive() {
        return this.getVida() > 0;
    }
}
