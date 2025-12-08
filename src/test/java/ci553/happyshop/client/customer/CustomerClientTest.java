package ci553.happyshop.client.customer;


import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.storageAccess.DatabaseRWFactory;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CustomerClientTest {

    private static CustomerView view;
    private static CustomerModel model;
    private static CustomerController controller;
    private static Product testProduct;

    @BeforeAll
    static void setUp() {
        Platform.startup(() -> {
            view = new CustomerView();
            controller = new CustomerController();
            model = new CustomerModel();
            DatabaseRW databaseRW = DatabaseRWFactory.createDatabaseRW();

            view.cusController = controller;
            controller.cusModel = model;
            model.cusView = view;
            model.databaseRW = databaseRW;
            //cusView.start(window);
        });
    }

    @AfterAll
    static void tearDown() {
        Platform.exit();
    }

    @Test
        // Check if the View class initiates the GUI smoothly.
    void startView() {
        Platform.runLater(() -> {
            Stage window = new Stage();
            view.start(window);
            assertNotNull(window.getScene());
        });
    }

    @Test
    // Check if the search query returns a list of relevant products.
    void startDoSearch() {
        Platform.runLater(() -> {
            try {
                view.tfKeyword.setText("USB");
                controller.doAction("Search");
                assertEquals(6, view.getObeProductList().size());

                view.tfKeyword.setText("");
                controller.doAction("Search");
                assertEquals(0, view.getObeProductList().size());
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // Check if the product can be assigned from the trolley.
    void startDoAdd() {
        Platform.runLater(() -> {
            try {
                testProduct = new Product("0001", "Lorem Ipsum", "0001", 1, 99);
                controller.doAction("Add To Trolley", new Object[]{testProduct, 1});
                assertEquals(1, view.getObeTrolleyList().size());
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    @Test
    // Check if the product can be removed from the trolley.
    void startDoRemove() {
        Platform.runLater(() -> {
            try {
                controller.doAction("Remove From Trolley", new Object[]{testProduct});
                assertEquals(0, view.getObeTrolleyList().size());
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    @Test
        // Check that both trolley and product list are cleared.
    void startDoClear() {
        Platform.runLater(() -> {
            try {
                controller.doAction("Cancel");
                assertTrue(view.getObeTrolleyList().isEmpty());
                controller.doAction("Clear");
                assertTrue(view.getObeProductList().isEmpty());
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    /**

    @Test
    // Check if the View class initiates the GUI smoothly.
    void startView() {
        Platform.runLater(() -> {
            Stage window = new Stage();
            view.start(window);
            assertNotNull(window.getScene());
        });
    }

    @Test
    // Check if the customer page is initialised.
    void startCustomerSearchPage() {
        Platform.runLater(() -> {
            VBox page = view.testCreateSearchPage();
            assertNotNull(page);
            assertNotNull(page.getChildren());
            assertEquals(VBox.class, page.getClass());
        });
    }

    @Test
        // Check if the ListView is initiated, and backed by the ObserverList.
    void startProductList() {
        Platform.runLater(() -> {
            ObservableList<Product> obsList = view.testCreateObservableProduct();
            assertNotNull(obsList);

            obsList.add(new Product("0001", "testing product", "0001", 1, 10));
            assertEquals(1, obsList.size());

            ListView<Product> lsProduct = view.testCreateListViewProduct(obsList);
            assertNotNull(lsProduct); assertEquals(ListView.class, lsProduct.getClass());
            assertNotNull(lsProduct.getCellFactory());
            assertNotNull(lsProduct.getItems().getFirst());
        });
    }

    @Test
        // Check if the ListView is initiated, and backed by the ObserverList.
    void startTrolleyList() {
        Platform.runLater(() -> {
            ObservableList<Product> obsList = view.testCreateObservableProduct();
            assertNotNull(obsList);

            obsList.add(new Product("0001", "testing product", "0001", 1, 10));
            assertEquals(1, obsList.size());

            ListView<Product> lsTrolley = view.testCreateListViewTrolley(obsList);
            assertNotNull(lsTrolley); assertEquals(ListView.class, lsTrolley.getClass());
            assertNotNull(lsTrolley.getCellFactory());
            assertNotNull(lsTrolley.getItems().getFirst());
        });
    }

    **/
}