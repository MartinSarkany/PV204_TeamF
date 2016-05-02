package enotes.smartcard;

import enotes.smartcard.applet.EnotesApplet;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.spec.RSAPublicKeySpec;
import javacard.framework.Util;         //javacard?
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.PublicKey;     //javacard?
import javacard.security.RSAPublicKey;  //javacard?
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.smartcardio.ResponseAPDU;

/**
 *
 * @author aoeiko
 */
public class CardCommunication {

    static CardMngr cardManager = new CardMngr();

    // INSTRUCTIONS
    final static byte INS_GEN_PUB_KEY_MOD = (byte) 0x50;
    final static byte INS_RET_PUB_EXP = (byte) 0x51;
    final static byte INS_GEN_SEC_KEY = (byte) 0x52;
    final static byte INS_SET_MOD = (byte) 0x53;
    final static byte INS_SET_EXP_SEND_SEC_KEY = (byte) 0x54;
    final static byte INS_VERIFYPIN = (byte) 0x55;
    final static byte INS_CHANGEPIN = (byte) 0x56;

    private static byte SELECT_ENOTESAPPLET[] = {(byte) 0x00, (byte) 0xa4, (byte) 0x04, (byte) 0x00, (byte) 0x0b,
        (byte) 0x65, (byte) 0x6e, (byte) 0x6f, (byte) 0x74, (byte) 0x65,
        (byte) 0x73, (byte) 0x20, (byte) 0x61, (byte) 0x70, (byte) 0x70,
        (byte) 0x6c/*, (byte) 0x65, (byte) 0x74*/};

    public static boolean connectToCard() {
        try {
            if (!cardManager.ConnectToCard()) {
                return false;
            }
            cardManager.sendAPDU(SELECT_ENOTESAPPLET);
        } catch (Exception ex) {
            // For debugging print out exception
            System.out.println("Exception: " + ex.getMessage());
            return false;
        }

        return true;
    }

    public static boolean generateSecretKey() {
        byte apdu[] = new byte[CardMngr.HEADER_LENGTH];
        apdu[CardMngr.OFFSET_CLA] = (byte) 0xB0;
        apdu[CardMngr.OFFSET_INS] = INS_GEN_SEC_KEY;
        apdu[CardMngr.OFFSET_P1] = (byte) 0x00;
        apdu[CardMngr.OFFSET_P2] = (byte) 0x00;
        apdu[CardMngr.OFFSET_LC] = (byte) 0;

        byte response[];
        try {
            response = cardManager.sendAPDU(apdu).getBytes();
            if (response[0] != (byte) 0x90 || response[1] != (byte) 0x00) {
                return false;
            }
        } catch (Exception ex) {
            // For debugging print out exception
            System.out.println("Exception: " + ex.getMessage());
            return false;
        }
        return true;
    }

    // Generates a keypair on the card and returns the public key
    private static PublicKey generateKeyPair() {

        byte apdu[] = new byte[CardMngr.HEADER_LENGTH];
        apdu[CardMngr.OFFSET_CLA] = (byte) 0xB0;
        apdu[CardMngr.OFFSET_INS] = INS_GEN_PUB_KEY_MOD;
        apdu[CardMngr.OFFSET_P1] = (byte) 0x00;
        apdu[CardMngr.OFFSET_P2] = (byte) 0x00;
        apdu[CardMngr.OFFSET_LC] = (byte) 0;

        byte response[];
        byte modulus[];
        byte exponent[];
        PublicKey publicKey;
        try {
            ResponseAPDU respAPDU = cardManager.sendAPDU(apdu);
            response = respAPDU.getBytes();
            if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != (byte) 0x00) {
                return null;
            }
            modulus = new byte[response.length - 2];
            System.arraycopy(response, 0, modulus, 0, response.length - 2);

            apdu[CardMngr.OFFSET_INS] = INS_RET_PUB_EXP;
            response = cardManager.sendAPDU(apdu).getBytes();
            if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != (byte) 0x00) {
                return null;
            }

            exponent = new byte[response.length - 2];
            System.arraycopy(response, 0, exponent, 0, response.length - 2);

            // set modulus and exponent to new public key object
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(new BigInteger(modulus), new BigInteger(exponent)); 
            publicKey = (PublicKey) keyFactory.generatePublic(pubKeySpec);
        } catch (Exception ex) {
            // For debugging print out exception
            System.out.println("Exception: " + ex.getMessage());
            return null;
        }
        return publicKey;
    }
    
