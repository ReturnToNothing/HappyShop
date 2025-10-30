package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.utility.UIStyle;
import ci553.happyshop.utility.WinPosManager;
import ci553.happyshop.utility.WindowBounds;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.sql.SQLException;

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

    private HBox hbRoot; // Top-level layout manager
    private VBox vbTrolleyPage;  //vbTrolleyPage and vbReceiptPage will swap with each other when need
    private VBox vbReceiptPage;

    private ObservableList<Product> obeProductList; // observable product list (forked from warehouse)

    TextField tfId; //for user input on the search page. Made accessible so it can be accessed or modified by CustomerModel
    TextField tfName; //for user input on the search page. Made accessible so it can be accessed by CustomerModel
    TextField tfKeyword; // user input on search page, accessible by the CustomerModel, able to search regardless name or id
    ListView<Product> obrLvProducts; // A ListView observes the product list (forked from warehouse)

    //four controllers needs updating when program going on
    private ImageView ivProduct; //image area in searchPage
    private Label lbProductInfo;//product text info in searchPage
    private TextArea taTrolley; //in trolley Page
    private TextArea taReceipt;//in receipt page

    // Holds a reference to this CustomerView window for future access and management
    // (e.g., positioning the removeProductNotifier when needed).
    private Stage viewWindow;

    public void start(Stage window) {
        VBox vbSearchPage = createSearchPage();
        vbTrolleyPage = CreateTrolleyPage();
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
        btnClear.getProperties().put("Action", "Clear");

        // Search button; finds product regardless of it's ID/Name
        Button btnSearch = new Button();
        btnSearch.setGraphic(ivSearch);
        btnSearch.setPrefSize(20f, 34f);
        btnSearch.setStyle(UIStyle.searchButtonStyle);
        btnSearch.setOnAction(this::buttonClicked);
        btnSearch.getProperties().put("Action", "Search");

        // Horizontal container for both btnClear and btnSearch
        HBox hbSearch = new HBox(5, btnClear, btnSearch);
        hbSearch.setAlignment(Pos.CENTER);

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

        VBox vbSearchPage = new VBox(15, laPageTitle, apSearch);
        vbSearchPage.setPrefWidth(COLUMN_WIDTH);
        vbSearchPage.setAlignment(Pos.TOP_CENTER);
        vbSearchPage.setStyle("-fx-padding: 5px");

        return vbSearchPage;
    }

    private VBox CreateTrolleyPage() {
        Label laPageTitle = new Label("🛒🛒  Trolley 🛒🛒");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        taTrolley = new TextArea();
        taTrolley.setEditable(false);
        taTrolley.setPrefSize(WIDTH/2, HEIGHT-50);

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(this::buttonClicked);
        btnCancel.setStyle(UIStyle.buttonStyle);

        Button btnCheckout = new Button("Check Out");
        btnCheckout.setOnAction(this::buttonClicked);
        btnCheckout.setStyle(UIStyle.buttonStyle);

        HBox hbBtns = new HBox(10, btnCancel,btnCheckout);
        hbBtns.setStyle("-fx-padding: 15px;");
        hbBtns.setAlignment(Pos.CENTER);

        vbTrolleyPage = new VBox(15, laPageTitle, taTrolley, hbBtns);
        vbTrolleyPage.setPrefWidth(COLUMN_WIDTH);
        vbTrolleyPage.setAlignment(Pos.TOP_CENTER);
        vbTrolleyPage.setStyle("-fx-padding: 15px;");
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


    private void buttonClicked(ActionEvent event) {
        try{
            Button btn = (Button)event.getSource();
            String action = btn.getText();
            if(action.equals("Add to Trolley")){
                showTrolleyOrReceiptPage(vbTrolleyPage); //ensure trolleyPage shows if the last customer did not close their receiptPage
            }
            if(action.equals("OK & Close")){
                showTrolleyOrReceiptPage(vbTrolleyPage);
            }
            cusController.doAction(action);
        }
        catch(SQLException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void update(String imageName, String searchResult, String trolley, String receipt) {

        ivProduct.setImage(new Image(imageName));
        lbProductInfo.setText(searchResult);
        taTrolley.setText(trolley);
        if (!receipt.equals("")) {
            showTrolleyOrReceiptPage(vbReceiptPage);
            taReceipt.setText(receipt);
        }
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
