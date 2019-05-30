package unimelb.bitbox;

import java.io.*;

import javax.crypto.Cipher;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import java.util.Base64;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.junit.Ignore;
import org.junit.Test;

import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

/*
 * For client to get and read public and private key from local files
 */

public class ClientKeys {
    private static Map<Integer, String> keyMap = new HashMap<Integer, String>();

    public static void main(String[] args) throws Exception {

        // Read local private key file and get key strings
//        String priKey = ClientKeys.getKeyContent("/client3.pem");
        String priKey = ClientKeys.getKeyContent("bitboxclient_rsa");

        System.out.println("private key origin str: \n" + priKey);

        // Read local public key file and get key strings
//        String pubKey = ClientKeys.getKeyContent("/client3.pem.pub");
        String pubKey = ClientKeys.getKeyContent("bitboxclient_rsa.pub");
        System.out.println("public key origin str:: \n" + pubKey);

        // convert public key string into compatible key format and get the identity
        PublicKey publicKey = RSAConverter.decodePublicKey(pubKey);
        String identity = RSAConverter.identity;
        System.out.println("identity is :"+identity);

        // convert private key into compatible key format
        PrivateKey privateKey = RSAConverter.convertPriKey(priKey);

        System.out.println("\nTesting RSA :\n");
        String encrypted = PubEncrypt("i am message for testing encryption", publicKey);
        System.out.println("Encrypted message : "+encrypted);

        String decrypted = PriDecrypt(encrypted, privateKey);
        System.out.println("Decrypted message : "+decrypted);


//        System.out.println("\nTetsing generating keys: \n");
//        genKeyPair();

    }

    public static void genKeyPair() throws NoSuchAlgorithmException {
        // KeyPairGenerator to generate pub/pri pair
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        // initialize key pair with given size
        keyPairGen.initialize(1024,new SecureRandom());
        // generate key pair
        KeyPair keyPair = keyPairGen.generateKeyPair();
        // get private key
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        // get private key
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        // get private key string
        String privateKeyString = Base64.getEncoder().encodeToString((privateKey.getEncoded()));
        // save pub and private key into map
        keyMap.put(0,publicKeyString);  //0 for public key
        keyMap.put(1,privateKeyString);  //1 for private key
    }

    /*
     * read key files from local file
     * @param key file name
     * @return pub/pri key
     */
    private static String getKeyContent(String filename) throws IOException {
        InputStream is = new FileInputStream(filename);
//        InputStream is = ClientKeys.class.getResourceAsStream(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        String lines = "";
        while ((line = reader.readLine()) != null) {
            lines = lines + line;
//            lines += "\n";

        }
        // TODO: remove last \n character
//        lines.substring(0,lines.length() - 1);

        return lines;
    }


    public static String PriEncrypt( String str, PrivateKey priKey) throws Exception{
        // using RSA de encrypt
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, priKey);
        String outStr = Base64.getEncoder().encodeToString(cipher.doFinal(str.getBytes("UTF-8")));

        return outStr;
    }

    public static String PubDecrypt( String str, PublicKey pubKey) throws Exception{

        // decode the encrypted string
        byte[] inputByte = Base64.getDecoder().decode(str.getBytes("UTF-8"));
        // use rsa to decode
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, pubKey);
        String outStr = new String(cipher.doFinal(inputByte));
        return outStr;
    }

    /*
     * use public key to encrypt the string
     * @param string that needs to be encrypted
     * @param public key that is used to encrypt
     * @return encrypted string
     */
    public static String PubEncrypt( String str, PublicKey publicKey ) throws Exception{

//        .decodeBase64(publicKey);
//        byte[] decoded = Base64.getDecoder().decode(publicKey);
//        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        // using RSA de encrypt
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
//        String outStr = Base64.encodeBase64String(cipher.doFinal(str.getBytes("UTF-8")));
        String outStr = Base64.getEncoder().encodeToString(cipher.doFinal(str.getBytes("UTF-8")));

        return outStr;
    }

    /*
     * use private key to decrypt the string
     * @param string that needs to be decrypted
     * @param private key that is used to decrypt
     * @return decrypted string
     */
    public static String PriDecrypt(String str, PrivateKey privateKey) throws Exception{
        // decode the encrypted string
        byte[] inputByte = Base64.getDecoder().decode(str.getBytes("UTF-8"));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        String outStr = new String(cipher.doFinal(inputByte));
        return outStr;
    }

}
