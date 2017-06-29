package Util;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by wjw_w on 2017/6/22.
 */
public class CryptorUtil {


    public static String getSaltedMD5(String src, byte salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(src.getBytes());
            byte[] md5 = md.digest();
            md5[0] = salt;

            return new BigInteger(1, md5).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getRandomSaltedMD5(String src) {
        byte salt = (byte) Math.abs(new SecureRandom().nextInt(256));
        src += salt;
        return getSaltedMD5(src, salt);
    }

    public static boolean equalsSaltedMD5(String src, String md5) {
        //byte salt = md5.getBytes()[0];
        String saltHex=md5.substring(0,2);
        byte salt=(byte)Integer.parseInt(saltHex,16);
        String srcMD5 = getSaltedMD5(src+salt, salt);
        return srcMD5.equals(md5);
    }


    public static String encryptBASE64(byte[] data) throws IOException {
        return (new BASE64Encoder()).encodeBuffer(data);
    }

    public static byte[] decryptBASE64(String data) throws IOException {
        return (new BASE64Decoder()).decodeBuffer(data);
    }

    /*public static byte[] encryptData(String BASE64PubKey, byte[] data) {

        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decryptBASE64(BASE64PubKey));
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Encrypt Failed");
        }
    }*/

    public static String pack(String BASE64PubKey, String rawData) throws IOException {
        byte[] encryptedData = cryptData(BASE64PubKey, rawData.getBytes(), Cipher.ENCRYPT_MODE);
        return encryptBASE64(encryptedData);
    }

    public static String unpack(String BASE64PriKey, String BASE64Data) throws IOException {
        byte[] data = decryptBASE64(BASE64Data);
        byte[] decryptedData = cryptData(BASE64PriKey, data, Cipher.DECRYPT_MODE);
        return new String(decryptedData, "UTF-8");
    }


    /*public static byte[] cryptData(Key key, byte[] data, int cryptMode) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(cryptMode, key);
            return cipher.doFinal(data);

        } catch (Exception e) {
            throw new IllegalStateException("Crypt Failed");
        }
    }*/

    public static byte[] cryptData(String BASE64Key, byte[] data, int cryptMode) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            KeySpec keySpec;
            Key key;
            switch (cryptMode) {
                case Cipher.ENCRYPT_MODE:
                    keySpec = new X509EncodedKeySpec(decryptBASE64(BASE64Key));
                    key = keyFactory.generatePublic(keySpec);
                    break;
                case Cipher.DECRYPT_MODE:
                    keySpec = new PKCS8EncodedKeySpec(decryptBASE64(BASE64Key));
                    key = keyFactory.generatePrivate(keySpec);
                    break;
                default:
                    return null;
            }
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(cryptMode, key);
            return cipher.doFinal(data);

        } catch (Exception e) {
            throw new IllegalStateException("Crypt Failed");
        }
    }

}