//    public static boolean verifyPIN(byte pin[]) {
//        // todo: get public key, encrypt PIN, send to card for verification
//        
//        PublicKey publicKey = generateKeyPair();
//        byte encryptedPin[];
//        if (publicKey == null) {
//            // For debugging
//            System.out.println("Public key is null");
//            return false;
//        }
//        try {
//            Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
//            cipher.init(Cipher.ENCRYPT_MODE, (Key) publicKey);
//            
//            encryptedPin = cipher.doFinal(pin);
//        } catch (Exception ex) {
//            // For debugging print out exception
//            System.out.println("Exception: " + ex.getMessage());
//            return false;
//        }
//        
//        byte apdu[] = new byte[CardMngr.HEADER_LENGTH + encryptedPin.length];
//        apdu[CardMngr.OFFSET_CLA] = (byte) 0xB0;
//        apdu[CardMngr.OFFSET_INS] = INS_VERIFYPIN;
//        apdu[CardMngr.OFFSET_P1] = (byte) 0x00;
//        apdu[CardMngr.OFFSET_P2] = (byte) 0x00;
//        apdu[CardMngr.OFFSET_LC] = (byte) encryptedPin.length;
//
//        System.arraycopy(pin, 0, apdu, CardMngr.OFFSET_DATA, encryptedPin.length);
//        byte response[];
//        try {
//            response = cardManager.sendAPDU(apdu).getBytes();
//            if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != (byte) 0x00) {
//                return false;
//            }
//        } catch (Exception ex) {
//            // For debugging print out exception
//            System.out.println("Exception: " + ex.getMessage());
//            return false;
//        } 
//        return true;
//    }
        
    
    //whatToDo: INS_VERIFYPIN to verify PIN or INS_CHANGEPIN to change PIN
    public static boolean doStuffWithPIN(byte pin[], byte whatToDo) {
        // todo: get public key, encrypt PIN, send to card for verification
        
        PublicKey publicKey = generateKeyPair();
        byte encryptedPin[];
        if (publicKey == null) {
            // For debugging
            System.out.println("Public key is null");
            return false;
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"/*, "BC"*/);  //not sure about BC here - bouncy castle? do we use this?
            cipher.init(Cipher.ENCRYPT_MODE, (Key) publicKey);
            
            encryptedPin = cipher.doFinal(pin);
        } catch (Exception ex) {
            // For debugging print out exception
            System.out.println("Exception: " + ex.getMessage());
            return false;
        }
        
        byte apdu[] = new byte[CardMngr.HEADER_LENGTH + encryptedPin.length];
        apdu[CardMngr.OFFSET_CLA] = (byte) 0xB0;
        apdu[CardMngr.OFFSET_INS] = whatToDo;
        apdu[CardMngr.OFFSET_P1] = (byte) 0x00;
        apdu[CardMngr.OFFSET_P2] = (byte) 0x00;
        apdu[CardMngr.OFFSET_LC] = (byte) encryptedPin.length;

        System.arraycopy(pin, 0, apdu, CardMngr.OFFSET_DATA, encryptedPin.length);
        byte response[];
        try {
            response = cardManager.sendAPDU(apdu).getBytes();
            if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != (byte) 0x00) {
                return false;
            }
        } catch (Exception ex) {
            // For debugging print out exception
            System.out.println("Exception: " + ex.getMessage());
            return false;
        } 
        return true;
    }
    
     public static boolean changePIN(byte pin[]) {
         return doStuffWithPIN(pin, INS_CHANGEPIN);
     }
     
    //use this instead of original verifyPIN to reduce code 
    public static boolean verifyPIN(byte pin[]) {
        return doStuffWithPIN(pin, INS_VERIFYPIN);
    }
  
    public static byte[] getSecretKey(){
        KeyPairGenerator keyGen;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException ex) {
            // For debugging print out exception
            System.out.println("Exception: " + ex.getMessage());
            return null;
        }
        keyGen.initialize(128);
        KeyPair keyPair = keyGen.genKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        byte[] modBytes = new byte[1024];
        publicKey.getModulus(modBytes, (short) 0);
        
        byte apdu[] = new byte[CardMngr.HEADER_LENGTH + modBytes.length];
        apdu[CardMngr.OFFSET_CLA] = (byte) 0xB0;
        apdu[CardMngr.OFFSET_INS] = INS_SET_MOD;
        apdu[CardMngr.OFFSET_P1] = (byte) 0x00;
        apdu[CardMngr.OFFSET_P2] = (byte) 0x00;
        apdu[CardMngr.OFFSET_LC] = (byte) modBytes.length;

        System.arraycopy(modBytes, 0, apdu, CardMngr.OFFSET_DATA, modBytes.length);
        byte response[];
        try {
            response = cardManager.sendAPDU(apdu).getBytes();
            if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != (byte) 0x00) {
                return null;
            }
        } catch (Exception ex) {
            // For debugging print out exception
            System.out.println("Exception: " + ex.getMessage());
            return null;
        }
        
        
        byte expBytes[] = new byte[128];
        apdu[CardMngr.OFFSET_INS] = INS_SET_EXP_SEND_SEC_KEY;
        apdu[CardMngr.OFFSET_LC] = (byte) expBytes.length;
        System.arraycopy(expBytes, 0, apdu, CardMngr.OFFSET_DATA, expBytes.length);
        try {
            response = cardManager.sendAPDU(apdu).getBytes();
            if (response[response.length - 2] != (byte) 0x90 || response[response.length - 1] != (byte) 0x00) {
                return null;
            }
        } catch (Exception ex) {
            // For debugging print out exception
            System.out.println("Exception: " + ex.getMessage());
            return null;
        }
        
        byte[] encryptedSecretKey = new byte[128];
        PrivateKey privateKey = keyPair.getPrivate();
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
        } catch (NoSuchAlgorithmException ex) {
            // For debugging print out exception
            System.out.println(ex.getMessage());
            return null;
        } catch (NoSuchPaddingException ex) {
            // For debugging print out exception
            System.out.println(ex.getMessage());
            return null;
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, (Key) privateKey);
        } catch (InvalidKeyException ex) {
            // For debugging print out exception
            System.out.println(ex.getMessage());
            return null;
        }
        
        byte[] secretKey;
        try {
            secretKey = cipher.doFinal(encryptedSecretKey);
        } catch (IllegalBlockSizeException ex) {
            // For debugging print out exception
            System.out.println(ex.getMessage());
            return null;
        } catch (BadPaddingException ex) {
            // For debugging print out exception
            System.out.println(ex.getMessage());
            return null;
        }
        
        return secretKey;       
    }
}
