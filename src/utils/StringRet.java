/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * servers to concatenate strings
 * @author jmoura
 */
public class StringRet implements Serializable {

    private final String separator = "\n";
    private final String tab = "\t";
    private final List<String> textRet = new ArrayList();

    /**
     * @return the list of messages
     */
    public List<String> getList() {
        return textRet;
    }

    public void add(String text) {
        this.textRet.add(text);
    }

    public void addTab(String text) {
        this.textRet.add(tab + text);
    }

    public void addTab(List<String> texts) {
        for (String elem : texts) {
            this.textRet.add(tab + elem);
        }
    }

    public void add(List<String> texts) {
        this.textRet.addAll(texts);
    }

    public String getText() {
        String ret = "";
        for (String elem : textRet) {
            ret += elem + separator;
        }
        return ret;
    }
}
