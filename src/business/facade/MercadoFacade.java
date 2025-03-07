/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package business.facade;

import java.io.Serializable;
import model.Cenario;
import model.Local;
import model.Mercado;
import model.Produto;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class MercadoFacade implements Serializable {

    private static final Log log = LogFactory.getLog(MercadoFacade.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final LocalFacade localFacade = new LocalFacade();
    private static final CidadeFacade cidadeFacade = new CidadeFacade();

    public int getSell(Produto product, int qtProduct, Mercado mercado) {
        return qtProduct * mercado.getProdutoVlVenda(product);
    }

    public int getBestSellProductionAndStorage(Local hex, Mercado market, Cenario scenario, int turn) {
        int ret = 0;
        for (Produto product : market.getProdutos()) {
            final int qtProduct;
            if (localFacade.isCidade(hex)) {
                qtProduct = localFacade.getProduction(hex, product, scenario, turn) + cidadeFacade.getEstoque(hex.getCidade(), product);
            } else {
                qtProduct = localFacade.getProduction(hex, product, scenario, turn);
            }
            final int sellTotal = getSell(product, qtProduct, market);
            if (sellTotal > ret) {
                ret = sellTotal;
            }
        }
        return ret;
    }

    public int getBestSellProduction(Local hex, Mercado mercado, Cenario cenario, int turno) {
        int ret = 0;
        for (Produto produto : mercado.getProdutos()) {
            final int qtProduct = localFacade.getProduction(hex, produto, cenario, turno);
            final int sellTotal = getSell(produto, qtProduct, mercado);
            if (sellTotal > ret) {
                ret = sellTotal;
            }
        }
        return ret;
    }
}
