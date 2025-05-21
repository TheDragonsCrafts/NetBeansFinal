package proyectopoo;

public class Product {
    private int id;
    private String name;
    private String size;
    private int pieces;
    private double cost;
    private String imagePath;

    public Product(int id, String name, String size, int pieces, double cost, String imagePath) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.pieces = pieces;
        this.cost = cost;
        this.imagePath = imagePath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getPieces() {
        return pieces;
    }

    public void setPieces(int pieces) {
        if (pieces < 0) {
            throw new IllegalArgumentException("Pieces cannot be less than 0.");
        }
        this.pieces = pieces;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        if (cost < 0.0) {
            throw new IllegalArgumentException("Cost cannot be less than 0.0.");
        }
        this.cost = cost;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
