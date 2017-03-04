package cryptography;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;

public class RSA {

    private String clearText;
    private byte[] encrypted;
    private String path;

    //initialize to pair apo to kleidia poy tha paraxthoun private , public
    private KeyPairGenerator KeyPairGen;
    //pairnw enan tixaio secure number
    private SecureRandom random;

    //initialize key pair generator
    KeyPair keypair;

    KeyPair loadedKeyPair;

    //pairnoyme ena public key
    PublicKey publicKey;
    //pairnoyme ena private key
    PrivateKey privateKey;

    //Initialize to cipher
    Cipher rsaEncrypt;

    public RSA() {
    }

    public RSA(String dText) {
        clearText = dText;
        encrypt();
    }
    
    
   
    public String getDecrypted() {
        decrypt();
        return clearText;
    }
              
    public void encrypt() {
        try {
            initialize(1);
            
            //dialegoume algorithmo ilopoihshs me private key
            rsaEncrypt.init(Cipher.ENCRYPT_MODE, privateKey);
            //encrypt
            encrypted = rsaEncrypt.doFinal(new String(clearText).getBytes());
            
            FileOutputStream fos = new FileOutputStream(path + "./key.dll");
            fos.write(encrypted);
            fos.close();
            
        } catch (Exception e) {

        }
    }

    public void decrypt() {
        try {
            initialize(2);
            
            File filePassword = new File("./key.dll");
            FileInputStream input = new FileInputStream(filePassword);
            encrypted=new byte[(int) filePassword.length()];
            input.read(encrypted);
            input.close();

            //decrypt
            rsaEncrypt.init(Cipher.DECRYPT_MODE, publicKey);
            //print ton algorithmo o opoios pali tha einai rsa
            //pairnw to encrypted byte array kai to apokriptografw
            
            byte bytesDecrypted[]=rsaEncrypt.doFinal(encrypted);
            
            StringBuilder b = new StringBuilder();
            
            for ( byte a : bytesDecrypted) {
                b.append((char)a); 
            }

            clearText = b.toString();
            
        } catch (Exception e) {

        }
    }

    public void initialize(int i) {
        try {
            path = "./";
            //initialize to pair apo to kleidia poy tha paraxthoun private , public
            KeyPairGen = KeyPairGenerator.getInstance("RSA");
            //pairnw enan tixaio secure number
            random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            //initialie to mikos twn kleidiwn 512 - 15384
            KeyPairGen.initialize(1024, random);

            //initialize key pair generator
            keypair = KeyPairGen.generateKeyPair();
            //pairnoyme ena public key

            if (i == 1) {
                SaveKeyPair(path, keypair);
            } else {
                loadedKeyPair = LoadKeyPair(path, "RSA");
            }
            //Initialize to cipher
            rsaEncrypt = Cipher.getInstance("RSA");

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void SaveKeyPair(String path, KeyPair keyPair) throws IOException {

        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();

        // Store Public Key.
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(
                publicKey.getEncoded());
        FileOutputStream fos = new FileOutputStream(path + "/public.key");
        fos.write(x509EncodedKeySpec.getEncoded());
        fos.close();

        // Store Private Key.
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
                privateKey.getEncoded());
        fos = new FileOutputStream(path + "/private.key");
        fos.write(pkcs8EncodedKeySpec.getEncoded());
        fos.close();
    }

    public KeyPair LoadKeyPair(String path, String algorithm)
            throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException {
        // Read Public Key.
        File filePublicKey = new File(path + "/public.key");
        FileInputStream fis = new FileInputStream(path + "/public.key");
        byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
        fis.read(encodedPublicKey);
        fis.close();

        // Read Private Key.
        File filePrivateKey = new File(path + "/private.key");
        fis = new FileInputStream(path + "/private.key");
        byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
        fis.read(encodedPrivateKey);
        fis.close();

        // Generate KeyPair.
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
                encodedPublicKey);
        publicKey = keyFactory.generatePublic(publicKeySpec);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
                encodedPrivateKey);
        privateKey = keyFactory.generatePrivate(privateKeySpec);

        return new KeyPair(publicKey, privateKey);
    }
}
