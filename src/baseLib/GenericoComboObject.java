package baseLib;

import java.io.Serializable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GenericoComboObject implements Serializable, IBaseModel, Comparable {

    private String display;
    private String id;
    private IBaseModel objeto;
    private static final Log log = LogFactory.getLog(GenericoComboObject.class);

    public GenericoComboObject(IBaseModel objetoBaseCombo) {
        this.objeto = objetoBaseCombo;
        this.display = objetoBaseCombo.getComboDisplay();
        this.id = objetoBaseCombo.getComboId();
    }

    public GenericoComboObject(String display, String id) {
        this.display = display;
        this.id = id;
    }

    private GenericoComboObject(Object display, Object id) {
        try {
            this.display = (String) display;
            this.id = (String) id;
        } catch (Exception exception) {
            log.fatal("Ops!: " + exception, exception);
        }
    }

    public IBaseModel getObject() {
        return this.objeto;
    }

    @Override
    public String toString() {
        return getComboDisplay();
    }

    @Override
    public String getComboDisplay() {
        return this.display;
    }

    @Override
    public String getComboId() {
        return this.id;
    }

    @Override
    public int getId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getCodigo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getNome() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int compareTo(Object o) {
        return this.getComboDisplay().compareTo(((GenericoComboObject) o).getComboDisplay());
    }
}
