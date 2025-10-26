package utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class ImplAES {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 256; // Chave de 256 bits

    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(KEY_SIZE, new SecureRandom());
        return keyGen.generateKey();
    }

    public static byte[] generateIv() {
        byte[] iv = new byte[16]; // 16 bytes = 128 bits
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public static byte[] encrypt(String plaintext, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        return cipher.doFinal(plaintext.getBytes("UTF-8"));
    }

    public static String decrypt(byte[] ciphertext, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] original = cipher.doFinal(ciphertext);
        return new String(original, "UTF-8");
    }

    public static SecretKey deriveAESKey(String passphrase) {
        try {
            byte[] keyBytes = passphrase.getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            keyBytes = sha.digest(keyBytes);
            // Pega os primeiros 32 bytes (256 bits) para a chave AES
            keyBytes = Arrays.copyOf(keyBytes, 32);

            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao derivar a chave AES.", e);
        }
    }

    public static String bytesToBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] base64ToBytes(String base64) {
        return Base64.getDecoder().decode(base64);
    }
}
