/*
 * SysReport.java
 *
 * Created on 30 de Marco de 2007, 10:33
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package persistence.reports;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import javax.imageio.ImageIO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SysApoio;
import persistenceLocal.PathFactory;

/**
 *
 * @author gurgel
 */
public class SysReport implements Serializable {

    private static final Log log = LogFactory.getLog(SysReport.class);
    private PrintStream console = System.out;
    private SysPdf pdf;
    private String baseDir;
    private boolean printToRoot = false;
    private boolean fileOpen = false;
    private boolean convertNewLine = false;
    private boolean nowrap = true;
    private int border = 0;
    private final String tableHeader = "<table cellspacing=%d cellpadding=%d border=%d align=center width=100%%>";

    /**
     * Creates a new instance of SysReport
     */
    public SysReport() {
    }

    public void consoleDesvia(String nmFolder, String nmFile, int turno, boolean temPdf) {
        doCreateFolders(nmFolder, turno);
        String nmBaseFile = getBasedir() + nmFile;
        String nmFileHtml = nmBaseFile + ".htm";
        //prepara HTML
        try {
            if ((new File(nmFileHtml)).exists()) {
                boolean success = (new File(nmFileHtml)).delete();
            }
            System.setOut(new PrintStream(new FileOutputStream(nmFileHtml, true)));
        } catch (FileNotFoundException ex) {
            log.error("Problemas gerando arquivo HTML.");
            throw new UnsupportedOperationException("Problemas gerando arquivo HTML.");
        }
        this.impHtml("<html><head>"
                + "<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"/>"
                + "</head><body><pre>");
        //prepara PDF
        if (temPdf) {
            String nmFilePdf = nmBaseFile + ".pdf";
            this.setPdf(new SysPdf(nmFilePdf));
        }
        this.setFileOpen(true);
    }

    /**
     * this one makes sure the folders are created, but does not create a file
     *
     * @param nmFolder
     * @param turno
     */
    public void doCreateFolders(String nmFolder, int turno) {
        setBasedir(nmFolder, turno);
        PathFactory.getInstance().criaBaseDir(nmFolder, turno);
    }

    public void consoleDesviaClose() {
        if (this.isFileOpen()) {
            this.impHtml("</pre></body></html>");
            if (this.isAtivo()) {
                this.getPdf().fechaPdf();
            }
        }
        System.setOut(this.getConsole());
        this.setFileOpen(false);
    }

    private void setBasedir(String nmFolder, int turno) {
        //diretorio para a partida/turno e mapas HTM
        if (isPrintToRoot()) {
            this.baseDir = nmFolder;
        } else {
            this.baseDir = nmFolder + SysApoio.lpad(turno + "", '0', 3) + "/";
        }
    }

    public String getMapadir() {
        return getBasedir() + "mapas/";
    }

    public void salvaImagem(RenderedImage rendImage, String filename) {
        // Write generated image to a file
        try {
            // Save as PNG
            File file = new File(this.getMapadir() + filename);
            ImageIO.write(rendImage, "png", file);
            // Save as JPG
//            file = new File(this.getBasedir() + filename + ".jpg");
//            ImageIO.write(rendImage, "jpg", file);
        } catch (java.io.FileNotFoundException ex) {
            log.error("Problemas no SysReport");
            throw new UnsupportedOperationException("Problemas no SysReport", ex);
        } catch (IOException ex) {
            log.error("Problemas no SysReport", ex);
        }
    }

    public DecimalFormat getMyFormatter() {
        return (SysApoio.getMyFormatter());
    }

    public PrintStream getConsole() {
        return console;
    }

    public void setConsole(PrintStream console) {
        this.console = console;
    }

    public SysPdf getPdf() {
        return pdf;
    }

    public void setPdf(SysPdf pdf) {
        this.pdf = pdf;
    }

