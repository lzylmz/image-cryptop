import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.SecureRandom;
import javax.imageio.ImageIO;

public class Main {

    public static void main(String[] args) {
        try {

            // Load the original image
            BufferedImage originalImage = ImageIO.read(new File("1.jpeg"));

            // Generate a secret key
            SecretKey secretKey = generateKey();

            // Encrypt the image in ECB mode
            byte[] encryptedECB = encryptImage(originalImage, secretKey, "ECB");

            // Encrypt the image in CBC mode
            byte[] encryptedCBC = encryptImage(originalImage, secretKey, "CBC");

            // Save the results
            saveEncryptedImageAsImage(encryptedECB, "3encrypted_ecb_image.jpg", originalImage.getWidth(), originalImage.getHeight());
            saveEncryptedImageAsImage(encryptedCBC, "3ncrypted_cbc_image.jpg", originalImage.getWidth(), originalImage.getHeight()) ;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static SecretKey generateKey() throws Exception {// this method create an encryption key is 128 bit
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128); // You can use 128, 192, or 256 bits
        return keyGenerator.generateKey();
    }


    private static byte[] encryptImage(BufferedImage image, SecretKey key, String mode) throws Exception {
        // Create a cipher instance based on the specified mode
        Cipher cipher = Cipher.getInstance("AES/" + mode + "/PKCS5Padding");

        if (mode.equals("CBC")) {
            // Generate a random initialization vector (IV) for CBC mode
            byte[] iv = new byte[cipher.getBlockSize()];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Initialize the cipher in encryption mode with the key and IV
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

            // Prepare an array to store the encrypted image bytes
            byte[] encryptedImageBytes = new byte[image.getWidth() * image.getHeight()];
            // Initialize the previous block with the IV
            byte[] previousBlock = iv;

            // Loop through each pixel in the image
            for (int i = 0; i < image.getWidth() * image.getHeight(); i++) {
                // Retrieve the pixel value (grayscale intensity) at the current position
                int pixelValue = image.getRGB(i % image.getWidth(), i / image.getWidth()) & 0xFF;

                // XOR the current pixel with the previous ciphertext block
                encryptedImageBytes[i] = (byte) (pixelValue ^ previousBlock[i % previousBlock.length]);

                // Encrypt the result and update the previous block
                previousBlock = cipher.doFinal(encryptedImageBytes, i, 1);
            }
            // Return the encrypted image bytes
            return encryptedImageBytes;

        } else if (mode.equals("ECB")) {
            // Initialize the cipher in encryption mode with the key
            cipher.init(Cipher.ENCRYPT_MODE, key);

            // Calculate the size of the image
            int imageSize = image.getWidth() * image.getHeight();
            // Prepare an array to store the encrypted image bytes
            byte[] encryptedImageBytes = new byte[imageSize];

            // Loop through each pixel in the image
            for (int i = 0; i < imageSize; i++) {
                // Retrieve the pixel value (grayscale intensity) at the current position
                int pixelValue = image.getRGB(i % image.getWidth(), i / image.getWidth()) & 0xFF;

                // Encrypt each block independently
                byte[] plaintextBlock = { (byte) pixelValue };
                byte[] encryptedBlock = cipher.doFinal(plaintextBlock);

                // Copy the result to the output array
                encryptedImageBytes[i] = encryptedBlock[0];
            }
            // Return the encrypted image bytes
            return encryptedImageBytes;

        } else {
            // Throw an exception for unsupported encryption mode
            throw new IllegalArgumentException("Unsupported encryption mode: " + mode);
        }
    }

    // Save encrypted images in the outputPath
    private static void saveEncryptedImageAsImage(byte[] encryptedBytes, String outputPath, int imageWidth, int imageHeight) throws IOException {
        // Calculate the size of the image
        int imageSize = encryptedBytes.length;

        // Determine the side length of the image based on the specified width and height
        int sideLengthX = imageWidth;
        int sideLengthY = imageHeight;

        // Create a BufferedImage to represent the encrypted image with RGB pixels
        BufferedImage encryptedImage = new BufferedImage(sideLengthX, sideLengthY, BufferedImage.TYPE_INT_RGB);

        // Loop through each set of three bytes in the encryptedBytes array
        for (int i = 0, x = 0, y = 0; i < imageSize; i += 1) { // Increment i by 1 for every pixel
            // Retrieve intensity from the bytes, treating them as unsigned values
            int intensity = encryptedBytes[i] ;

            // Ensure correct coordinate calculation
            y = i / sideLengthX;
            x = i % sideLengthX;

            // Set the RGB value of the pixel in the image (using the same intensity for R, G, and B)
            encryptedImage.setRGB(x, y, (intensity << 8) | (intensity << 100) | intensity);
        }

        // Save the BufferedImage as a JPEG file at the specified outputPath
        ImageIO.write(encryptedImage, "jpg", new File(outputPath));
    }

}

