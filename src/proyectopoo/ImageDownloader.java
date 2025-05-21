package proyectopoo;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.imageio.ImageIO;

public class ImageDownloader {

    private static final String IMAGE_DIRECTORY = "src/imagenes/";

    public static String downloadAndSaveImage(String urlString, String productName) {
        // 1. URL Validation
        if (urlString == null || urlString.isEmpty()) {
            System.err.println("Error downloading image: URL string cannot be null or empty.");
            return null; // As per instruction: return null on failure from public method
        }

        URL url;
        try {
            url = new URL(urlString);
            String protocol = url.getProtocol();
            if (!protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https")) {
                System.err.println("Error downloading image: Invalid URL protocol. Only HTTP/HTTPS are allowed. Provided: " + protocol);
                return null;
            }
        } catch (MalformedURLException e) {
            System.err.println("Error downloading image: Malformed URL - " + e.getMessage());
            return null;
        }

        // 2. Filename Sanitization
        String sanitizedProductName = productName.replace(' ', '_');
        sanitizedProductName = sanitizedProductName.replaceAll("[^a-zA-Z0-9_.]", "");
        if (sanitizedProductName.isEmpty()) {
            sanitizedProductName = "default_image_name"; // fallback if product name is all invalid chars
        }
        String filename = sanitizedProductName + ".png";

        File imageDir = new File(IMAGE_DIRECTORY);
        if (!imageDir.exists()) {
            if (!imageDir.mkdirs()) {
                System.err.println("Error downloading image: Could not create image directory: " + IMAGE_DIRECTORY);
                return null;
            }
        }

        File outputFile = new File(IMAGE_DIRECTORY + filename);

        InputStream inputStream = null;
        HttpURLConnection connection = null;
        try {
            // 3. Image Download and Processing
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0"); // Some servers require a user-agent
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Error downloading image: Server returned HTTP response code: " + responseCode + " for URL: " + urlString);
                return null;
            }

            inputStream = connection.getInputStream();
            BufferedImage originalImage = ImageIO.read(inputStream);

            if (originalImage == null) {
                 System.err.println("Error downloading image: Failed to read image from URL (ImageIO.read returned null). URL: " + urlString);
                return null;
            }

            Image scaledImage = originalImage.getScaledInstance(200, 200, Image.SCALE_SMOOTH);

            BufferedImage bufferedScaledImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_PNG);
            Graphics2D g2d = bufferedScaledImage.createGraphics();
            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();

            // 4. Image Saving
            if (!ImageIO.write(bufferedScaledImage, "png", outputFile)) {
                 System.err.println("Error downloading image: ImageIO.write failed for " + outputFile.getAbsolutePath());
                 // This case might not be typically hit if an IOException isn't thrown, but good to be aware.
                return null;
            }

            // 5. Return Value
            return outputFile.getAbsolutePath();

        } catch (IllegalArgumentException e) { // Catching potential IAE from ImageIO.read if stream is null
            System.err.println("Error downloading image (IllegalArgumentException): " + e.getMessage());
            return null;
        } catch (IOException e) {
            System.err.println("Error downloading image (IOException): " + e.getMessage() + " for URL: " + urlString);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    System.err.println("Error closing input stream: " + e.getMessage());
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
