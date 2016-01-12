/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import baseLib.SysApoio;
import business.converter.ConverterFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Artefato;
import model.Cidade;
import model.Exercito;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Personagem;
import msgs.BaseMsgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author gurgel
 */
public class LocalFacade implements Serializable {

    private static final Log log = LogFactory.getLog(LocalFacade.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    public SortedMap<String, Artefato> getArtefatos(Local local) {
        return local.getArtefatos();
    }

    public int getCol(Local local) {
        return Integer.parseInt(local.getCodigo().substring(0, 2));
    }

    public String getCoordenadas(Local local) {
        if (local == null) {
            return "-";
        } else {
            return local.getCoordenadas();
        }

    }

    public SortedMap<String, Exercito> getExercitos(Local local) {
        return local.getExercitos();
    }

    /**
     * List alive chars at the hex
     *
     * @param local
     * @return
     */
    public SortedMap<String, Personagem> getPersonagens(Local local) {
        PersonagemFacade personagemFacade = new PersonagemFacade();
        final SortedMap<String, Personagem> ret = new TreeMap<String, Personagem>();
        for (Personagem personagem : local.getPersonagens().values()) {
            if (personagemFacade.isLocalConhecido(personagem)) {
                ret.put(personagem.getCodigo(), personagem);
            }
        }
        return ret;
    }

    public int getRow(Local local) {
        return Integer.parseInt(local.getCodigo().substring(2));
    }

    public String getTerrenoNome(Local local) {
        try {
            return local.getTerreno().getNome();
        } catch (NullPointerException ex) {
            return "-";
        }
    }

    public String getClima(Local local) {
        try {
            return BaseMsgs.localClima[local.getClima()];
        } catch (NullPointerException ex) {
            return "-";
        }
    }

    public Cidade getCidade(Local local) {
        return local.getCidade();
    }

    public String getTerrenoCodigo(Local local) {
        return local.getTerreno().getCodigo();
    }

    /**
     * Deve ser utilizado quando importa o observador, no caso, se cidade é
     * oculta.
     *
     * @param local
     * @param observer
     * @return
     */
    public boolean isCidade(Local local, Nacao observer) {
        boolean ret = false;
        try {
            Cidade cidade = local.getCidade();
            ret = (cidade != null && (!cidade.isOculto() || cidade.getNacao() == observer));
        } catch (NullPointerException ex) {
            ret = false;
        }
        return ret;
    }

    /**
     * Este deve ser usado para verificar a existencia da cidade, sem importar
     * se é oculta.
     *
     * @param local
     * @return
     */
    public boolean isCidade(Local local) {
        return (local.getCidade() != null);
    }

    public boolean isEstrada(Local local, int direcao) {
        return local.isEstrada(direcao);
    }

    public boolean isPonte(Local local, int direcao) {
        return local.isPonte(direcao);
    }

    public boolean isLanding(Local local, int direcao) {
        return local.isLanding(direcao);
    }

    public boolean isRiacho(Local local, int direcao) {
        return local.isRiacho(direcao);
    }

    public boolean isRio(Local local, int direcao) {
        return local.isRio(direcao);
    }

    public boolean isVau(Local local, int direcao) {
        return local.isVau(direcao);
    }

    public boolean isAncoravel(Local local) {
        return local.getTerreno().isAncoravel() || this.isDocas(local);
    }

    public boolean isDocas(Local local) {
        try {
            return local.getCidade().getDocas() > 0;
        } catch (NullPointerException ex) {
            return false;
        }
    }

    /**
     * Testa o lado em que esta cruzando mais o dois adjacentes por um rio.
     *
     * @param destino
     * @param direcao
     * @return
     */
    public boolean isRioLado(Local local, int direcao) {
        boolean ret = false;
        for (int ii = -1; !ret && ii <= 1; ii++) {
            int temp = ConverterFactory.getDirecao(direcao + ii);
            if (!ret && local.isRio(temp)) {
                ret = true;
            }
        }
        return ret;
    }

    public boolean isBarcos(Local local) {
        ExercitoFacade exercitoFacade = new ExercitoFacade();
        for (Exercito exercito : local.getExercitos().values()) {
            if (exercitoFacade.isEsquadra(exercito)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRastroExercito(Local local, int direcao) {
        return local.isRastro(direcao);
    }

    public Local getLocalVizinho(Local local, String direcao, SortedMap<String, Local> lista) {
        return lista.get(getIdentificacaoVizinho(local, direcao));
    }

    public Local getLocalVizinho(Local local, int direcao, SortedMap<String, Local> lista) {
        return lista.get(getIdentificacaoVizinho(local, direcao));
    }

    public String getIdentificacaoVizinho(Local atual, String nmDirecao) {
        return getIdentificacaoVizinho(atual, ConverterFactory.direcaoToInt(nmDirecao));
    }

    public String getIdentificacaoVizinho(Local atual, int direcao) {
        String newIdentificacao;
        //int newHex[] = this.identificacaoToInt();
        Integer lCol = this.getCol(atual);
        Integer lRow = this.getRow(atual);
        //calcula row & col
        switch (direcao) {
            case 0: //C 0 0
                break;
            case 1: //NO 0 -1
                if ((lRow % 2) == 1) {
                    lCol--;
                }
                lRow--;
                break;
            case 2: //NE +1 -1
                if ((lRow % 2) == 0) {
                    lCol++;
                }
                lRow--;
                break;
            case 3: //L +1 0
                lCol++;
                break;
            case 4: //SE +1 +1
                if ((lRow % 2) == 0) {
                    lCol++;
                }
                lRow++;
                break;
            case 5: //SO 0 +1
                if ((lRow % 2) == 1) {
                    lCol--;
                }
                lRow++;
                break;
            case 6: //O -1 0
                lCol--;
                break;
            default:
                //direcao inválida
                lCol = -1;
                lRow = -1;
                break;
        }
        //converte row/col em coordenada
        if (direcao == 0) {
            newIdentificacao = atual.getCodigo();
            //verifica se saiu do mapa
//        } else if (lCol < 1 || lRow < 1) {
//            //fora do mapa.
//            newIdentificacao = null;
        } else {
            //converte de volta para string e retorna o Hex
            newIdentificacao = SysApoio.pointToCoord(lCol, lRow);
        }
        return newIdentificacao;
    }

    public int getDistancia(Local origem, Local destino) {
        if (origem == destino) {
            return 0;
        } else {
            int rowOrigem = this.getRow(origem);
            int colOrigem = this.getCol(origem);
            int rowDestino = this.getRow(destino);
            int colDestino = this.getCol(destino);
            return SysApoio.getDistancia(colOrigem, colDestino, rowOrigem, rowDestino);
        }
    }

    public HashMap<Local, Integer> getLocalRange(Local local, int range, boolean borderOnly, SortedMap<String, Local> listLocais) {
        HashMap<Local, Integer> list = new HashMap<Local, Integer>();
        for (Local target : listLocais.values()) {
            final int distancia = getDistancia(local, target);
            if (borderOnly && distancia == range) {
                //no filling, border only
                list.put(target, distancia);
            } else if (distancia <= range) {
                //with filling
                list.put(target, distancia);
            }
        }
        return list;
    }

    public boolean isAgua(Local local) {
        return local.getTerreno().isAgua();
    }

    public boolean isVisible(Local local, Jogador observer) {
        final String flags = local.getVisibilidadeNacao();
        if (flags == null || flags.trim().equals("")) {
            //no one has visibility
            return false;
        }
        String[] elem = flags.split(";");
        for (String elem1 : elem) {
            String[] nat = elem1.split("=");
            if (SysApoio.parseInt(nat[1]) > 0 && observer.isNacao(SysApoio.parseInt(nat[0]))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param personagem
     * @param tipo
     * @param tipo 0 = todos - self
     * @param tipo 1 = mesma nacao - self
     * @param tipo 2 = outras nacoes - self
     * @param tipo 3 = em exercito e com pericia de comandante, qualquer nacao -
     * self + Null (optional)
     * @param tipo 4 = todos + self
     * @return
     */
    public Personagem[] listPersonagemLocal(Local local, Personagem personagem, int tipo) {
        List<Personagem> ret = new ArrayList();
        if (tipo == 0) {
            ret.addAll(local.getPersonagens().values());
            ret.remove(personagem);
        } else if (tipo == 1) {
            for (Personagem pers : local.getPersonagens().values()) {
                if (personagem != pers && personagem.getNacao() == pers.getNacao()) {
                    ret.add(pers);
                }
            }
            ret.remove(personagem);
        } else if (tipo == 2) {
            ret.addAll(local.getPersonagens().values());
            ret.removeAll(personagem.getNacao().getPersonagens());
            ret.remove(personagem);
        } else if (tipo == 3) {
            for (Personagem pers : local.getPersonagens().values()) {
                if (pers.isComandante()) {
                    ret.add(pers);
                }
            }
            if (personagem != null && personagem.isComandaExercito()) {
                ret.remove(personagem);
            }
        } else if (tipo == 4) {
            ret.addAll(local.getPersonagens().values());
        }
        return (Personagem[]) ret.toArray(new Personagem[0]);
    }

    public Personagem[] listPersonagemLocal(Local local, Nacao nacao) {
        List<Personagem> ret = new ArrayList();
        for (Personagem pers : local.getPersonagens().values()) {
            if (nacao == pers.getNacao() && !pers.isHero()) {
                //check if hero
                ret.add(pers);
            }
        }
        return (Personagem[]) ret.toArray(new Personagem[0]);
    }

    public boolean isCombatTookPlace(Local local) {
        return local.hasHabilidade(";LHC;");
    }
}
