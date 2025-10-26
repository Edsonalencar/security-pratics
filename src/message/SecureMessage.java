package message;

import java.io.Serializable;

public class SecureMessage implements Serializable {

    private SecureMessageCommand command;

    private byte[] ciphertext;
    private byte[] iv;
    private byte[] mac;

    private String resourceName; // O nome do servidor
    private String newAddress; // Novo endereço

    public SecureMessage() {}

    public SecureMessage(
        SecureMessageCommand command,
        byte[] ciphertext,
        byte[] iv,
        byte[] mac,
        String resourceName,
        String newAddress
    ) {
        this.command = command;
        this.ciphertext = ciphertext;
        this.iv = iv;
        this.mac = mac;
        this.resourceName = resourceName;
        this.newAddress = newAddress;
    }

    // Getters
    public SecureMessageCommand getCommand() { return command; }
    public byte[] getCiphertext() { return ciphertext; }
    public byte[] getIv() { return iv; }
    public byte[] getMac() { return mac; }
    public String getResourceName() { return resourceName; }
    public String getNewAddress() { return newAddress; }

    // setters
    public void setCommand(SecureMessageCommand command) {this.command = command;}
    public void setCiphertext(byte[] ciphertext) {this.ciphertext = ciphertext;}
    public void setIv(byte[] iv) {this.iv = iv;}
    public void setMac(byte[] mac) {this.mac = mac;}
    public void setResourceName(String resourceName) {this.resourceName = resourceName;}
    public void setNewAddress(String newAddress) {this.newAddress = newAddress;}
}
