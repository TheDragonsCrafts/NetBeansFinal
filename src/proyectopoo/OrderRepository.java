package proyectopoo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderRepository {
    private static final String ORDER_DATA_DIRECTORY = "src/textos/orders/";
    private ProductRepository productRepository;

    public OrderRepository(ProductRepository productRepository) {
        if (productRepository == null) {
            throw new IllegalArgumentException("ProductRepository cannot be null.");
        }
        this.productRepository = productRepository;
        new File(ORDER_DATA_DIRECTORY).mkdirs(); // Create directory if it doesn't exist
    }

    // Placeholder for getNextOrderId - will be implemented if needed by a calling class.
    // For now, addOrder assumes orderId is pre-assigned to the Order object.
    // public int getNextOrderId(String clientName) { ... }


    private String sanitizeClientName(String clientName) {
        if (clientName == null || clientName.trim().isEmpty()) {
            return "unknown_client";
        }
        return clientName.replaceAll("[^a-zA-Z0-9_.-]", "_").replace(' ', '_');
    }

    public boolean addOrder(Order order) {
        if (order == null) {
            return false;
        }
        String sanitizedClientName = sanitizeClientName(order.getClientName());
        File clientOrderFile = new File(ORDER_DATA_DIRECTORY + sanitizedClientName + "_orders.txt");
        
        // Ensure the directory exists
        File parentDir = clientOrderFile.getParentFile();
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                System.err.println("Could not create order data directory: " + parentDir.getAbsolutePath());
                return false;
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        try (FileWriter fw = new FileWriter(clientOrderFile, true);
             BufferedWriter writer = new BufferedWriter(fw)) {
            
            for (OrderItem item : order.getItems()) {
                String productNameForFile = item.getProduct().getName().replace(' ', '_');
                String dateString = dateFormat.format(order.getOrderDate());
                
                writer.write(order.getOrderId() + "\t" + 
                             productNameForFile + "\t" + 
                             item.getQuantity() + "\t" + 
                             item.getPriceAtTimeOfOrder() + "\t" +
                             dateString);
                writer.newLine();
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error writing order to file: " + e.getMessage());
            return false;
        }
    }

    public List<Order> getOrdersByClient(String clientName) {
        String sanitizedClientName = sanitizeClientName(clientName);
        File clientOrderFile = new File(ORDER_DATA_DIRECTORY + sanitizedClientName + "_orders.txt");

        if (!clientOrderFile.exists()) {
            return new ArrayList<>(); // No orders for this client
        }

        Map<Integer, List<OrderItem>> orderItemsMap = new HashMap<>();
        Map<Integer, Date> orderDatesMap = new HashMap<>();
        // To reconstruct the original client name if needed, but Order object takes the original name
        // Map<Integer, String> orderClientNameMap = new HashMap<>(); 

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        try (BufferedReader reader = new BufferedReader(new FileReader(clientOrderFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length < 5) {
                    System.err.println("Skipping malformed order line: " + line);
                    continue;
                }

                try {
                    int orderId = Integer.parseInt(parts[0]);
                    String productNameWithUnderscores = parts[1];
                    int quantity = Integer.parseInt(parts[2]);
                    double priceAtTimeOfOrder = Double.parseDouble(parts[3]);
                    Date orderDate = dateFormat.parse(parts[4]);

                    // Inefficiently find product by name.
                    // TODO: Implement findProductByName in ProductRepository for efficiency.
                    Product product = null;
                    String targetProductName = productNameWithUnderscores.replace('_', ' ');
                    for (Product p : productRepository.getAllProducts()) {
                        if (p.getName().equalsIgnoreCase(targetProductName)) {
                            product = p;
                            break;
                        }
                    }

                    if (product == null) {
                        System.err.println("Product not found: " + targetProductName + ". Skipping order item.");
                        continue; 
                    }

                    OrderItem orderItem = new OrderItem(product, quantity, priceAtTimeOfOrder);
                    
                    orderItemsMap.computeIfAbsent(orderId, k -> new ArrayList<>()).add(orderItem);
                    orderDatesMap.putIfAbsent(orderId, orderDate); // Date should be same for all items of an order

                } catch (NumberFormatException | ParseException e) {
                    System.err.println("Error parsing order line: " + line + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading order file for client " + clientName + ": " + e.getMessage());
            return new ArrayList<>();
        }

        List<Order> clientOrders = new ArrayList<>();
        for (Map.Entry<Integer, List<OrderItem>> entry : orderItemsMap.entrySet()) {
            int orderId = entry.getKey();
            List<OrderItem> items = entry.getValue();
            Date date = orderDatesMap.get(orderId);
            
            if (items != null && !items.isEmpty() && date != null) {
                // The original clientName (passed as argument) should be used for constructing the Order,
                // as sanitizedClientName is just for filename.
                try {
                    Order order = new Order(orderId, clientName, items, date);
                    clientOrders.add(order);
                } catch (IllegalArgumentException e) {
                    System.err.println("Error creating order object for orderId " + orderId + ": " + e.getMessage());
                }
            }
        }
        return clientOrders;
    }

    public List<Order> getAllOrders() {
        File dir = new File(ORDER_DATA_DIRECTORY);
        if (!dir.exists() || !dir.isDirectory()) {
            return new ArrayList<>();
        }

        File[] orderFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith("_orders.txt");
            }
        });

        List<Order> allOrders = new ArrayList<>();
        if (orderFiles == null) {
            return allOrders;
        }

        for (File orderFile : orderFiles) {
            String fileName = orderFile.getName();
            // Extract client name from filename: "Client_Name_orders.txt" -> "Client Name"
            String sanitizedClientName = fileName.substring(0, fileName.length() - "_orders.txt".length());
            // This is tricky: sanitization is one-way. We can't perfectly get the original client name.
            // For now, we'll use the sanitized name as the client name when loading all orders.
            // A better approach would be to store original client name inside the file or have a separate mapping.
            // Or, the Order object could store the original client name, and we rely on that.
            // The current getOrdersByClient takes the original client name, so we need to adapt.
            // For now, let's assume the client name used for constructing the Order in getOrdersByClient
            // is the one we want. If getOrdersByClient is called with a "guessed" client name from the
            // filename, that name will be used in the Order objects.
            
            // We need a way to get the "original" client name. The current structure
            // assumes getOrdersByClient is called with the actual client name.
            // For getAllOrders, we are deriving a "client name" from the filename.
            // This derived name might not be the original.
            // Let's assume the filename's prefix is the client name we should use.
            String clientNameFromFile = sanitizedClientName.replace('_', ' '); 


            List<Order> clientOrders = getOrdersByClient(clientNameFromFile); // This will use clientNameFromFile
            allOrders.addAll(clientOrders);
        }
        
        // Due to how orders are grouped in getOrdersByClient, some orders might be duplicated
        // if a client name derived from one file matches the client name derived from another
        // (e.g., "Client A_orders.txt" and "Client_A_orders.txt" might both yield "Client A").
        // Or, if getOrdersByClient uses the clientName parameter to build the Order, and we pass
        // the filename-derived client name, it should be fine.
        // The current implementation of getOrdersByClient uses the provided clientName parameter for the Order object.
        return allOrders;
    }
}
