package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.ProductListFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 * You can either directly modify the CustomerModel class to implement the required tasks,
 * or create a subclass of CustomerModel and override specific methods where appropriate.
 */
public class CustomerModel {
    public RemoveProductNotifier proNotifier; //
    public CustomerView cusView;
    public DatabaseRW databaseRW; //Interface type, not specific implementation
                                  //Benefits: Flexibility: Easily change the database implementation.
    private ArrayList<Product> productList = new ArrayList<>(); // search results fetched from the database
    private ArrayList<Product> trolleyList =  new ArrayList<>(); // a list of products in trolley

    // Four UI elements to be passed to CustomerView for display updates.
    private String displayTaReceipt = "";                                // Text area content showing receipt after checkout (Receipt Page)
    private String searchSummary = "Search Summary"; // Label that displays the count of searched products
    private String orderSummary = "Order Summary"; // Label that displays the total cost of ordered products.

    //SELECT productID, description, image, unitPrice,inStock quantity
    void doSearch() throws SQLException {
        String keyword = cusView.tfKeyword.getText().trim();
        // check if keyword isn't empty before querying

        if (!keyword.isEmpty()) {
            productList = databaseRW.searchProduct(keyword);
            if (!productList.isEmpty()) {
                searchSummary = productList.size() + " Products found.";
            } else {
                searchSummary = "Products not found.";
            }
        }
        else {
            productList.clear();
            searchSummary = "Empty input.";
            System.out.println("please type product ID or name to search");
        }
        updateView();
    }

    void doAdd(Product product, int quantity) {
        if (product != null) {
            // Ensure the product is present in the trolley before adjusting its quantity.
            if (!trolleyList.contains(product)) {
                trolleyList.add(product);
            }

            // Calculate the actual number of items still available to be added
            int availableQuantity = product.getStockQuantity() - product.getOrderedQuantity();

            // Check if there is enough stock for the amount of quantity requested.
            if (availableQuantity - quantity > 0) {
                product.setOrderedQuantity(product.getOrderedQuantity() + quantity);
                System.out.println("Added Product " + product.getProductDescription() + " Quantity Ordered: " + product.getOrderedQuantity());
            } else {
                // otherwise, set the ordered quantity by its intial quantity.
                if (product.getStockQuantity() > 0) {
                    product.setOrderedQuantity(product.getStockQuantity());
                } else {
                    product.setOrderedQuantity(quantity);
                }
                System.out.println("Added Product " + product.getProductDescription() + " Max Quantity Reached: " + product.getOrderedQuantity());
            }
        }  else {
            System.out.println("must search and get an available product before add to trolley");
        }
        updateView();
    }

    void doRemove(Product product) {
        if (product != null) {
            product.setOrderedQuantity(0);
            trolleyList.remove(product);
            System.out.println("Removed Product " + product.getProductDescription());
        }  else {
            System.out.println("must select an existing product inside the trolley");
        }
        updateView();
    }

    void doCheckOut() throws IOException, SQLException {
        if(!trolleyList.isEmpty()){
            // Group the products in the trolley by productId to optimize stock checking
            // Check the database for sufficient stock for all products in the trolley.
            // If any products are insufficient, the update will be rolled back.
            // If all products are sufficient, the database will be updated, and insufficientProducts will be empty.
            // Note: If the trolley is already organized (merged and sorted), grouping is unnecessary.
           // ArrayList<Product> groupedTrolley = groupProductsById(trolleyList);
            // Grouping products by id for optimisation isn't necessary after merging existing products.
            ArrayList<Product> insufficientProducts = databaseRW.purchaseStocks(trolleyList);

            System.out.println(insufficientProducts);

            if(insufficientProducts.isEmpty()) { // If stock is sufficient for all products
                //get OrderHub and tell it to make a new Order
                OrderHub orderHub = OrderHub.getOrderHub();
                Order theOrder = orderHub.newOrder(trolleyList);
                trolleyList.clear();
                orderSummary = "Order Summary";
                displayTaReceipt = String.format(
                        "Order_ID: %s\nOrdered_Date_Time: %s\n%s",
                        theOrder.getOrderId(),
                        theOrder.getOrderedDateTime(),
                        ProductListFormatter.buildString(theOrder.getProductList())
                );
                System.out.println(displayTaReceipt);
            }
            else { // Some products have insufficient stock — build an error message to inform the customer
                orderSummary = "Checkout failed as one of the products have insufficient stock";
                StringBuilder errorMsg = new StringBuilder();
                for(Product p : insufficientProducts){
                    // Remove the present product with insufficient stock from the trolley list.
                    trolleyList.remove(p);
                    // Produce a proper error format for the error message
                    errorMsg.append("\u2022 ").append(p.getProductId()).append(", ")
                            .append(p.getProductDescription()).append(" (Only ")
                            .append(p.getStockQuantity()).append(" available, ")
                            .append(p.getOrderedQuantity()).append(" requested)\n");
                }
                proNotifier.showRemovalMsg(errorMsg.toString());
                System.out.println("stock is not enough");
            }
        }
        else{
            System.out.println("Your trolley is empty");
        }
        updateView();
    }

    void doCancel( ){
        trolleyList.clear();
        orderSummary = "Order Summary";
        updateView();
    }

    void doClear() {
        productList.clear();
        searchSummary = "Search Summary";
        updateView();
    }

    void doCloseReceipt(){
        displayTaReceipt="";
    }

    void updateView() {
        if (!trolleyList.isEmpty()) {
            double sumPrice = trolleyList.stream().mapToDouble(item -> item.getUnitPrice() * item.getOrderedQuantity()).sum();
            int totalItems = trolleyList.stream().mapToInt(Product::getOrderedQuantity).sum();
            orderSummary = String.format("Items (%d): £%.2f", totalItems, sumPrice);
        } else {
            orderSummary = "Order Summary";
        }
        cusView.update(productList, trolleyList, searchSummary, orderSummary);
    }
     // extra notes:
     //Path.toUri(): Converts a Path object (a file or a directory path) to a URI object.
     //File.toURI(): Converts a File object (a file on the filesystem) to a URI object
}
