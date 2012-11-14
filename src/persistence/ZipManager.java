package persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ResourceBundle;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Singleton para gerenciar o ZIps que deve ser único.
 * @author gurgel
 */
public class ZipManager implements Serializable {

    private static final Log log = LogFactory.getLog(ZipManager.class);
    private static final BundleManager label = SettingsManager.getInstance().getBundleManager();
    private static ZipManager instance;

    private ZipManager() {
    }

    public synchronized static ZipManager getInstance() {
        if (instance == null) {
            log.debug("Criou instancia do ZipManager.");
            instance = new ZipManager();
        }
        return instance;
    }

    public void doCompressGzip(File inFile, File outFile) throws PersistenceException {
        FileInputStream in = null;
        GZIPOutputStream out = null;
        try {
            log.debug("Opening the input file.");
            try {
                in = new FileInputStream(inFile);
            } catch (FileNotFoundException ex) {
                throw new PersistenceException(label.getString("ARQUIVO.NAO.ENCONTRADO") + inFile, ex);
            }

            log.debug("Creating the GZIP output stream.");
            try {
                out = new GZIPOutputStream(new FileOutputStream(outFile));
            } catch (FileNotFoundException ex) {
                throw new PersistenceException(label.getString("FILE.NOT.CREATED") + outFile.getAbsolutePath(), ex);
            }

            log.debug("Transfering bytes from input file to GZIP Format.");
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            log.debug("Completing the GZIP file");
            in.close();
            //out.finish();
            out.finish();
            out.close();
        } catch (IOException ex) {
            throw new PersistenceException(label.getString("IO.ERROR"), ex);
        }
    }

    public void doCompressZip(FileInputStream fileInputStream) {
        String outFilezip = "outFile.zip";
        ZipOutputStream out = null;
        BufferedInputStream in = null;
        try {
            out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outFilezip)));
            byte[] data = new byte[1000];
            in = new BufferedInputStream(fileInputStream);
            //adiciona arquivos no index do zip
            out.putNextEntry(new ZipEntry(outFilezip));
            //escreve o zip
            int count;
            while ((count = in.read(data, 0, 1000)) != -1) {
                out.write(data, 0, count);
            }
            log.debug("Your file is zipped:" + outFilezip);
        } catch (FileNotFoundException ex) {
            log.error(ex);
        } catch (IOException ex) {
            log.error(ex);
        } finally {
            try {
                in.close();
                out.flush();
                out.close();
            } catch (IOException ex) {
                log.error(ex);
            }
        }
    }
    /*
     * original code
    import java.io.*;
    import java.util.zip.*;
    public class ZipFileExample
    {
    public static void main(String a[])
    {
    try
    {
    ZipOutputStream out = new ZipOutputStream(new
    BufferedOutputStream(new FileOutputStream("outFile.zip")));
    byte[] data = new byte[1000];
    BufferedInputStream in = new BufferedInputStream
    (new FileInputStream("profile.txt"));
    int count;
    out.putNextEntry(new ZipEntry("outFile.zip"));
    while((count = in.read(data,0,1000)) != -1)
    {
    out.write(data, 0, count);
    }
    in.close();
    out.flush();
    out.close();
    System.out.println("Your file is zipped");
    }
    catch(Exception e)
    {
    e.printStackTrace();
    }
    }
    }
     */

    /**
     * Uncompress the incoming file.
     * @param inFileName Name of the file to be uncompressed
     */
    public File doUncompressGzip(File inFile) throws PersistenceException {
        String inFileName = inFile.getName();
        File ret = null;
        log.debug("Opening the compressed file: " + inFile.getAbsolutePath());
        try {
            GZIPInputStream in = null;
            try {
                in = new GZIPInputStream(new FileInputStream(inFile));
            } catch (FileNotFoundException ex) {
                throw new PersistenceException(label.getString("ARQUIVO.NAO.ENCONTRADO") + inFile.getAbsolutePath());
            }
            log.debug("Open the output file.");
            String outFileName = getFileName(inFileName);
            FileOutputStream out = null;
            try {
                //ret should be a temp file
                ret = File.createTempFile(outFileName, null);
                ret.deleteOnExit();
                //ret = new File(outFileName);
                out = new FileOutputStream(ret);
            } catch (FileNotFoundException ex) {
                throw new PersistenceException(label.getString("FILE.NOT.WRITE") + outFileName);
            }
            log.debug("Transfering bytes from compressed file to the output file.");
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            log.debug("Closing the file and stream");
            in.close();
            out.flush();
            out.close();
        } catch (IOException ex) {
            throw new PersistenceException(ex);
        }
        return ret;
    }

    /**
     * Used to extract the filename without its extension.
     * @param f Incoming file to get the filename
     * @return <code>String</code> representing the filename without its
     *         extension.
     */
    private static String getFileName(String f) {
        String fname = "";
        int i = f.lastIndexOf('.');

        if (i > 0 && i < f.length() - 1) {
            fname = f.substring(0, i);
        }
        return fname;
    }

    /**
     * Used to extract and return the extension of a given file.
     * @param f Incoming file to get the extension of
     * @return <code>String</code> representing the extension of the incoming
     *         file.
     */
    private static String getExtension(String f) {
        String ext = "";
        int i = f.lastIndexOf('.');

        if (i > 0 && i < f.length() - 1) {
            ext = f.substring(i + 1);
        }
        return ext;
    }
}
