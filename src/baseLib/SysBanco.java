/*
 * SysBanco.java
 *
 * Created on 26 de Junho de 2006, 15:56
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package baseLib;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.PersistenceException;
import persistenceCommons.SettingsManager;

/**
 *
 * @author Gurgel
 */
public class SysBanco {

    private static Connection conn = null;
    private static final Log log = LogFactory.getLog(SysBanco.class);

    /**
     * Creates a new instance of SysBanco
     */
    public SysBanco() {
        if (getConn() == null) {
            criaConn();
        }
    }

    public synchronized static Connection getConn() {
        if (conn == null) {
            criaConn();
        }
        return conn;
    }

    public static void setConn(Connection c) {
        conn = c;
    }

    private static void criaConn() {
        try {
//            Class.forName("com.mysql.jdbc.Driver");
            final String conexao = String.format("jdbc:mysql://%s/%s", SettingsManager.getInstance().getConfig("bdServer", "localhost"), SettingsManager.getInstance().getConfig("bdDatabase"));
            log.info("Conectando ao banco de dados:" + conexao);
            setConn(DriverManager.getConnection(conexao, SettingsManager.getInstance().getConfig("bdLogin"), SettingsManager.getInstance().getConfig("bdSenha")));
        } catch (SQLException e) {
            log.error("Banco fora do ar.", e);
            throw new UnsupportedOperationException("Banco fora do ar.");
//        } catch (ClassNotFoundException e) {
//            log.error("texto: ClassNotFoundException....", e);
//            throw new UnsupportedOperationException("Para tudo que nao ta indo bem.");
        }
    }

    public static void cleanUp(PreparedStatement pstm, ResultSet rs) throws SQLException {
        if (rs != null) {
            rs.close();
        }
        if (pstm != null) {
            pstm.close();
        }
    }

    public static void cleanUp(ResultSet rs) throws SQLException {
        if (rs != null) {
            rs.close();
        }
    }

    public static void cleanUp(PreparedStatement pstm) throws SQLException {
        if (pstm != null) {
            pstm.close();
        }
    }

    public static String buscaDs(String sql, String param) {
        return SysBanco.doBuscaDs(sql.replace("?", "'" + param + "'"));
    }

    public static String buscaDs(String sql, int param) {
        return SysBanco.doBuscaDs(sql.replace("?", param + ""));
    }

    public static String buscaDs(String sql) {
        return doBuscaDs(sql);
    }

    public static String buscaDs(String sql, String param, String fieldName) {
        return SysBanco.doBuscaDs(sql.replace("?", "'" + param + "'"), fieldName);
    }

    private static String doBuscaDs(String sql) {
        return doBuscaDs(sql, "1");
    }

    private static String doBuscaDs(String sql, String field) {
        //imp(sql);
        String ret = "";
        try {
            PreparedStatement pstm = getConn().prepareStatement(sql);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                if (field.equals("1")) {
                    ret = rs.getString(1);
                } else {
                    ret = rs.getString(field);
                }
            }
            cleanUp(pstm, rs);
        } catch (SQLException e) {
            log.error("texto: SQLException...", e);
        }
        return ret;
    }

    public static int rodaUpdate(String sql, String par1, String par2, String par3, String par4) throws PersistenceException {
        int ret = -1;
        try {
            PreparedStatement pstm;
            pstm = getConn().prepareStatement(sql);
            pstm.setString(1, par1);
            pstm.setString(2, par2);
            pstm.setString(3, par3);
            pstm.setString(4, par4);
            ret = pstm.executeUpdate();
            SysBanco.cleanUp(pstm);
        } catch (SQLException ex) {
            throw new PersistenceException(ex);
        }
        return ret;
    }

    public static int rodaUpdate(String sql, String par1, String par2, String par3) throws PersistenceException {
        int ret = -1;
        try {
            PreparedStatement pstm;
            pstm = getConn().prepareStatement(sql);
            pstm.setString(1, par1);
            pstm.setString(2, par2);
            pstm.setString(3, par3);
            ret = pstm.executeUpdate();
            SysBanco.cleanUp(pstm);
        } catch (SQLException ex) {
            throw new PersistenceException(ex);
        }
        return ret;
    }

    public static int rodaUpdate(String sql, String par1, String par2) throws PersistenceException {
        int ret = -1;
        try {
            PreparedStatement pstm;
            pstm = getConn().prepareStatement(sql);
            pstm.setString(1, par1);
            pstm.setString(2, par2);
            ret = pstm.executeUpdate();
            SysBanco.cleanUp(pstm);
        } catch (SQLException ex) {
            throw new PersistenceException(ex);
        }
        return ret;
    }

    public static int rodaUpdate(String sql, int par1, int par2) {
        int ret = -1;
        try {
            PreparedStatement pstm;
            pstm = getConn().prepareStatement(sql);
            pstm.setInt(1, par1);
            pstm.setInt(2, par2);
            ret = pstm.executeUpdate();
            SysBanco.cleanUp(pstm);
        } catch (SQLException e) {
            log.error("texto: SQLException..." + sql, e);
        }
        return ret;
    }

    public static int rodaUpdate(String sql, int par1) {
        int ret = -1;
        try {
            PreparedStatement pstm;
            pstm = getConn().prepareStatement(sql);
            pstm.setInt(1, par1);
            ret = pstm.executeUpdate();
            SysBanco.cleanUp(pstm);
        } catch (SQLException e) {
            log.error("texto: SQLException..." + sql, e);
        }
        return ret;
    }

    public static int rodaUpdate(String sql) {
        int ret = -1;
        try {
            PreparedStatement pstm;
            pstm = getConn().prepareStatement(sql);
            ret = pstm.executeUpdate();
            SysBanco.cleanUp(pstm);
        } catch (SQLException e) {
            log.error("texto: SQLException..." + sql, e);
        }
        return ret;
    }

    public static PreparedStatement prepareStatement(String sql) throws SQLException {
        return SysBanco.getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }

    public static int getLastInsertedId(PreparedStatement pstm) throws SQLException {
        int ret = 0;
        ResultSet rs = pstm.getGeneratedKeys();
        if (rs.next()) {
            ret = rs.getInt(1);
        }
        SysBanco.cleanUp(rs);
        return ret;
    }
}
