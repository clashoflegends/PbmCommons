/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package persistenceLocal;

import baseLib.ExtensionFileFilter;
import business.services.ComparatorFactory;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import model.Partida;
import model.World;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 *
 * @author jmoura
 */
public class PathFactory implements Serializable {

    private static final Log log = LogFactory.getLog(PathFactory.class);
    private static final BundleManager label = SettingsManager.getInstance().getBundleManager();
    private static PathFactory instance;

    public synchronized static PathFactory getInstance() {
        if (PathFactory.instance == null) {
            PathFactory.instance = new PathFactory();
        }
        return PathFactory.instance;
    }

    public static String getWorldFileName(World world) {
        return String.format(label.getString("FILENAME.WORLD"),
                world.getPartida().getNome(),
                world.getPartida().getId());
    }

    public static String getFileName(Partida partida) {
        return String.format(label.getString("FILENAME.SAVE.PARTIDA"),
                partida.getId(),
                partida.getTurno(),
                partida.getJogadorAtivo().getLogin());
    }

    public static String getFileNameRegras(Partida partida) {
        return String.format(label.getString("FILENAME.SAVE.REGRAS"),
                partida.getId());
    }

    public static String getDirName(Partida partida) {
        return SettingsManager.getInstance().getSaveDir() + String.format("%s_%s/",
                SysApoio.lpad(partida.getId() + "", '0', 3),
                partida.getCodigo());
    }

    public static String getPortraitDirName() {
        return SettingsManager.getInstance().getSaveDir() + "images/Pers/";
    }

    public String getStatsDirName() {
        return SettingsManager.getInstance().getSaveStatsDir();
    }

    public String getStatsFileName(int idGame, String nmGame) {
        return String.format(SettingsManager.getInstance().getSaveStatsDir() + label.getString("XLS.STATS.FILENAME"), idGame, nmGame);
    }

    public static ExtensionFileFilter getFilterAcoes() {
        return new ExtensionFileFilter(label.getString("FILTRO.ACOES"), "rc.egf", "");
    }

    public static ExtensionFileFilter getFilterImages() {
        return new ExtensionFileFilter(label.getString("FILTRO.IMAGE"), "png", "");
    }

    public static ExtensionFileFilter getFilterAcoesImport() {
        return new ExtensionFileFilter(label.getString("FILTRO.ACOES"), "egf", "o");
    }

    public static ExtensionFileFilter getFilterWorld() {
        return new ExtensionFileFilter(label.getString("FILTRO.WORLD"), "wd.egf", "");
    }

    public static ExtensionFileFilter getFilterResults() {
        return new ExtensionFileFilter(label.getString("FILTRO.RESULTADO"), "rr.egf", "");
    }

    public void doMoveFile(File file) {
        // Destination directory
        File dirNew = new File(SettingsManager.getInstance().getConfig("doneDir"));
        File destination = new File(dirNew, file.getName());

        // Move file to new directory
        if (deleteFile(destination) && file.renameTo(destination)) {
            log.debug("Movendo arquivo: " + destination.getPath());
        } else {
            // File was not successfully moved
            log.error("Erro movendo arquivo: " + destination.getPath());
        }
    }

    public File getLoadDir() {
        // Destination directory
        File loadDir = new File(SettingsManager.getInstance().getConfig("loadDir"));
        if (!loadDir.exists()) {
            throw new UnsupportedOperationException("Folder not found: " + loadDir.getAbsolutePath());
        }
        return loadDir;
    }

    public File[] getLoadDirFiles() {
        File[] files = getLoadDir().listFiles(getFilterAcoesImport());
        ComparatorFactory.getComparatorFileTimeSorter(files);
        return files;
    }

    public List<File> listCommandFile(File baseDir, String mask) {
        List<File> ret = new ArrayList<File>();
        File[] files = baseDir.listFiles(getFilterAcoesImport());
        for (File file : files) {
            if (file.getName().contains(mask)) {
                ret.add(file);
            }
        }
        return ret;
    }

    public File getLoadDirTargetFile(String filename) {
        return new File(getLoadDir(), filename);
    }

    private boolean deleteFile(File file) {
        String fileName = file.getName();
        // Make sure the file or directory exists and isn't write protected
        if (!file.exists()) {
            //throw new IllegalArgumentException("Delete: no such file or directory: " + fileName);
            return true;
        }

        if (!file.canWrite()) {
            throw new IllegalArgumentException("Delete: write protected: " + fileName);
        }

        // If it is a directory, make sure it is empty
        if (file.isDirectory()) {
            String[] files = file.list();
            if (files.length > 0) {
                throw new IllegalArgumentException("Delete: directory not empty: " + fileName);
            }
        }

        // Attempt to delete it
        boolean success = file.delete();

//        if (!success) {
//            throw new IllegalArgumentException("Delete: deletion failed");
//        }
        return success;
    }
}
