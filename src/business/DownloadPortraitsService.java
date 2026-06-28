/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package business;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;

/**
 *
 * @author Serguei
 */
public interface DownloadPortraitsService {
    
    
    public void checkNetworkConnection() throws ConnectException;

    /** Download the portraits zip from {@code url} into {@code destFolder} (as portraits.zip). */
    public File downloadPortraitsZip(String url, String destFolder) throws FileNotFoundException;

    /** Size in bytes of the zip at {@code url} (HEAD, follows redirects); 0 if unknown. */
    public int getSize(String url) throws FileNotFoundException;
}
