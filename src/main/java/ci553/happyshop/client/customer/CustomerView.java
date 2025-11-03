package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.UIStyle;
import ci553.happyshop.utility.WinPosManager;
import ci553.happyshop.utility.WindowBounds;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import javax.security.auth.login.AccountNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The CustomerView is separated into two sections by a line :
 *
 * 1. Search Page – Always visible, allowing customers to browse and search for products.
 * 2. the second page – display either the Trolley Page or the Receipt Page
 *    depending on the current context. Only one of these is shown at a time.
 */

public class CustomerView  {
    public CustomerController cusController;

    private final int WIDTH = UIStyle.customerWinWidth;
    private final int HEIGHT = UIStyle.customerWinHeight;
    private final int COLUMN_WIDTH = WIDTH / 2 - 10;

    private Label laSearchSummary; // displays the search results
    private Label laOrderSummary; // displays the sum of product's cost
    private HBox hbRoot; // Top-level layout manager
    private VBox vbTrolleyPage;  //vbTrolleyPage and vbReceiptPage will swap with each other when need
    private VBox vbReceiptPage;

    private ObservableList<Product> obeProductList; // observable product list (forked from warehouse)
    private ObservableList<Product> obeTrolleyList; // observable trolley list

    TextField tfKeyword; // user input on search page, accessible by the CustomerModel, able to search regardless name or id
    ListView<Product> obrLvProducts; // A ListView observes the product list (forked from warehouse)
    ListView<Product> obrLvTrolley; // A listView observes products added into the trolley

    //four controllers needs updating when program going on
    private TextArea taReceipt;//in receipt page

    // Holds a reference to this CustomerView window for future access and management
    // (e.g., positioning the removeProductNotifier when needed).
    private Stage viewWindow;

    public void start(Stage window) {
        VBox vbSearchPage = createSearchPage();
        vbTrolleyPage = createTrolleyPage();
        vbReceiptPage = createReceiptPage();

        // Create a divider line
        Line line = new Line(0, 0, 0, HEIGHT);
        line.setStrokeWidth(4);
        line.setStroke(Color.PINK);
        VBox lineContainer = new VBox(line);
        lineContainer.setPrefWidth(4); // Give it some space
        lineContainer.setAlignment(Pos.CENTER);

        hbRoot = new HBox(10, vbSearchPage, lineContainer, vbTrolleyPage); //initialize to show trolleyPage
        hbRoot.setAlignment(Pos.CENTER);
        hbRoot.setStyle(UIStyle.rootStyle);

        Scene scene = new Scene(hbRoot, WIDTH, HEIGHT);
        window.setScene(scene);
        window.setTitle("🛒 HappyShop Customer Client");
        WinPosManager.registerWindow(window,WIDTH,HEIGHT); //calculate position x and y for this window
        window.show();
        viewWindow=window;// Sets viewWindow to this window for future reference and management.
    }

