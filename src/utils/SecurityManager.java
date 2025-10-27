package utils;

import message.SecureMessage;
import message.SecureMessageCommand;

import javax.crypto.SecretKey;

public class SecurityManager {

    private static final SecretKey SECRET_KEY = ImplAES.deriveAESKey("Ufersa_Seguranca_2025_Pratica_Off1");
    public static String HMAC_KEY = "CHAVE_SECRETA_HMAC_1234567890";

    public static SecureMessage createSecuredRequest(SecureMessageCommand command, String resourceName, String newAddress) throws Exception {
        String originalMessage = command + "|" + resourceName + "|" + (newAddress != null ? newAddress : "null");
        byte[] macBytes = ImplHMAC.handler(HMAC_KEY, originalMessage).getBytes("UTF-8");

        byte[] iv = ImplAES.generateIv();
        byte[] cipherText = ImplAES.encrypt(originalMessage, SECRET_KEY, iv);

        return new SecureMessage(command, cipherText, iv, macBytes, resourceName, newAddress);
    }

    public static String processSecuredMessage(SecureMessage message) throws Exception {
        String originalMessage = ImplAES.decrypt(message.getCiphertext(), SECRET_KEY, message.getIv());
        String expectedMacHex = ImplHMAC.handler(HMAC_KEY, originalMessage);
        String receivedMacHex = new String(message.getMac(), "UTF-8");

        if (!expectedMacHex.equals(receivedMacHex))
            throw new SecurityException("Falha na autenticação ou integridade (MAC inválido). Mensagem descartada.");

        return originalMessage;
    }
}
