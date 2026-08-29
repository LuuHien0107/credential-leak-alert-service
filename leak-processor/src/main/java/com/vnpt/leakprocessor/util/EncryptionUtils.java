package com.vnpt.leakprocessor.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Công cụ hỗ trợ mã hóa và giải mã mật khẩu lộ lọt bằng thuật toán đối xứng AES
 * (128-bit).
 */
public class EncryptionUtils {

    // Khóa bí mật 16-byte (128-bit) dùng cho mã hóa AES đối xứng
    private static final String SECRET_KEY = "VNPT_SmartCA_Key";
    private static final String ALGORITHM = "AES";

    /**
     * Mã hóa chuỗi văn bản thuần túy sang định dạng chuỗi mã hóa Base64 bằng thuật
     * toán AES.
     *
     * @param plainText chuỗi mật khẩu văn bản thường cần mã hóa.
     * @return chuỗi đã mã hóa định dạng Base64.
     */
    public static String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Có lỗi phát sinh trong quá trình mã hóa dữ liệu", e);
        }
    }

    /**
     * Giải mã chuỗi mã hóa định dạng Base64 ngược lại văn bản thường bằng thuật
     * toán AES.
     *
     * @param encryptedText chuỗi mã hóa dạng Base64 cần giải mã.
     * @return mật khẩu văn bản thường ban đầu.
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Có lỗi phát sinh trong quá trình giải mã dữ liệu", e);
        }
    }
}
