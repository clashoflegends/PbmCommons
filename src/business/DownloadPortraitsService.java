/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package business;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Properties;

/**
 *
 * @author Serguei
 */
public interface DownloadPortraitsService {
    
    
    public void checkNetworkConnection() throws ConnectException;
    
    public Properties getServerPropertiesFile() throws IOException;
    
    public File downloadPortraisFile(String portraitsFileName, String portraitsFolder) throws FileNotFoundException;
    
    public int getSize(String portraitsFileName) throws FileNotFoundException;
}
