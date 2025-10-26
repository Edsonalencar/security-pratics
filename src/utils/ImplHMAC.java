package utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class ImplHMAC {
    public static final String ALG = "HmacSHA256";
    //public static final String ALG = "HmacSHA224";
    //public static final String ALG = "HmacSHA384";
    //public static final String ALG = "HmacSHA512";

    public static String handler (String key, String message) throws Exception {
        Mac shaHMAC = Mac.getInstance(ALG);

        SecretKeySpec MACKey = new SecretKeySpec(key.getBytes("UTF-8"), ALG);
        shaHMAC.init(MACKey);

        byte[] bytesHMAC = shaHMAC.doFinal(message.getBytes("UTF-8"));

        return Byte2hex.handler(bytesHMAC);
    }
}
