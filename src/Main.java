import utils.Byte2hex;
import utils.ImplMD5;

public class Main {

    public static void main(String[] args) {
        String algoritmo = "SHA3-256"; // MD5 | SHA3-256
        String texto = "Funções de hash";

        System.out.println("Entrada (string): " + texto);
        System.out.println("Entrada (tamanho): " + texto.length());

        byte[] bytesTextoMD5 = ImplMD5.handler(texto.getBytes(ImplMD5.UTF_8), algoritmo);

        System.out.println("Hexa: " + Byte2hex.handler(bytesTextoMD5));
        System.out.println("Tamanho: " + bytesTextoMD5.length);
    }

}