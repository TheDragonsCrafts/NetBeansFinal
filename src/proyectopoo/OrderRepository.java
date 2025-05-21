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
        // Stricter sanitization: replace dots and dashes as well to prevent path traversal components like ".."
        // Allow only alphanumeric and underscore.
        String sanitized = clientName.replaceAll("[^a-zA-Z0-9]", "_");
        // Replace multiple underscores with a single one if desired (optional)
        // sanitized = sanitized.replaceAll("__+", "_");
        if (sanitized.trim().isEmpty()) { // If original name was all special chars
            return "unknown_client";
        }
        return sanitized;
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

        File[] orderFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith("_orders.txt"));

        List<Order> allOrders = new ArrayList<>();
        if (orderFiles == null) {
            System.err.println("Warning: orderFiles array is null. Check directory permissions or I/O issue for: " + ORDER_DATA_DIRECTORY);
            return allOrders;
        }

        for (File orderFile : orderFiles) {
            String fileName = orderFile.getName();
            String clientNameSuffix = "_orders.txt";
            
            if (fileName.length() <= clientNameSuffix.length()) {
                System.err.println("Skipping malformed order filename: " + fileName);
                continue;
            }
            
            // Extract client name from filename: "Client_Name_orders.txt" -> "Client Name"
            // This part assumes the client name was sanitized by replacing spaces with underscores
            // and does not perfectly reverse all sanitization (e.g. if original had multiple underscores).
            String sanitizedClientNameFromFile = fileName.substring(0, fileName.length() - clientNameSuffix.length());
            
            // The clientName used to create the Order object within getOrdersByClient
            // should ideally be the *original* client name if it were stored.
            // Since it's not, using the sanitized-then-desanitized name is the current best effort.
            String clientNameFromFile = sanitizedClientNameFromFile.replace('_', ' '); 

            // It's important that getOrdersByClient can correctly find the file
            // using the clientNameFromFile, so clientNameFromFile should lead to
            // the same sanitizedClientName as stored.
            // If clientNameFromFile was "Client Name", sanitizeClientName("Client Name") -> "Client_Name"
            // This means clientNameFromFile should be what sanitizeClientName would produce if it were "Client_Name" initially.
            // The current sanitizeClientName replaces spaces with underscores.
            // So, if original was "Client Name", it becomes "Client_Name".
            // If filename is "Client_Name_orders.txt", then sanitizedClientNameFromFile is "Client_Name".
            // clientNameFromFile becomes "Client Name".
            // Then getOrdersByClient called with "Client Name" will sanitize it to "Client_Name" and find the file. This seems okay.

            List<Order> clientOrders = getOrdersByClient(clientNameFromFile); 
            allOrders.addAll(clientOrders);
        }
        
        // Removing duplicates might be needed if multiple filenames could resolve to the same
        // logical client after de-sanitization, but given the current sanitization (unique names
        // should map to unique sanitized names), this is less of a concern unless there are
        // case sensitivity issues or similar minor variations not handled by sanitization.
        // The current getOrdersByClient groups by orderId, so it should not add duplicate Order objects
        // for the same orderId from the same file. The concern is more about adding the same logical
        // order set multiple times if "clientA_orders.txt" and "ClientA_orders.txt" both exist and map
        // to the same clientNameFromFile. FilenameFilter is case-insensitive already.
        return allOrders;
    }
}
