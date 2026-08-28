/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import business.converter.ColorFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Artefato;
import model.Cenario;
import model.Cidade;
import model.Exercito;
import model.Feitico;
import model.Habilidade;
import model.Local;
import model.Nacao;
import model.Ordem;
import model.Personagem;
import model.PersonagemFeitico;
import model.PersonagemOrdem;
import model.Raca;
import msgs.BaseMsgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 *
 * @author gurgel
 */
public class PersonagemFacade implements Serializable {

    /** The historical single fallback portrait: a hooded rogue. Still the last resort. */
    public static final String PORTRAIT_BLANK = "blank.jpg";

    /**
     * Shown when no class can be derived: a deliberately anonymous hooded figure. Two cases reach it,
     * and anonymity is right for both. A DEAD character has had every skill zeroed by the Judge, so
     * the class is destroyed rather than hidden; an ENEMY seen at low visibility has had its skills
     * stripped on export. Falling back to blank.jpg instead would show David Ble's hooded ASSASSIN,
     * which both breaks the new art style and quietly asserts a class the viewer is not entitled to.
     */
    public static final String PORTRAIT_UNKNOWN = "default_unknown.jpg";

    /**
     * Variante-level habilidade -> portrait set name. First match wins, so a variante carrying two
     * flags gets the earlier one. Add a row plus 8 images named default_&lt;set&gt;_&lt;classe&gt;_&lt;m|f&gt;.jpg
     * to introduce a new style; no other code changes.
     */
    private static final String[][] PORTRAIT_SETS = {
        {";SJG;", "greek"},
        {";SJD;", "dance"}};
    //;SJT; (Westeros) deliberately has NO row. The generic default set IS the Westeros art, so a
    //GoT game reaches it by falling through; giving ;SJT; its own copy would mean two files to keep
    //in sync and a stale one the moment either moves. The flag stays in the DB, inert, ready for the
    //day GoT needs art that differs from the default - then it gets a row and its own files.

