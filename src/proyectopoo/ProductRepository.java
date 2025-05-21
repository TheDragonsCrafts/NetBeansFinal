package proyectopoo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProductRepository {
    private static final String DATA_FILE_PATH = "src/textos/POO.txt";

    public int getNextID() {
        int maxID = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = line.split(" ");
                    if (parts.length > 0) {
                        int id = Integer.parseInt(parts[0]);
                        if (id > maxID) {
                            maxID = id;
                        }
                    }
                } catch (NumberFormatException e) {
                    // Log error or skip line
                    System.err.println("Skipping line due to NumberFormatException: " + line);
                }
            }
        } catch (IOException e) {
            // If file doesn't exist or other IO error, start IDs from 1
            return 1;
        }
        return maxID + 1;
    }

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = line.split(" ");
                    if (parts.length >= 5) {
                        int id = Integer.parseInt(parts[0]);
                        String name = parts[1].replace('_', ' ');
                        String size = parts[2];
                        int pieces = Integer.parseInt(parts[3]);
                        double cost = Double.parseDouble(parts[4]);
                        // Assuming imagePath is not in this file format or is handled differently.
                        // For now, let's pass a null or empty string for imagePath.
                        String imagePath = (parts.length > 5) ? parts[5] : ""; 

                        products.add(new Product(id, name, size, pieces, cost, imagePath));
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Skipping malformed line: " + line + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading data file: " + e.getMessage());
            // Return empty list if file not found or other IO error
        }
        return products;
    }

    public boolean addProduct(Product product) {
        File dataFile = new File(DATA_FILE_PATH);
        File tempFile = new File(dataFile.getParent(), "POO.tmp");
        File backupFile = new File(dataFile.getParent(), "POO.bak"); // For restoring original if rename fails

        // Ensure the directory exists
        File parentDir = dataFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        List<Product> products = getAllProducts(); // Read existing products
        products.add(product); // Add the new product

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            for (Product p : products) {
                String nameForFile = p.getName().replace(' ', '_');
                writer.write(p.getId() + " " + nameForFile + " " + p.getSize() + " " + p.getPieces() + " " + p.getCost());
                writer.newLine();
            }
            writer.flush(); // Ensure all data is written to the temp file
        } catch (IOException e) {
            System.err.println("Error writing to temporary file: " + e.getMessage());
            // Attempt to delete tempFile if it was created
            if (tempFile.exists()) {
                tempFile.delete();
            }
            return false;
        }

        try {
            // Create a backup of the original file (if it exists)
            if (dataFile.exists() && dataFile.length() > 0) { // Check if dataFile exists and is not empty
                Files.copy(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Delete original file
            if (dataFile.exists()) {
                if (!dataFile.delete()) {
                    System.err.println("Could not delete original data file.");
                    // Attempt to restore from backup if deletion failed
                    if (backupFile.exists()) {
                        Files.move(backupFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    return false;
                }
            }

            // Rename temp file to original file name
            if (!tempFile.renameTo(dataFile)) {
                System.err.println("Could not rename temp file to data file.");
                // Attempt to restore from backup if rename failed
                if (backupFile.exists()) {
                    Files.move(backupFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return false;
            }

            // If successful, delete the backup
            if (backupFile.exists()) {
                backupFile.delete();
            }
            return true;

        } catch (IOException e) {
            System.err.println("IOException during file operations (delete/rename): " + e.getMessage());
            // Attempt to restore the original file from backup
            if (backupFile.exists()) {
                try {
                    Files.move(backupFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.err.println("Original file restored from backup.");
                } catch (IOException restoreEx) {
                    System.err.println("Could not restore original file from backup: " + restoreEx.getMessage());
                }
            }
            // Attempt to delete tempFile if it still exists
            if (tempFile.exists()) {
                tempFile.delete();
            }
            return false;
        }
    }

    public boolean deleteProduct(int productId) {
        List<Product> products = getAllProducts();
        List<Product> updatedProducts = products.stream()
                                                .filter(p -> p.getId() != productId)
                                                .collect(Collectors.toList());

        if (products.size() == updatedProducts.size()) {
            // Product not found, or list was empty. Consider this a success.
            return true;
        }

        File dataFile = new File(DATA_FILE_PATH);
        File tempFile = new File(dataFile.getParent(), "POO.tmp");
        File backupFile = new File(dataFile.getParent(), "POO.bak");

        // Ensure the directory exists
        File parentDir = dataFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            for (Product p : updatedProducts) {
                String nameForFile = p.getName().replace(' ', '_');
                writer.write(p.getId() + " " + nameForFile + " " + p.getSize() + " " + p.getPieces() + " " + p.getCost());
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing updated product list to temporary file: " + e.getMessage());
            if (tempFile.exists()) {
                tempFile.delete();
            }
            return false;
        }

        try {
            if (dataFile.exists() && dataFile.length() > 0) {
                 Files.copy(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            if (dataFile.exists()) {
                if (!dataFile.delete()) {
                    System.err.println("Could not delete original data file for delete operation.");
                    if (backupFile.exists()) {
                        Files.move(backupFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    return false;
                }
            }

            if (!tempFile.renameTo(dataFile)) {
                System.err.println("Could not rename temp file to data file for delete operation.");
                 if (backupFile.exists()) {
                    Files.move(backupFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return false;
            }
            if (backupFile.exists()) {
                backupFile.delete();
            }
            return true;
        } catch (IOException e) {
            System.err.println("IOException during file operations (delete/rename) for delete operation: " + e.getMessage());
            if (backupFile.exists()) {
                try {
                    Files.move(backupFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.err.println("Original file restored from backup after delete operation failure.");
                } catch (IOException restoreEx) {
                    System.err.println("Could not restore original file from backup after delete operation failure: " + restoreEx.getMessage());
                }
            }
            if (tempFile.exists()) {
                tempFile.delete();
            }
            return false;
        }
    }
}
