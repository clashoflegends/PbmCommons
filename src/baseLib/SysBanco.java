/*
 * SysBanco.java
 *
 * Created on 26 de Junho de 2006, 15:56
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package baseLib;

import java.sql.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
            log.info("Conectando ao banco de dados:" + SysProperties.getProps("bdDatabase"));
            Class.forName("com.mysql.jdbc.Driver");
            String conexao = "jdbc:mysql://localhost:3306/" + SysProperties.getProps("bdDatabase");
            setConn(DriverManager.getConnection(conexao, SysProperties.getProps("bdLogin"),SysProperties.getProps("bdSenha")));
        } catch (SQLException e) {
            log.error("Banco fora do ar.", e);
            throw new UnsupportedOperationException("Banco fora do ar.");
        } catch (ClassNotFoundException e) {
            log.error("texto: ClassNotFoundException....", e);
            throw new UnsupportedOperationException("Para tudo que nao ta indo bem.");
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

    public static String buscaDs(String sql) {
        //imp(sql);
        String ret = "";
        try {
            PreparedStatement pstm = getConn().prepareStatement(sql);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                ret = rs.getString(1);
            }
            cleanUp(pstm, rs);
        } catch (SQLException e) {
            log.error("texto: SQLException...", e);
        }
        return ret;
    }

    public static int rodaUpdate(String sql, int idPartida, int turno) {
        int ret = -1;
        try {
            PreparedStatement pstm;
            pstm = getConn().prepareStatement(sql);
            pstm.setInt(1, idPartida);
            pstm.setInt(2, turno);
            ret = pstm.executeUpdate();
            SysBanco.cleanUp(pstm);
        } catch (SQLException e) {
            log.error("texto: SQLException...", e);
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