    private static final Log log = LogFactory.getLog(PersonagemFacade.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final LocalFacade localFacade = new LocalFacade();
    private final NacaoFacade nacaoFacade = new NacaoFacade();
    private final CidadeFacade cidadeFacade = new CidadeFacade();
    private static final AcaoFacade acaoFacade = new AcaoFacade();

    /**
     * Default portrait filename for a character who has none of their own: one per class per gender.
     * <p>
     * A character is not a single class - {@code isComandante()}, {@code isAgente()},
     * {@code isEmissario()} and {@code isMago()} are independent and derived from the NATURAL skill
     * being above zero - so a commander who also studies magic matches two. The class shown is the
     * one with the HIGHEST natural skill, and on a tie the order is
     * <b>Comandante &gt; Mago &gt; Agente &gt; Emissario</b> (John, 2026-08-24).
     * <p>
     * Returns {@link #PORTRAIT_BLANK} when no natural skill is above zero, so a character with no
     * class at all keeps the historical behaviour. The caller must still cope with the file being
     * absent - {@code ImageManager} falls back to blank.jpg - because the art ships in portraits.zip
     * and a player may not have downloaded the current pack yet.
     *
     * @param personagem the character
     * @return a filename such as {@code default_comandante_f.jpg}, never null
     */
    public String getDefaultPortraitFilename(Personagem personagem) {
        final String classe = getPortraitClasse(personagem);
        if (classe == null) {
            //no class we are allowed to see: the anonymous figure, not a class-bearing placeholder
            return PORTRAIT_UNKNOWN;
        }
        return String.format("default_%s_%s.jpg", classe, getPortraitGenderSuffix(personagem));
    }

    /**
     * Default portrait for a character in a scenario that ships its own art style, e.g.
     * {@code default_greek_comandante_m.jpg}. Same class and gender rule as the generic set.
     * <p>
     * Returns <b>null</b> rather than blank.jpg when there is no set or no class, so the caller
     * falls through to the generic default instead of dropping straight to the floor. That matters:
     * the scenario art ships in portraits.zip like everything else, and a player who has not
     * downloaded the current pack must degrade to the navy class default, not to blank.jpg.
     *
     * @param personagem the character
     * @param portraitSet the set name from {@link #getPortraitSet(Cenario)}, may be null
     * @return a filename, or null when this character has no scenario-specific default
     */
    public String getDefaultPortraitFilename(Personagem personagem, String portraitSet) {
        if (portraitSet == null) {
            return null;
        }
        final String classe = getPortraitClasse(personagem);
        if (classe == null) {
            return null;
        }
        return String.format("default_%s_%s_%s.jpg", portraitSet, classe, getPortraitGenderSuffix(personagem));
    }

    /**
     * Which framing variant this character's DEFAULT portrait uses, so two characters of the same
     * class, gender and nation are not drawn pixel-identically. Stable for the life of the character
     * and identical on every client, because it is derived from {@code identificacao} - the 5-char
     * code the Judge assigns at creation and never changes afterwards.
     * <p>
     * Keyed on a hash rather than a letter of the code: second letters are heavily vowel-weighted
     * (a, o and e alone are 55% of all characters), so slicing the alphabet directly splits the
     * population about 24/32/43, while the hash lands within a point or two of even.
     *
     * @param personagem the character
     * @return an index into the variant table, 0 when there is nothing to hash
     */
    public int getDefaultPortraitVariant(Personagem personagem) {
        final String seed = (personagem == null) ? null : personagem.getCodigo();
        if (seed == null || seed.isEmpty()) {
            return 0;
        }
        //String.hashCode is specified by the JLS, so this is identical on every JVM and every client.
        return Math.floorMod(seed.hashCode(), ColorFactory.PORTRAIT_VARIANT_COUNT);
    }

    /**
     * The scenario's portrait set, declared by a variante-level habilidade under the {@code ;SJ_;}
     * mask, or null when the scenario ships no art of its own.
     * <p>
     * The flag lives on the VARIANTE ({@code variante.habilidades}, surfacing as
     * {@code cenario.habilidades}) rather than the game, so every game of a variante looks the same
     * and no per-game data entry is needed. Reading it off the Cenario is only safe because the
     * Cenario cache is keyed per game - variantes 11 and 16 share id_cenario 11, and before that fix
     * a GoT game could have been handed F&D's flags. Requires Distiler 2.88 or newer to write the EGF.
     *
     * @param cenario the game's scenario, may be null
     * @return "greek", "dance", "got", or null
     */
    public String getPortraitSet(Cenario cenario) {
        if (cenario == null) {
            return null;
        }
        for (String[] set : PORTRAIT_SETS) {
            if (cenario.hasHabilidade(set[0])) {
                return set[1];
            }
        }
        return null;
    }

    /**
     * The class whose art a character should wear: the one with the HIGHEST natural skill, tie-break
     * Comandante &gt; Mago &gt; Agente &gt; Emissario. Null when no natural skill is above zero.
     */
    private String getPortraitClasse(Personagem personagem) {
        if (personagem == null) {
            return null;
        }
        //ordered by the tie-break priority, so the first strict maximum wins
        final String[] classe = {"comandante", "mago", "agente", "emissario"};
        final int[] pericia = {
            personagem.getPericiaComandanteNatural(),
            personagem.getPericiaMagoNatural(),
            personagem.getPericiaAgenteNatural(),
            personagem.getPericiaEmissarioNatural()};
        int best = 0;
        for (int ii = 1; ii < pericia.length; ii++) {
            if (pericia[ii] > pericia[best]) {
                best = ii;
            }
        }
        return (pericia[best] > 0) ? classe[best] : null;
    }

    /**
     * "f" for a woman, "m" otherwise. Personagem.sexo is 0=male, 1=female, 2=undefined; undefined
     * takes the male art rather than dropping to blank.jpg, because showing the right CLASS matters
     * more than the gender and a handful of old rows are still undefined.
     */
    private String getPortraitGenderSuffix(Personagem personagem) {
        return (personagem.getSexo() == 1) ? "f" : "m";
    }

    public Collection<Artefato> getArtefatos(Personagem personagem) {
        return personagem.getArtefatos().values();
    }

    public Nacao getNacao(Personagem personagem) {
        return personagem.getNacao();
    }

    public Raca getNacaoRaca(Personagem personagem) {
        return personagem.getNacao().getRaca();
    }

    public Local getLocal(Personagem personagem) {
        Local local = null;
        if (isLocalConhecido(personagem)) {
            local = personagem.getLocal();
        }
        return local;
    }

    public Cidade getCidade(Personagem personagem) {
        Cidade cidade = null;
        if (isLocalConhecido(personagem)) {
            if (localFacade.isCidade(personagem.getLocal(), this.getNacao(personagem))) {
                cidade = localFacade.getCidade(personagem.getLocal());
            }
        }
        return cidade;
    }

    public int getCidadeTamanho(Personagem personagem) {
        final Cidade cidade = getCidade(personagem);
        if (cidade == null) {
            return 0;
        } else {
            return cidadeFacade.getTamanho(cidade);
        }
    }

    public int getCidadeFortificacao(Personagem personagem) {
        final Cidade cidade = getCidade(personagem);
        if (cidade == null) {
            return 0;
        } else {
            return cidadeFacade.getFortificacao(cidade);
        }
    }

    public Local getLocalOrigem(Personagem personagem) {
        // FIXME: tem que saber se o personagem comecou o turno como refem, neste caso nao divulga local
        Local local = null;
        if (!personagem.isRefem()) {
            local = personagem.getLocalOrigem();
        }
        return local;
    }

    /**
     * PC can be moving by himself, in an army, or with a leader
     *
     * @param pc
     * @return
     */
    public Local getLocalDestination(Personagem pc, SortedMap<String, Local> locais) {
        for (PersonagemOrdem po : pc.getAcoes().values()) {
            if (acaoFacade.isMovimentoDirection(po)) {
                //FIXME: calculate final hex based on directions
                final SortedMap<Integer, Local> pathMov = acaoFacade.getLocalDestinationPath(pc, po, locais);
                //return last position
                return pathMov.get(pathMov.size() - 1);
            }
            if (acaoFacade.isMovimento(po)) {
                //just get the final hex
                final Local localDestination = acaoFacade.getLocalDestination(pc, po, locais);
                if (localDestination != null) {
                    return localDestination;
                }
            }
            if (pc.getLider() != null) {
                //if traveling with a leader then... BEWARE recursive code!!!
                return getLocalDestination(pc.getLider(), locais);
            }
        }
        return pc.getLocal();
    }

    public String getNacaoNome(Personagem personagem) {
        try {
            return personagem.getNacao().getNome();
        } catch (NullPointerException ex) {
            return labels.getString("DESCONHECIDO");
        }
    }

    public String getCoordenadas(Personagem personagem) {
        return LocalFacade.getCoordenadas(personagem.getLocal());
    }

    public String getNome(Personagem personagem) {
        return personagem.getNome();
    }

    public boolean isComandante(Personagem personagem) {
        return personagem.isComandante();
    }

    public boolean isMago(Personagem personagem) {
        return personagem.isMago();
    }

    public boolean isNpc(Personagem personagem) {
        return personagem.isNpc() || nacaoFacade.isNpc(personagem);
    }

    public boolean isHero(Personagem personagem) {
        return personagem.isHero();
    }

    public boolean isPersonagemHasFeitico(Personagem personagem, Feitico feitico) {
        return personagem.isFeitico(feitico);
    }

    public PersonagemFeitico[] listFeiticoByOrdem(Ordem ordem, Personagem personagem) {
        SortedMap<Integer, PersonagemFeitico> smRet = new TreeMap();
        //PersonagemFeitico[] ret = null;
        if (personagem.isMago()) {
            for (PersonagemFeitico magia : personagem.getFeiticos().values()) {
                try {
                    if (magia.getFeitico().getOrdem() == ordem) {
                        smRet.put(magia.getFeitico().getNumero(), magia);
                    }
                } catch (NullPointerException ex) {
                    //nao adiciona a magia
                }
            }
        }
        return (PersonagemFeitico[]) smRet.values().toArray(new PersonagemFeitico[0]);
    }

    public Feitico[] listFeiticoByOrdem(Ordem ordem, List<Feitico> listaFeiticos) {
        SortedMap<Integer, Feitico> smRet = new TreeMap();
        //PersonagemFeitico[] ret = null;
        for (Feitico feitico : listaFeiticos) {
            try {
                if (feitico.getOrdem() == ordem) {
                    smRet.put(feitico.getNumero(), feitico);
                }
            } catch (NullPointerException ex) {
                //nao adiciona a magia
            }
        }
        return (Feitico[]) smRet.values().toArray(new Feitico[0]);
    }

    /**
     * lista os feiticos que o personagem conhece de um livro
     */
    public Feitico[] listFeiticos(Personagem personagem, String livro) {
        SortedMap<Integer, Feitico> smRet = new TreeMap();
        for (PersonagemFeitico personagemFeitico : personagem.getFeiticos().values()) {
            try {
                if (livro.equals(personagemFeitico.getFeitico().getLivroFeitico())) {
                    smRet.put(personagemFeitico.getFeitico().getNumero(), personagemFeitico.getFeitico());
                }
            } catch (NullPointerException ex) {
                //nao adiciona a magia
            }
        }
        return (Feitico[]) smRet.values().toArray(new Feitico[0]);
    }

    public SortedMap<Integer, PersonagemFeitico> getFeiticos(Personagem personagem) {
        if (!isMorto(personagem)) {
            return personagem.getFeiticos();
        } else {
            return null;
        }
    }

    public boolean hasFeiticoRequisito(Personagem personagem, Feitico feiticoAlvo, Feitico[] listFeiticos) {
        boolean ret = false;
        FeiticoFacade feiticoFacade = new FeiticoFacade();
        if (feiticoFacade.isProibido(feiticoAlvo)) {
            //TODO: verifica artefatos
            //verifica NSP
            if (nacaoFacade.hasHabilidade(this.getNacao(personagem), "0023") && feiticoAlvo.getNumero() == 502) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), ";PZW;") && feiticoAlvo.getNumero() == 502) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), "0024") && feiticoAlvo.getNumero() == 508) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), ";PZCM;") && feiticoAlvo.getNumero() == 508) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), "0025") && feiticoAlvo.getNumero() == 510) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), ";PZCF;") && feiticoAlvo.getNumero() == 510) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), "0026") && feiticoAlvo.getNumero() == 512) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), ";PZH;") && feiticoAlvo.getNumero() == 512) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), "0027") && feiticoAlvo.getNumero() == 244) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), ";PZFH;") && feiticoAlvo.getNumero() == 244) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), "0028") && feiticoAlvo.getNumero() == 248) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), ";PZF;") && feiticoAlvo.getNumero() == 248) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), "0029") && feiticoAlvo.getNumero() == 246) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), ";PZS;") && feiticoAlvo.getNumero() == 246) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), "0030") && feiticoAlvo.getNumero() == 314) {
                ret = true;
            } else if (nacaoFacade.hasHabilidade(this.getNacao(personagem), ";PZT;") && feiticoAlvo.getNumero() == 314) {
                ret = true;
            }

        } else {
            ret = feiticoFacade.hasRequisito(feiticoAlvo, listFeiticos);
        }
        return ret;
    }

    public boolean isInCapital(Personagem personagem) {
        try {
            return (personagem.getLocal() == nacaoFacade.getLocal(personagem.getNacao()));
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isInTerra(Personagem personagem) {
        return !personagem.getLocal().getTerreno().isAgua();
    }

    public boolean isInEsquadra(Personagem personagem) {
        try {
            return (this.isComandaEsquadra(personagem) || this.isComandaEsquadra(personagem.getLider()));
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isInExercito(Personagem personagem) {
        try {
            return (this.isComandaExercito(personagem) || this.isComandaExercito(personagem.getLider()));
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isComandaExercito(Personagem personagem) {
        return (personagem.getExercito() != null);
    }

    public boolean isComandaEsquadra(Personagem personagem) {
        try {
            ExercitoFacade exercitoFacade = new ExercitoFacade();
            return (this.isComandaExercito(personagem) && exercitoFacade.isEsquadra(personagem.getExercito()));
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isComandaGrupo(Personagem personagem) {
        return personagem.isComandaGrupo();
    }

    public boolean isInGrupo(Personagem personagem) {
        return personagem.isComandaGrupo() || personagem.getLider() != null;
    }

    public boolean isInCidadePropria(Personagem personagem) {
        try {
            return (personagem.getLocal().getCidade().getNacao() == personagem.getNacao());
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isInCidadePropriaNaoSitiado(Personagem personagem) {
        try {
            return (!personagem.getLocal().getCidade().isSitiado()
                    && this.isInCidadePropria(personagem));
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isInCidadeLealdade(Personagem personagem) {
        try {
            return (personagem.getLocal().getCidade().getLealdade() >= CenarioFacade.MINIMUM_LOYALTY);
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isInCidadeRaca(Personagem personagem) {
        try {
            return (this.isInCidade(personagem)
                    && personagem.getLocal().getCidade().getNacao().getRaca() == personagem.getLocal().getCidade().getRaca());
        } catch (NullPointerException ex) {
            return false;
        }
    }

    /**
     * Is there something in this character's hex that an attack order could actually engage: an enemy
     * ARMY, or an enemy CITY whose garrison would defend it. Used to keep "Attack Enemy" out of the
     * order combo when there is nothing to attack, after a player reported a beginner picking it in
     * an empty hex.
     * <p>
     * Enemy means {@code Nacao.isInimigo}, a negative relationship - NOT {@code isInCidadeAlheio},
     * which counts any city that is not your own and so would treat an ALLIED city as a target.
     * <p>
     * Deliberately reads only what the client can see: an army at visibility 0 is dropped on export
     * ({@code ServerExercitoDao}) and is simply absent here, so a hidden force makes this return
     * false. That is why the requisito it backs must stay bypassable - the ALL checkbox skips
     * requisitos entirely, which is the escape hatch for attacking a force you know about but cannot
     * see.
     *
     * @param personagem the character
     * @return true when an enemy army or an enemy city shares the hex
     */
    public boolean isInHexInimigo(Personagem personagem) {
        try {
            final Nacao minha = personagem.getNacao();
            final Cidade cidade = personagem.getLocal().getCidade();
            if (cidade != null && cidade.getTamanho() >= 1 && cidade.getNacao() != null
                    && cidade.getNacao().isInimigo(minha)) {
                return true;   //an enemy city defends itself with its garrison
            }
            for (Exercito exercito : personagem.getLocal().getExercitos().values()) {
                if (exercito.getNacao() != null && exercito.getNacao().isInimigo(minha)) {
                    return true;
                }
            }
        } catch (NullPointerException ex) {
            return false;
        }
        return false;
    }

    public boolean isInCidadeAlheio(Personagem personagem) {
        try {
            return (this.isInCidade(personagem)
                    && personagem.getLocal().getCidade().getNacao() != personagem.getNacao());
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isInCidade(Personagem personagem) {
        try {
            return (personagem.getLocal().getCidade() != null && personagem.getLocal().getCidade().getTamanho() >= 1);
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isInCidadeInimiga(Personagem personagem) {
        return this.isInCidadeAlheio(personagem);
    }

    public boolean isInCidadeAliada(Personagem personagem) {
        //cidade propria ou aliada
        try {
            return (this.isInCidadePropria(personagem)
                    || personagem.getLocal().getCidade().getNacao().getRelacionamento(personagem.getNacao()) >= 2);
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isInCidadeVassalo(Personagem personagem) {
        //cidade propria ou aliada
        try {
            if (this.isInCidadePropria(personagem)) {
                return true;
            } else if (personagem.getLocal().getCidade().getNacao().getRelacionamento(personagem.getNacao()) == 3) {
                return true;
            } else {
                if (personagem.getNacao().hasHabilidade(";PRA;")
                        && personagem.getLocal().getCidade().getNacao().getRelacionamento(personagem.getNacao()) >= 2) {
                    return true;
                }
            }
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isLocalConhecido(Personagem personagem) {
        boolean ret = true;
        if (personagem.getVida() <= 0) {
            ret = false;
        } else if (personagem.isRefem()) {
            ret = false;
        }
        return ret;
    }

    public boolean isTerrainLandmark(Personagem personagem) {
        return localFacade.isTerrainLandmark(personagem.getLocal());
    }

    public boolean isTerrainLandmarkSpire(Personagem personagem) {
        return localFacade.isTerrainLandmarkSpire(personagem.getLocal());
    }

    public boolean isMorto(Personagem personagem) {
        return (personagem.getVida() <= 0);
    }

    public boolean isAtivo(Personagem personagem) {
        return isLocalConhecido(personagem);
    }

    public List<String[]> getPericias(Personagem personagem, Cenario cenario) {
        int aTipo = 0, aTitulo = 1, aNatural = 2, aFinal = 3;
        List<String[]> pericias = new ArrayList<>();
        if (isMorto(personagem)) {
            return pericias;
        }
        CenarioFacade cenarioFacade = new CenarioFacade();
        if (personagem.isComandante()) {
            String[] temp = new String[4];
            temp[aTipo] = labels.getString(cenarioFacade.getTituloClasse(cenario, CenarioFacade.COMANDANTE));
            temp[aTitulo] = cenarioFacade.getTituloPericia(cenario, CenarioFacade.COMANDANTE, personagem.getPericiaComandanteNatural());
            temp[aNatural] = personagem.getPericiaComandanteNatural() + "";
            temp[aFinal] = personagem.getPericiaComandante() + "";
            pericias.add(temp);
        }
        if (personagem.isMago()) {
            String[] temp = new String[4];
            temp[aTipo] = labels.getString(cenarioFacade.getTituloClasse(cenario, CenarioFacade.WIZARD));
            temp[aTitulo] = cenarioFacade.getTituloPericia(cenario, CenarioFacade.WIZARD, personagem.getPericiaMagoNatural());
            temp[aNatural] = personagem.getPericiaMagoNatural() + "";
            temp[aFinal] = personagem.getPericiaMago() + "";
            pericias.add(temp);
        }
        if (personagem.isEmissario()) {
            String[] temp = new String[4];
            temp[aTipo] = labels.getString(cenarioFacade.getTituloClasse(cenario, CenarioFacade.DIPLOMAT));
            temp[aTitulo] = cenarioFacade.getTituloPericia(cenario, CenarioFacade.DIPLOMAT, personagem.getPericiaEmissarioNatural());
            temp[aNatural] = personagem.getPericiaEmissarioNatural() + "";
            temp[aFinal] = personagem.getPericiaEmissario() + "";
            pericias.add(temp);
        }
        if (personagem.isAgente()) {
            String[] temp = new String[4];
            temp[aTipo] = labels.getString(cenarioFacade.getTituloClasse(cenario, CenarioFacade.ROGUE));
            temp[aTitulo] = cenarioFacade.getTituloPericia(cenario, CenarioFacade.ROGUE, personagem.getPericiaAgenteNatural());
            temp[aNatural] = personagem.getPericiaAgenteNatural() + "";
            temp[aFinal] = personagem.getPericiaAgente() + "";
            pericias.add(temp);
        }
        if (personagem.getPericiaFurtividade() > 0) {
            String[] temp = new String[4];
            temp[aTipo] = labels.getString("FURTIVIDADE");
            temp[aTitulo] = "";
            temp[aNatural] = personagem.getPericiaFurtividadeNatural() + "";
            temp[aFinal] = personagem.getPericiaFurtividade() + "";
            pericias.add(temp);
        }
        return pericias;
    }

    public String[] getVida(Personagem personagem) {
        int aTipo = 0, aTitulo = 1, aNatural = 2;
        String[] temp = new String[3];
        temp[aTipo] = labels.getString("VITALIDADE");
        if (isMorto(personagem)) {
            temp[aTitulo] = labels.getString("MORTO");
        } else {
            int nn = (int) (personagem.getVida() / 10);
            temp[aTitulo] = BaseMsgs.tituloAtributoVida[Math.min(nn, BaseMsgs.tituloAtributoVida.length - 1)];
        }
        temp[aNatural] = personagem.getVida() + "";
        return temp;
    }

    public String[] getDueloNatural(Personagem personagem) {
        int aTipo = 0, aTitulo = 1, aNatural = 2, aFinal = 3;
        String[] temp = new String[4];
        if (!isMorto(personagem)) {
            temp[aTipo] = labels.getString("DUELO");
            temp[aNatural] = doCalculaDueloNatural(personagem) + "";
            temp[aFinal] = personagem.getDuelo() + "";
        }
        return temp;
    }

    public int getDuelo(Personagem personagem) {
        if (!isMorto(personagem)) {
            return personagem.getDuelo();
        }
        return 0;
    }

    /**
     * calcula o duelo NATURAL do personagem
     */
    private int doCalculaDueloNatural(Personagem personagem) {
        /**
         * calcular o duelo: definir maior duelo por pericia, com artefatos ai somar 25% dos duelos das demais pericias. ai somar bonus de duelo e bonus de
         * artefato de combate
         */
        float duelo;
        duelo = getMaiorDueloNatural(personagem);
        duelo += (personagem.getPericiaComandanteNatural()
                + personagem.getPericiaAgenteNatural() * 0.75F
                + personagem.getPericiaEmissarioNatural() * 0.50F
                + personagem.getPericiaMagoNatural()) * 0.25F;
        duelo -= (getMaiorDueloNatural(personagem) * 0.25F);
        if (personagem.isArtefatoCombateAtivo()) {
            duelo += personagem.getArtefatoCombateAtivo().getValor() / 50;
            duelo += personagem.getDueloBonus();
        }
        return (int) duelo;
    }

    /**
     * retorna pericia com maior duelo, contando atributos, que o personagem tem
     */
    private int getMaiorDueloNatural(Personagem personagem) {
        double[] duelos = new double[4];
        duelos[0] = personagem.getPericiaComandanteNatural();
        duelos[1] = personagem.getPericiaAgenteNatural() * 0.75F;
        duelos[2] = personagem.getPericiaEmissarioNatural() * 0.50F;
        duelos[3] = personagem.getPericiaMagoNatural();
        Arrays.sort(duelos);
        return (int) duelos[3];
    }

    public boolean isArtefatoAtivo(Personagem personagem, Artefato artefato) {
        if (!personagem.isArtefato(artefato)) {
            return false;
        } else if (personagem.getArtefatoCombateAtivo() == artefato) {
            return true;
        } else if (isArtefatoPodeUsar(personagem, artefato)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isArtefatoPodeUsar(Personagem personagem, Artefato artefato) {
        boolean ret = false;
        if (artefato.isComandante() && personagem.isComandante()) {
            ret = true;
        } else if (artefato.isAgente() && personagem.isAgente()) {
            ret = true;
        } else if (artefato.isEmissario() && personagem.isEmissario()) {
            ret = true;
        } else if (artefato.isMago() && personagem.isMago()) {
            ret = true;
        } else if (artefato.isFurtividade()) {
            ret = true;
        }
        return ret;
    }

    /*
     * Níveis : Comandante 0 Agente 0 Emissário 65 Mago 0 Vitalidade 100
     * Furtividade 0 Duelo 32 Artefatos : Nenhum Feiticos (+0) : Nenhum
     * ________________________________________ A Musa se encontrava em 1919
     * (Litoral). A nação não recebeu nenhuma ordem. A Musa está em 1919
     * (Litoral), Aldeia de Rhodes da nação Persia.
     */
    public String getResultado(Personagem personagem) {
        if (personagem == null) {
            return labels.getString("NENHUM");
        }
        //FIXME: tratamento para personagem de outras nacoes, em especial as inimigas.
        String ret = "";
        //imprime resultados do personagem:
        /*
         * A Musa se encontrava em 1919 (Litoral). A nação não recebeu nenhuma
         * ordem. A Musa está em 1919 (Litoral), Aldeia de Rhodes da nação
         * Persia.
         */
        //estava em:
        Local localOrigem = this.getLocalOrigem(personagem);
        Local localAtual = this.getLocal(personagem);
        if (localOrigem != null) {
            if (this.isLocalConhecido(personagem) && localAtual != localOrigem) {
                ret += String.format(labels.getString("PERSONAGEM.ENCONTRAVA.EM"),
                        personagem.getNome(),
                        LocalFacade.getCoordenadas(localOrigem),
                        localFacade.getTerrenoNome(localOrigem));
                ret += "\n";
            } else {
                //continua no mesmo local e local eh conhecido. nao imprime mensagem.
            }
        } else {
            ret += String.format(labels.getString("PERSONAGEM.ENCONTRAVA.EM.DESCONHECIDO"),
                    personagem.getNome());
            ret += "\n";
        }
        if (personagem.isDoubleAgent()) {
            ret += String.format(labels.getString("DUPLO.DE"), nacaoFacade.getNome(getNacaoSubordinada(personagem)));
            ret += "\n";
        }
        //ordens:
        if (personagem.getResultados() != null && !personagem.getResultados().equals("")) {
            ret += SysApoio.stringParse(personagem.getResultados(), labels) + "\n";
        }
        ret += getResultadoLocal(personagem);
        return ret;
    }

    public String getResultadoLocal(Personagem personagem) {
        if (personagem == null) {
            return labels.getString("NENHUM");
        }
        String ret = "";
        if (this.isMorto(personagem)) {
            ret += String.format(labels.getString("PERSONAGEM.CORPO.EM"),
                    personagem.getNome(), personagem.getLocal().getCoordenadas());
//            ret += " " + labels.getString("VITALIDADE.MORTO");
            ret += "\n";
        } else if (this.isLocalConhecido(personagem)) {
            //esta em:
            Local localAtual = this.getLocal(personagem);
            if (localAtual != null) {
                ret += String.format(labels.getString("ESTA.EM"),
                        personagem.getNome(),
                        LocalFacade.getCoordenadas(localAtual),
                        localFacade.getTerrenoNome(localAtual));
                if (localFacade.isCidade(localAtual, this.getNacao(personagem))) {
                    Cidade cidade = localFacade.getCidade(localAtual);
                    //", Vila/Torre de Larissa da nação Macedonia.\n"
                    ret += String.format(labels.getString("TAMANHO.CIDADE.NACAO"),
                            cidadeFacade.getTamanhoFortificacao(cidade),
                            cidadeFacade.getNome(cidade),
                            cidadeFacade.getNacaoNome(cidade));
                }
                ret += ".";
            } else {
                ret += String.format(labels.getString("ESTA.LOCAL.DESCONHECIDO"),
                        personagem.getNome());
            }
        } else {
            ret += String.format(labels.getString("ESTA.LOCAL.DESCONHECIDO"),
                    personagem.getNome());
        }
        ret += "\n";

        //ret += String.format("\n%s (%s)\n", personagem.getNome(), personagem.getCodigo());
        //FIXME: Tratamento para personagem refem
        if (this.isComandaExercito(personagem)) {
            //PENDING: exercito ou esquadra?
            ret += String.format(labels.getString("PERSONAGEM.COMANDA.EXERCITO.ESQUADRA"), personagem.getNome());
            ret += "\n";
            ret += getAcompanhantes(personagem);
            ret += "\n";
        }
        if (this.isComandaGrupo(personagem)) {
            ret += String.format(labels.getString("PERSONAGEM.COMANDA.GRUPO.NOME"), personagem.getNome());
            ret += "\n";
            ret += getAcompanhantes(personagem);
            ret += "\n";
        }
        if (personagem.getLider() != null) {
            ret += String.format(labels.getString("PERSONAGEM.VIAJA.ACOMPANHANDO"), personagem.getLider().getNome());
            ret += "\n";
        }
        //PENDING: Refem, viajando com, ...
        //ret += String.format("Duelo: %d\n", personagem.getDueloNatural());
        return ret;
    }

    //personagens viajando com o comandante?
    private static String getAcompanhantes(Personagem personagem) {
        SortedMap<String, Personagem> liderados = personagem.getLiderados();
        if (liderados.size() > 0) {
            String msg = String.format(labels.getString("PERSONAGEM.VIAJA.ACOMPANHADO.POR.NOME"), personagem.getNome());
            for (Personagem elem : liderados.values()) {
                msg += String.format("\n   - %s", elem.getNome());
            }
            return msg;
        } else {
            return ".";
        }
    }

    public boolean isDoubleAgent(Personagem personagem) {
        return personagem.isDoubleAgent();
    }

    public Nacao getNacaoSubordinada(Personagem personagem) {
        return personagem.getNacaoSubordinada();
    }

    public Exercito getExercito(Personagem personagem) {
        return personagem.getExercito();
    }

    public Exercito getExercitoViajando(Personagem personagem) {
        if (personagem.isComandaExercito()) {
            return personagem.getExercito();
        } else if (this.isInExercito(personagem)) {
            return personagem.getLider().getExercito();
        } else {
            return null;
        }
    }

    public boolean hasExtraOrdem(Personagem personagem) {
        return (personagem.getOrdensExtraQt() > 0);
    }

    public String getFeiticoNome(PersonagemFeitico magia) {
        return magia.getFeitico().getNome();
    }

    public String getFeiticoTomo(PersonagemFeitico magia) {
        return magia.getFeitico().getLivroFeitico();
    }

    public int getFeiticoHabilidade(PersonagemFeitico magia) {
        return magia.getHabilidade();
    }

    public boolean isInCidadePropriaRaca(Personagem personagem) {
        try {
            return (personagem.getNacao().getRaca() == personagem.getLocal().getCidade().getRaca());
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isPodeMoverCidade(Personagem personagem) {
        try {
            if (personagem.getNacao().hasHabilidade(";PKM;")) {
                return true;
            } else {
                return (personagem.getNacao().hasHabilidadeNacao("0036"));
            }
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public boolean isPersonagemHasItem(Personagem personagem, String type) {
        if (personagem.getArtefatos().isEmpty()) {
            return false;
        } else if (type.equals("Any")) {
            return true;
        }
        for (Artefato artefato : personagem.getArtefatos().values()) {
            if (type.equals("Scry") && artefato.isExploracao()) {
                return true;
            }
            if (type.equals("Summon") && artefato.isSummon()) {
                return true;
            }
            if (type.equals("DragonEgg") && artefato.isDragonEgg()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasArtefatos(Personagem personagem) {
        return !personagem.getArtefatos().isEmpty();
    }

    public int getUpkeepMoney(Personagem personagem, Cenario scenario) {
        int ret = 0;
        if (scenario.hasHabilidade(";SUP;")) {
            return 0;
        } else if (personagem.getNacao().hasHabilidade(";PUC;")) {
            //Free People: Character's upkeep cost %s%% less
            ret += personagem.getPericiaNaturalTotal() * 20 * (100 - personagem.getNacao().getHabilidadeValor(";PUC;")) / 100;
        } else if (personagem.getNacao().hasHabilidade(";PUC5;")) {
            //Free People: Character's upkeep cost %s%% less
            ret += personagem.getPericiaNaturalTotal() * 20 * (100 - personagem.getNacao().getHabilidadeValor(";PUC5;")) / 100;
        } else {
            ret += personagem.getPericiaNaturalTotal() * 20;
        }
        return ret;
    }

    public String getInfoShort(Personagem personagem) {
        String ret = getPericiasShort(personagem);
        if (this.isComandaEsquadra(personagem)) {
            ret += " " + labels.getString("ESQUADRA");
        } else if (this.isComandaExercito(personagem)) {
            ret += " " + labels.getString("EXERCITO");
        } else if (this.isComandaGrupo(personagem)) {
            ret += " " + labels.getString("GRUPO");
        }
        return ret;
    }

    public String getPericiasShort(Personagem personagem) {
        if (isMorto(personagem)) {
            return labels.getString("MORTO");
        }
        String skills = "";
        if (personagem.isComandante()) {
            skills += " C" + personagem.getPericiaComandante();
        }
        if (personagem.isMago()) {
            skills += " W" + personagem.getPericiaMago();
        }
        if (personagem.isEmissario()) {
            skills += " D" + personagem.getPericiaEmissario();
        }
        if (personagem.isAgente()) {
            skills += " R" + personagem.getPericiaAgente();
        }
        if (personagem.getPericiaFurtividade() > 0) {
            skills += " S" + personagem.getPericiaFurtividade();
        }
        skills += " L" + personagem.getVida();
        skills += " C" + personagem.getDuelo();
        return skills;
    }

    public Collection<Habilidade> getHabilidades(Personagem pers) {
        Collection<Habilidade> ret = new ArrayList<>();
        for (Habilidade hab : pers.getHabilidades().values()) {
            if (hab.getCodigo().equals(";-;") || hab.isHidden()) {
                continue;
            }
            ret.add(hab);
        }
        return ret;
    }
}
