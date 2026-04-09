package org.example.back.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * AES 对称加密/解密工具，与前端 crypto-js ECB/PKCS7 模式对齐
 */
public class AESUtil {

    private static final String KEY = "ElecEnergy@16Key";
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";

    public static String decrypt(String encryptedText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("密码解密失败", e);
        }
    }
}