    public boolean isAtivo() {
        try {
            return this.getPdf().isAtivo();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public void writeImg(BufferedImage imagem, String fileName) {
        this.salvaImagem(imagem, fileName);
        this.impHtml("<p align='center'><img src=mapas/" + fileName + "></p>");
        if (this.isAtivo()) {
            this.getPdf().escreveImagem(imagem);
        }
    }

    public void writeImgToPdfOnly(BufferedImage imagem, String fileName) {
        if (this.isAtivo()) {
            this.getPdf().escreveCelulaImagem(imagem);
        }
    }

    public void write(String linha) {
        if (!linha.equals("")) {
            this.impHtml(linha);
            if (this.isAtivo()) {
                this.getPdf().imp(linha);
            }
        }
    }

    public void writeBold(String linha) {
        if (!linha.equals("")) {
            this.impHtml(String.format("<b>%s</b>", linha));
            if (this.isAtivo()) {
                this.getPdf().imp(linha, true);
            }
        }
    }

    public void writeBoldFirstLine(String linha) {
        if (!linha.equals("")) {
            String[] linhas = linha.split("\n", 2);
            boolean first = true;
            for (String texto : linhas) {
                if (first) {
                    this.writeBold(texto);
                    first = false;
                } else {
                    this.write(texto);
                }
            }
        }
    }

    public void writeTitle(String linha) {
        this.impHtml("<h2><div align=Center>" + linha + "</div></h2>");
        if (this.isAtivo()) {
            this.getPdf().impTitulo(linha);
        }
    }

    public void newLine() {
        SysApoio.imp();
        if (this.isAtivo()) {
            this.getPdf().imp();
        }
    }

    public void newPage() {
        if (this.isAtivo()) {
            this.getPdf().novaPagina();
        }
    }

    public void lineSeparator() {
        String lineSeparator = "\n<hr>\n";
        this.impHtml(lineSeparator);
        if (this.isAtivo()) {
            this.getPdf().imp();
        }
    }

    public void writeTabela(float[] qtColunas) {
        this.impHtml(String.format(tableHeader, 2, 2, getBorder()));
        if (this.isAtivo()) {
            this.getPdf().escreveTabela(qtColunas);
        }
        this.writeTabelaLine();
    }

    public void writeTabela(int qtColunas) {
        this.impHtml(String.format(tableHeader, 2, 2, getBorder()));
        if (this.isAtivo()) {
            this.getPdf().escreveTabela(qtColunas);
        }
        this.writeTabelaLine();
    }

    public void writeCelulaPre(String texto) {
        this.impHtml("<td " + getNowrap() + "><pre>" + texto + "</pre></td>");
        if (this.isAtivo()) {
            this.getPdf().escreveCelula(texto);
        }
    }

    public void writeCelula(String texto) {
        this.impHtml("<td " + getNowrap() + ">" + texto + "</td>");
        if (this.isAtivo()) {
            this.getPdf().escreveCelula(texto);
        }
    }

    public void writeCelulaPortrait(String texto, BufferedImage imagem, String filename) {
//        this.salvaImagem(imagem, filename);
        this.impHtml("<td nowrap valign=top>");
        this.impHtml("<img src='" + PathFactory.getPortraitDirName() + filename + "'>");
        this.impHtml("<div align=left>" + texto + "</div>");
        if (this.isAtivo()) {
            this.getPdf().escreveCelula(texto, PathFactory.getPortraitDirName() + filename);
        }
    }

    public void writeTabelaLine() {
        this.impHtml("<tr>");
//        if(this.isAtivo()) {
//            this.getPdf().imp();
//        }
    }

    public void writeTabelaFecha() {
        this.impHtml("</table>");
        if (this.isAtivo()) {
            this.getPdf().fechaTabela();
        }
    }

    public void writeSubTabela(float[] qtColunas) {
        this.impHtml(String.format("<td>" + tableHeader, 0, 0, getBorder()));
        if (this.isAtivo()) {
            this.getPdf().escreveSubTabela(qtColunas);
        }
        this.writeSubTabelaLine();
    }

    public void writeSubTabela(int qtColunas) {
        this.impHtml(String.format("<td>" + tableHeader, 0, 0, getBorder()));
        if (this.isAtivo()) {
            this.getPdf().escreveSubTabela(qtColunas);
        }
        this.writeSubTabelaLine();
    }

    public void writeSubCelulaPre(String texto) {
        this.impHtml("<td " + getNowrap() + "><pre>" + texto + "</pre></td>");
        if (this.isAtivo()) {
            this.getPdf().escreveCelula(texto);
        }
    }

    public void writeSubCelula(String texto) {
        this.impHtml("<td " + getNowrap() + ">" + texto + "</td>");
        if (this.isAtivo()) {
            this.getPdf().escreveSubCelula(texto);
        }
    }

    public void writeSubCelulaBold(String texto) {
        this.impHtml("<td " + getNowrap() + "><b>" + texto + "</b></td>");
        if (this.isAtivo()) {
            this.getPdf().escreveSubCelulaBold(texto);
        }
    }

    public void writeSubTabelaLine() {
        this.impHtml("<tr>");
//        if(this.isAtivo()) {
//            this.getPdf().imp();
//        }
    }

    public void writeSubTabelaFecha() {
        this.impHtml("</table>");
        if (this.isAtivo()) {
            this.getPdf().fechaSubTabela();
        }
    }

    public void writeCelulaBold(String texto) {
        this.impHtml("<td " + getNowrap() + "><b>" + texto + "</b></td>");
        if (this.isAtivo()) {
            this.getPdf().escreveCelulaBold(texto);
        }
    }

    public void writeCelulaTitle(String texto) {
        this.impHtml("<td " + getNowrap() + " colspan=10><b>" + texto + "</b></td>");
        if (this.isAtivo()) {
            this.getPdf().escreveCelulaTitulo(texto);
        }
        writeTabelaLine();
    }

    public String getBasedir() {
        return this.baseDir;
    }

    /**
     * @return the fileOpen
     */
    private boolean isFileOpen() {
        return fileOpen;
    }

    /**
     * @param isfileopen the fileOpen to set
     */
    private void setFileOpen(boolean isfileopen) {
        this.fileOpen = isfileopen;
    }

    /**
     * @return the printToRoot
     */
    public boolean isPrintToRoot() {
        return printToRoot;
    }

    /**
     * @param printToRoot the printToRoot to set
     */
    public void setPrintToRoot(boolean printToRoot) {
        this.printToRoot = printToRoot;
    }

    /**
     * @return the border
     */
    public int getBorder() {
        return border;
    }

    /**
     * can only be user after cosoleDesvia
     *
     * @param border the border to set
     */
    public void setBorder(int border) {
        this.border = border;
        if (this.isAtivo()) {
            this.getPdf().setBorder(border);
        }
    }

    /**
     * @return the nowrap
     */
    public boolean isNowrap() {
        return nowrap;
    }

    /**
     * @param nowrap the nowrap to set
     */
    public void setNowrap(boolean nowrap) {
        this.nowrap = nowrap;
    }

    private String getNowrap() {
        if (nowrap) {
            return "nowrap";
        } else {
            return "";
        }
    }

    private void impHtml(String linha) {
        System.out.println(this.ascToHtml(linha));
    }

    private void impHtml(int linha) {
        System.out.println(linha);
    }

    private void impHtml(long linha) {
        System.out.println(linha);
    }

    /**
     * HTML entity per character, indexed by code point (all mapped characters are &lt; 0x180).
     * Replaces a chain of 68 {@code String.replaceAll} calls, each of which compiled a fresh regex
     * Pattern and rescanned the whole string - about 70 Pattern.compile + 70 scans PER REPORT CELL,
     * for every character, city and army of every nation. Profiling the Judge (3 jstack samples,
     * 2026-08-09) put the "Escrevendo ROs" phase squarely in Pattern.compile/Matcher.find here.
     *
     * Byte-identical to the old chain by construction: every source is a single character, no source
     * character appears in any replacement, so sequential replacement and one left-to-right pass
     * cannot differ. SysReportEscapeTest pins that against a copy of the original chain.
     */
    private static final String[] ESCAPE = new String[0x180];
    private static final String TAB_SPACES = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";

    static {
        ESCAPE[0x00C0] = "&Agrave;";   // Agrave
        ESCAPE[0x00C1] = "&Aacute;";   // Aacute
        ESCAPE[0x00C2] = "&Acirc;";   // Acirc
        ESCAPE[0x00C3] = "&Atilde;";   // Atilde
        ESCAPE[0x00C4] = "&Auml;";   // Auml
        ESCAPE[0x00C5] = "&Aring;";   // Aring
        ESCAPE[0x00C6] = "&AElig;";   // AElig
        ESCAPE[0x00C7] = "&Ccedil;";   // Ccedil
        ESCAPE[0x00C8] = "&Egrave;";   // Egrave
        ESCAPE[0x00C9] = "&Eacute;";   // Eacute
        ESCAPE[0x00CA] = "&Ecirc;";   // Ecirc
        ESCAPE[0x00CB] = "&Euml;";   // Euml
        ESCAPE[0x00CC] = "&Igrave;";   // Igrave
        ESCAPE[0x00CD] = "&Iacute;";   // Iacute
        ESCAPE[0x00CE] = "&Icirc;";   // Icirc
        ESCAPE[0x00CF] = "&Iuml;";   // Iuml
        ESCAPE[0x00D0] = "&ETH;";   // ETH
        ESCAPE[0x00D1] = "&Ntilde;";   // Ntilde
        ESCAPE[0x0152] = "&OElig;";   // OElig
        ESCAPE[0x00D2] = "&Ograve;";   // Ograve
        ESCAPE[0x00D3] = "&Oacute;";   // Oacute
        ESCAPE[0x00D4] = "&Ocirc;";   // Ocirc
        ESCAPE[0x00D5] = "&Otilde;";   // Otilde
        ESCAPE[0x00D6] = "&Ouml;";   // Ouml
        ESCAPE[0x00D8] = "&Oslash;";   // Oslash
        ESCAPE[0x0160] = "&Scaron;";   // Scaron
        ESCAPE[0x00D9] = "&Ugrave;";   // Ugrave
        ESCAPE[0x00DA] = "&Uacute;";   // Uacute
        ESCAPE[0x00DB] = "&Ucirc;";   // Ucirc
        ESCAPE[0x00DC] = "&Uuml;";   // Uuml
        ESCAPE[0x00DD] = "&Yacute;";   // Yacute
        ESCAPE[0x00DE] = "&THORN;";   // THORN
        ESCAPE[0x0178] = "&Yuml;";   // Yuml
        ESCAPE[0x00E0] = "&agrave;";   // agrave
        ESCAPE[0x00E1] = "&aacute;";   // aacute
        ESCAPE[0x00E2] = "&acirc;";   // acirc
        ESCAPE[0x00E3] = "&atilde;";   // atilde
        ESCAPE[0x00E4] = "&auml;";   // auml
        ESCAPE[0x00E5] = "&aring;";   // aring
        ESCAPE[0x00E6] = "&aelig;";   // aelig
        ESCAPE[0x00E7] = "&ccedil;";   // ccedil
        ESCAPE[0x00E8] = "&egrave;";   // egrave
        ESCAPE[0x00E9] = "&eacute;";   // eacute
        ESCAPE[0x00EA] = "&ecirc;";   // ecirc
        ESCAPE[0x00EB] = "&euml;";   // euml
        ESCAPE[0x00EC] = "&igrave;";   // igrave
        ESCAPE[0x00ED] = "&iacute;";   // iacute
        ESCAPE[0x00EE] = "&icirc;";   // icirc
        ESCAPE[0x00EF] = "&iuml;";   // iuml
        ESCAPE[0x00F0] = "&eth;";   // eth
        ESCAPE[0x00F1] = "&ntilde;";   // ntilde
        ESCAPE[0x0153] = "&oelig;";   // oelig
        ESCAPE[0x00F2] = "&ograve;";   // ograve
        ESCAPE[0x00F3] = "&oacute;";   // oacute
        ESCAPE[0x00F4] = "&ocirc;";   // ocirc
        ESCAPE[0x00F5] = "&otilde;";   // otilde
        ESCAPE[0x00F6] = "&ouml;";   // ouml
        ESCAPE[0x00F8] = "&oslash;";   // oslash
        ESCAPE[0x0161] = "&scaron;";   // scaron
        ESCAPE[0x00F9] = "&ugrave;";   // ugrave
        ESCAPE[0x00FA] = "&uacute;";   // uacute
        ESCAPE[0x00FB] = "&ucirc;";   // ucirc
        ESCAPE[0x00FC] = "&uuml;";   // uuml
        ESCAPE[0x00FD] = "&yacute;";   // yacute
        ESCAPE[0x00FE] = "&thorn;";   // thorn
        ESCAPE[0x00FF] = "&yuml;";   // yuml
    }

    /**
     * Escape accented characters to HTML entities so player/character names render correctly instead
     * of as diamonds. Single pass, and allocates nothing when the text has no mapped character (the
     * common case for a report cell).
     *
     * @param original text to escape (NPEs on null, as the previous implementation did)
     * @param convertNewLine when true, newlines become &lt;br&gt;
     */
    static String escapeHtml(String original, boolean convertNewLine) {
        final int len = original.length();
        StringBuilder sb = null;                      // stays null while nothing needs escaping
        for (int ii = 0; ii < len; ii++) {
            final char cc = original.charAt(ii);
            final String replacement;
            if (cc == '\n') {
                replacement = convertNewLine ? "<br>" : null;
            } else if (cc == '\t') {
                replacement = TAB_SPACES;
            } else {
                replacement = (cc < ESCAPE.length) ? ESCAPE[cc] : null;
            }
            if (replacement == null) {
                if (sb != null) {
                    sb.append(cc);
                }
                continue;
            }
            if (sb == null) {
                sb = new StringBuilder(len + 32).append(original, 0, ii);
            }
            sb.append(replacement);
        }
        return (sb == null) ? original : sb.toString();
    }

    private String ascToHtml(String original) {
        return escapeHtml(original, isConvertNewLine());
    }

    /**
     * @return the convertNewLine
     */
    public boolean isConvertNewLine() {
        return convertNewLine;
    }

    /**
     * @param convertNewLine the convertNewLine to set
     */
    public void setConvertNewLine(boolean convertNewLine) {
        this.convertNewLine = convertNewLine;
    }
}
