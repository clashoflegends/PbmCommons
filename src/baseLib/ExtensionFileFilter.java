/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package baseLib;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author gurgel
 */
public class ExtensionFileFilter extends FileFilter implements FilenameFilter, Serializable {

    String description;
    String extensions[];
    String startWith = "";

    public ExtensionFileFilter(String description, String extension, String startWith) {
        
        this(description, new String[]{extension});
        this.startWith = startWith.toLowerCase();
    }

    public ExtensionFileFilter(String description, String extension) {
        this(description, new String[]{extension});
    }

    public ExtensionFileFilter(String description, String extensions[]) {
        if (description == null) {
            this.description = extensions[0];
        } else {
            this.description = description;
        }
        this.extensions = (String[]) extensions.clone();
        toLower(this.extensions);
    }

    private void toLower(String array[]) {
        for (int i = 0,  n = array.length; i < n; i++) {
            array[i] = array[i].toLowerCase();
        }
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        } else {
            return checkExtensions(file.getName().toLowerCase());
        }
    }

    @Override
    public boolean accept(File dir, String fileName) {
        return checkExtensions(fileName);
    }

    private boolean checkExtensions(String fileName) {
        for (int i = 0,  n = extensions.length; i < n; i++) {
            if (fileName.endsWith("." + extensions[i]) && fileName.startsWith(startWith)) {
                return true;
            }
        }
        return false;
    }
}
