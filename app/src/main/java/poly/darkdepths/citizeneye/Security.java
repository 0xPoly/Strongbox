package poly.darkdepths.citizeneye;

import android.content.Context;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.DataInputStream;


/**
 * This class handles all security functions in Strongbox.
 * Iteration count and salt length from http://csrc.nist.gov/publications/nistpubs/800-132/nist-sp800-132.pdf
 */
public class Security {
    private SecretKey key;

    // TODO is this safe?
    public SecretKey getKey(){
        return this.key;
    }

    public void generateKey(char[] passphraseOrPin, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Number of PBKDF2 hardening rounds to use. Above 1000 round NIST recommendation
        // TODO how much time does this take?
        final int iterations = 5000;

        // Generate a 256-bit key
        final int outputKeyLength = 256;

        // TODO find a way to make SHA256 work!
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec keySpec = new PBEKeySpec(passphraseOrPin, salt, iterations, outputKeyLength);
        SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);

        this.key = secretKey;
    }

    private static byte[] generateSalt() {
        // Above 128 bit NIST recommendation
        final int saltLength = 32;

        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[saltLength];
        sr.nextBytes(salt);

        return salt;
    }

    /**
     * Function generates cryptographically random salt and stores it on internal storage,
     * accessible only to this application. Context must be passed for openFileOutput accessibility
     */
    public static void writeSalt(Context ctx) {
        FileOutputStream outputStream;
        final String filename = "salt.cfg";
        byte[] salt = generateSalt();

        try {
            outputStream = ctx.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(salt);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves salt from internal storage and returns byte array
     * @param ctx
     * @return
     */
    public static byte[] getSalt(Context ctx) {
        InputStream inputStream;
        DataInputStream dataInputStream;
        String saltFile = "salt.cfg";
        byte[] salt = {0};

        try {
            inputStream = ctx.openFileInput(saltFile);
            dataInputStream = new DataInputStream(inputStream);
            int length = (int) dataInputStream.available();
            salt = new byte[length];
            dataInputStream.readFully(salt);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return salt;
    }

    public static byte[] encrypt(SecretKey key, byte[] input){
        byte[] output = null;
        key = new SecretKeySpec(key.getEncoded(), "AES");


        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            output = cipher.doFinal(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    public static byte[] decrypt(SecretKey key, byte[] input){
        byte[] output = null;
        key = new SecretKeySpec(key.getEncoded(), "AES");

        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            output = cipher.doFinal(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }
}
