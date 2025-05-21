package proyectopoo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Order {
    private int orderId;
    private String clientName;
    private List<OrderItem> items;
    private double totalAmount;
    private Date orderDate;

    public Order(int orderId, String clientName, List<OrderItem> items, Date orderDate) {
        if (clientName == null || clientName.trim().isEmpty()) {
            throw new IllegalArgumentException("Client name cannot be null or empty.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Items list cannot be null or empty.");
        }
        if (orderDate == null) {
            throw new IllegalArgumentException("Order date cannot be null.");
        }

        this.orderId = orderId;
        this.clientName = clientName;
        this.items = new ArrayList<>(items); // Ensure mutability and ownership
        this.orderDate = orderDate;
        
        // Calculate totalAmount
        this.totalAmount = 0;
        for (OrderItem item : this.items) {
            if (item == null) { // Additional check for null items in the list
                throw new IllegalArgumentException("Order item within the list cannot be null.");
            }
            this.totalAmount += item.getTotalPrice();
        }
        if (this.totalAmount < 0) { // Should not happen if item prices are validated, but good for robustness
             throw new IllegalArgumentException("Total amount cannot be negative.");
        }
    }

    public int getOrderId() {
        return orderId;
    }

    public String getClientName() {
        return clientName;
    }

    public List<OrderItem> getItems() {
        // Return a copy to maintain encapsulation if external modification is a concern
        // For now, returning the direct reference as per simple getter instruction.
        return items; 
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public Date getOrderDate() {
        return orderDate;
    }
}
