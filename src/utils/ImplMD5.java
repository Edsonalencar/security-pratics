package utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ImplMD5 {
    static final Charset UTF_8 = StandardCharsets.UTF_8;

    public static byte[] handler (byte[] bytesEntrada, String alg) {
        MessageDigest md = null;

        try {
            md = MessageDigest.getInstance(alg);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        byte[] bytesResumo = md.digest(bytesEntrada);
        return bytesResumo;
     }

}