    private VBox createSearchPage() {
        Label laPageTitle = new Label("Customer Client");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        // Icons for each button
        ImageView ivSearch = new ImageView("magnifier.png");
        ivSearch.setFitHeight(20f);
        ivSearch.setFitWidth(20f);
        ivSearch.setPreserveRatio(true);

        ImageView ivClear = new ImageView("clear.png");
        ivClear.setFitHeight(15f);
        ivClear.setFitWidth(15f);
        ivClear.setPreserveRatio(true);

        // Cancellation button that clears the text field
        Button btnClear = new Button();
        btnClear.setGraphic(ivClear);
        btnClear.setPrefSize(10f, 15f);
        btnClear.setStyle(UIStyle.cancelButtonStyle);
        btnClear.setOnAction(this::buttonClicked);
        btnClear.getProperties().put("Clear", null);

        // Search button; finds product regardless of it's ID/Name
        Button btnSearch = new Button();
        btnSearch.setGraphic(ivSearch);
        btnSearch.setPrefSize(20f, 35f);
        btnSearch.setStyle(UIStyle.searchButtonStyle);
        btnSearch.setOnAction(this::buttonClicked);
        btnSearch.getProperties().put("Search", null);

        // Horizontal container for both btnClear and btnSearch
        HBox hbSearch = new HBox(5, btnClear, btnSearch);
        hbSearch.setAlignment(Pos.CENTER);

        // Displays the summary of queried products
        laSearchSummary = new Label("Search Summary");
        laSearchSummary.setStyle(UIStyle.labelStyle);

        // Unified search input;
        tfKeyword = new TextField();
        tfKeyword.setPromptText("Search product by Name/ID");
        tfKeyword.setStyle(UIStyle.textFiledStyle);
        tfKeyword.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("TextField changes from " + oldValue + " to " + newValue);
            // If there's any input, make the clear button appear
            if (!newValue.isBlank()) {
                btnClear.opacityProperty().set(1);
                btnClear.setDisable(false);
            } else {
                btnClear.opacityProperty().set(0);
                btnClear.setDisable(true);
            }
        });

        // Observable object for containing products.
        obeProductList = FXCollections.observableArrayList();

        // ListView for displaying products with appealing UI
        obrLvProducts = new ListView<>(obeProductList); // updates the list from the observer
        obrLvProducts.setPrefHeight(HEIGHT - 100);
        obrLvProducts.setFixedCellSize(50);
        obrLvProducts.setStyle(UIStyle.listViewStyle);

        // Custom cell factory for displaying customisable objects with image, buttons, text and more.
        obrLvProducts.setCellFactory(param -> new ListCell<Product>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setGraphic(null);
                    //System.out.println("setCellFactory - empty item");
                } else {
                    HBox hbProItem = createProductItem(product);
                    hbProItem.setAlignment(Pos.CENTER);
                    setGraphic(hbProItem);
                }
            }
        });

        // Anchor container for a flexible layer of anchoring nodes
        AnchorPane apSearch = new AnchorPane();
        apSearch.getChildren().addAll(tfKeyword, hbSearch);

        // Had to manually set the anchors to allow it from stretching on four sides
        AnchorPane.setTopAnchor(tfKeyword, 0.0);
        AnchorPane.setBottomAnchor(tfKeyword, 0.0);
        AnchorPane.setLeftAnchor(tfKeyword, 0.0);
        AnchorPane.setRightAnchor(tfKeyword, 0.0);

        // Same configuration applies on every node, ensuring the hbSearch is pinned on the right side
        AnchorPane.setRightAnchor(hbSearch, 0.0);
        AnchorPane.setTopAnchor(hbSearch, 0.0);
        AnchorPane.setBottomAnchor(hbSearch, 1.0);


        VBox vbSearchPage = new VBox(15, laPageTitle, apSearch, laSearchSummary, obrLvProducts);
        vbSearchPage.setPrefWidth(COLUMN_WIDTH);
        vbSearchPage.setAlignment(Pos.TOP_CENTER);
        vbSearchPage.setStyle("-fx-padding: 5px");

        return vbSearchPage;
    }

    private VBox createTrolleyPage() {
        Label laPageTitle = new Label("🛒🛒  Trolley 🛒🛒");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        // Label for displaying the total cost of each order product.
        laOrderSummary = new Label("Order Summary");
        laOrderSummary.setStyle(UIStyle.labelStyle);

        // Observable object for containing products in the trolley.
        obeTrolleyList = FXCollections.observableArrayList();

        // ListView for displaying products in the trolley with appealing UI
        obrLvTrolley = new ListView<>(obeTrolleyList); // updates the list from the observer
        obrLvTrolley.setPrefHeight(HEIGHT - 100);
        obrLvTrolley.setFixedCellSize(50);
        obrLvTrolley.setStyle(UIStyle.listViewStyle);

        // Custom cell factory for displaying customisable objects with image, buttons, text and more.
        obrLvTrolley.setCellFactory(param -> new ListCell<Product>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setGraphic(null);
                  //  System.out.println("setCellFactory - empty item");
                } else {
                    HBox hbProItem = CreateTrolleyItem(product);
                    hbProItem.setAlignment(Pos.CENTER);
                    setGraphic(hbProItem);
                }
            }
        });

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(this::buttonClicked);
        btnCancel.setStyle(UIStyle.buttonStyle);
        btnCancel.getProperties().put("Cancel", null);

        Button btnCheckout = new Button("Check Out");
        btnCheckout.setOnAction(this::buttonClicked);
        btnCheckout.setStyle(UIStyle.buttonStyle);
        btnCheckout.getProperties().put("Check Out", null);

        HBox hbBtns = new HBox(10, btnCancel, btnCheckout);
        hbBtns.setStyle("-fx-padding: 15px;");
        hbBtns.setAlignment(Pos.CENTER);

        vbTrolleyPage = new VBox(15, laPageTitle, obrLvTrolley, laOrderSummary, hbBtns);
        vbTrolleyPage.setPrefWidth(COLUMN_WIDTH);
        vbTrolleyPage.setAlignment(Pos.TOP_CENTER);
        vbTrolleyPage.setStyle("-fx-padding: 5px;");

        return vbTrolleyPage;
    }

    private VBox createReceiptPage() {
        Label laPageTitle = new Label("Receipt");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        taReceipt = new TextArea();
        taReceipt.setEditable(false);
        taReceipt.setPrefSize(WIDTH/2, HEIGHT-50);

        Button btnCloseReceipt = new Button("OK & Close"); //btn for closing receipt and showing trolley page
        btnCloseReceipt.setStyle(UIStyle.buttonStyle);
        btnCloseReceipt.setOnAction(this::buttonClicked);

        vbReceiptPage = new VBox(15, laPageTitle, taReceipt, btnCloseReceipt);
        vbReceiptPage.setPrefWidth(COLUMN_WIDTH);
        vbReceiptPage.setAlignment(Pos.TOP_CENTER);
        vbReceiptPage.setStyle(UIStyle.rootStyleYellow);
        return vbReceiptPage;
    }

    private HBox createProductItem(Product product) {
        String imageName = product.getProductImageName(); // image name e.g 0001.jpg
        String relativeImageUrl = StorageLocation.imageFolder + imageName;
        Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath(); // absolute path to the image
        String imageFullUri = imageFullPath.toUri().toString(); // build the full image uri
        ImageView ivPro;
        try {
            // Attempt to load the product's uri image
            ivPro = new ImageView(new Image(imageFullUri, 50, 45, true, true));
        } catch (final Exception e) {
            // Otherwise, use a default image directly from the resources folder without crashing
            ivPro = new ImageView(new Image("imageHolder.jpg", 50, 45, true, true));
        }
        Label lStock = new Label("Pending Stock"); // a label about the product's stock availability
        // if statement to evaluate the product's status based of it's stock
        // In real implementation, stock availability is not based on hardcoded values; (stock = units daily x lead day x etc.)
        int stockQuantity = product.getStockQuantity();
        if (stockQuantity <= 30 && stockQuantity >= 1) {
            // Low stock
            lStock.setStyle(UIStyle.labelLowStockStyle);
            lStock.setText("⚠ Low Stock " + stockQuantity);
        } else if (stockQuantity <= 0) {
            // Out of Stock
            lStock.setStyle(UIStyle.labelOutOfStockStyle);
            lStock.setText("✘ Out of Stock");
        } else {
            // In Stock
            lStock.setStyle(UIStyle.labelInStockStyle);
            lStock.setText("✔ In Stock");
        }
        Label lDetail = new Label(product.getProductDescription());  // A label about product's detail
        Label lId = new Label(product.getProductId()); // A label about product's id
        Label lPrice = new Label("£" + product.getUnitPrice()); // A label about product's price
        lPrice.setStyle(UIStyle.labelPriceStyle);
        lId.setStyle(UIStyle.labelIdStyle);
        // drop-down container with a set between 1 and 10;
        ComboBox<Number> comboB = new ComboBox<>();
        comboB.setPrefSize(65, 33);
        for (int i = 1; i <= 10; i++) {
            if (i == 1) {
                comboB.setValue(i);
            }
            comboB.getItems().add(i);
        }
        // add to trolley button to assign the product
        Button btnAdd = new Button("🛒");
        btnAdd.setPrefSize(35, 33);
        btnAdd.setOnAction(this::buttonClicked);
        btnAdd.getProperties().put("Add To Trolley", Arrays.asList(product, comboB.getValue()));
        // Listener for updating the product's ordered quantity
        comboB.valueProperty().addListener((obs, oldValue, newValue) -> {
            btnAdd.getProperties().put("Add To Trolley", Arrays.asList(product, comboB.getValue()));
        });
        HBox HBMiddle = new HBox(5, lPrice, lId);
        HBMiddle.setAlignment(Pos.CENTER_LEFT);
        VBox vbTop = new VBox(0, lDetail, HBMiddle, lStock);
        vbTop.setAlignment(Pos.CENTER_LEFT);
        HBox hbRight = new HBox(5, comboB, btnAdd);
        hbRight.setAlignment(Pos.CENTER);
        return new HBox(10, ivPro, vbTop, hbRight);
    }

    private HBox CreateTrolleyItem(Product product) {
        String imageName = product.getProductImageName(); // image name e.g 0001.jpg
        String relativeImageUrl = StorageLocation.imageFolder + imageName;
        Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath(); // absolute path to the image
        String imageFullUri = imageFullPath.toUri().toString(); // build the full image uri
        ImageView ivPro;
        try {
            // Attempt to load the product's uri image
            ivPro = new ImageView(new Image(imageFullUri, 50, 45, true, true));
        } catch (final Exception e) {
            // Otherwise, use a default image directly from the resources folder without crashing
            ivPro = new ImageView(new Image("imageHolder.jpg", 50, 45, true, true));
        }
        Label lStock = new Label("Pending Stock"); // a label about the product's stock availability
        // if statement to evaluate the product's status based of it's stock
        // In real implementation, stock availability is not based on hardcoded values; (stock = units daily x lead day x etc.)
        int stockQuantity = product.getStockQuantity();
        if (stockQuantity <= 30 && stockQuantity >= 1) {
            // Low stock
            lStock.setStyle(UIStyle.labelLowStockStyle);
            lStock.setText("⚠ Low Stock " + stockQuantity);
        } else if (stockQuantity <= 0) {
            // Out of Stock
            lStock.setStyle(UIStyle.labelOutOfStockStyle);
            lStock.setText("✘ Out of Stock");
        } else {
            // In Stock
            lStock.setStyle(UIStyle.labelInStockStyle);
            lStock.setText("✔ In Stock");
        }
        Label lDetail = new Label(product.getProductDescription());  // A label about product's detail
        Label lId = new Label(product.getProductId()); // A label about product's id
        Label lPrice = new Label("£" + String.format("%.2f", product.getUnitPrice() * product.getOrderedQuantity())); // A label about product's price
        lPrice.setStyle(UIStyle.labelPriceStyle);
        lId.setStyle(UIStyle.labelIdStyle);
        int currentQuantity = product.getOrderedQuantity();
        // numeric up-down container with a set between 1 and 10; able to either increment or decrement the value.
        SpinnerValueFactory<Integer> valueFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, currentQuantity, 1);
        Spinner<Integer> numericSpinner = new Spinner<>();
        numericSpinner.setPrefSize(70, 33);
        numericSpinner.setValueFactory(valueFactory);
        numericSpinner.setEditable(false);
        // Listener of updating the product's quantity via increment/decrement
        numericSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            try {
                cusController.doAction("Add To Trolley", Arrays.asList(product, newValue - oldValue).toArray());
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        // add to trolley button to assign the product
        Button btnRemove = new Button("🗑");
        btnRemove.setPrefSize(35, 33);
        btnRemove.setOnAction(this::buttonClicked);
        btnRemove.getProperties().put("Remove From Trolley", List.of(product));
        HBox HBMiddle = new HBox(5, lPrice, lId);
        HBMiddle.setAlignment(Pos.CENTER_LEFT);
        VBox vbTop = new VBox(0, lDetail, HBMiddle, lStock);
        vbTop.setAlignment(Pos.CENTER_LEFT);
        HBox hbRight = new HBox(5, numericSpinner, btnRemove);
        hbRight.setAlignment(Pos.CENTER);
        return new HBox(10, ivPro, vbTop, hbRight);
    }

    private void buttonClicked(ActionEvent event) {
        try {
            Button btn = (Button) event.getSource();
            ObservableMap<Object, Object> properties = btn.getProperties();
            /**
             * Using custom properties over text for invoking the Controller in the following reasons:
             * 1. Texts are unreliable as it's easily writeable
             * 2. Unable to display graphic if the text is specified.
             * 3. properties can hold multiple actions rather than one.
             */
            /* Iterate through the map of properties */
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                Object key = entry.getKey(); // Key is the action
                Object value = entry.getValue(); // Value is the arguments

                /* evaluate that the key & value are string type */
                if (key instanceof String) {
                    if (key.equals("Clear")) {
                        tfKeyword.clear();
                    }
                    if (value != null) {
                        // unpack the arguments if the value is a list
                        if (value instanceof List) {
                            cusController.doAction((String) key, ((List<?>) value).toArray());
                        }
                    } else {
                        cusController.doAction((String) key);
                    }
                }
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(ArrayList<Product> productList, ArrayList<Product> trolleyList, String SearchSummary, String OrderSummary) {
        int proCounter = productList.size();
        System.out.println(proCounter);
        obeProductList.clear();
        obeProductList.addAll(productList);
        obeTrolleyList.clear();
        obeTrolleyList.addAll(trolleyList);
        laSearchSummary.setText(SearchSummary);
        laOrderSummary.setText(OrderSummary);
    }

    // Replaces the last child of hbRoot with the specified page.
    // the last child is either vbTrolleyPage or vbReceiptPage.
    private void showTrolleyOrReceiptPage(Node pageToShow) {
        int lastIndex = hbRoot.getChildren().size() - 1;
        if (lastIndex >= 0) {
            hbRoot.getChildren().set(lastIndex, pageToShow);
        }
    }

    WindowBounds getWindowBounds() {
        return new WindowBounds(viewWindow.getX(), viewWindow.getY(),
                  viewWindow.getWidth(), viewWindow.getHeight());
    }
}
