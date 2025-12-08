package ci553.happyshop.client.customer;
import ci553.happyshop.catalogue.Product;

import java.io.IOException;
import java.sql.SQLException;

/**
 * CustomerController acts as a dispatcher for the CustomerView; converting actions into executions towards the CustomerModel.
 */
public class CustomerController {
    public CustomerModel cusModel;
    // Literally forgotten that java's index starts 0 not 1...
    /**
     * Overloading method of doAction, dispatches customer action.
     * @param action The name of the action to perform.
     * @param object The arguments required for the action.
     * @throws SQLException if a database operation fails.
     * @throws IOException  if an I/O operation fails.
     */
    public void doAction(String action, Object[] object) throws SQLException, IOException {
        switch (action) {
            case "Add To Trolley":
                cusModel.doAdd((Product) object[0], (int) object[1]);
                break;
            case "Remove From Trolley":
                cusModel.doRemove((Product) object[0]);
                break;
        }
    }
    /**
     * dispatches customer action.
     * @param action The name of the action to perform.
     * @throws SQLException if a database operation fails.
     * @throws IOException  if an I/O operation fails.
     */
    public void doAction(String action) throws SQLException, IOException {
        switch (action) {
            case "Search":
                cusModel.doSearch();
                break;
            case "Cancel":
                cusModel.doCancel();
                break;
            case "Clear":
                cusModel.doClear();
                break;
            case "Check Out":
                cusModel.doCheckOut();
                break;
            case "OK & Close":
                cusModel.doCloseReceipt();
                break;
        }
    }
}
