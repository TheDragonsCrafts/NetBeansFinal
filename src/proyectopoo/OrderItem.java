package proyectopoo;

public class OrderItem {
    private Product product;
    private int quantity;
    private double priceAtTimeOfOrder;

    public OrderItem(Product product, int quantity, double priceAtTimeOfOrder) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
        if (priceAtTimeOfOrder < 0) {
            throw new IllegalArgumentException("Price at time of order must be greater than or equal to 0.");
        }
        this.product = product;
        this.quantity = quantity;
        this.priceAtTimeOfOrder = priceAtTimeOfOrder;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPriceAtTimeOfOrder() {
        return priceAtTimeOfOrder;
    }

    public double getTotalPrice() {
        return quantity * priceAtTimeOfOrder;
    }
}
