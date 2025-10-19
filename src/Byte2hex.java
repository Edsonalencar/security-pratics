public class Byte2hex {

    public static String handler (byte[] bytes) {

        StringBuilder strHex = new StringBuilder();

        for (byte b : bytes)
            strHex.append(String.format("%02x", b & 0xFF));

        return strHex.toString();
    }
}
