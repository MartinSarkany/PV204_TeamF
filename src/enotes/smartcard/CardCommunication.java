package enotes.smartcard;

import enotes.smartcard.applet.EnotesApplet;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.RSAPublicKeySpec;
import javacard.framework.Util;
import javacard.security.PublicKey;
import javacard.security.RSAPublicKey;
import javax.crypto.Cipher;
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
        (byte) 0x6c, (byte) 0x65, (byte) 0x74};

    public boolean connectToCard() {
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

    public boolean generateSecretKey() {
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
    private PublicKey generateKeyPair() {

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
            response = cardManager.sendAPDU(apdu).getBytes();
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
    
    public boolean verifyPIN(byte pin[]) {
        // todo: get public key, encrypt PIN, send to card for verification
        
        PublicKey publicKey = generateKeyPair();
        byte encryptedPin[];
        if (publicKey == null) {
            // For debugging
            System.out.println("Public key is null");
            return false;
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, (Key) publicKey);
            
            encryptedPin = cipher.doFinal(pin);
        } catch (Exception ex) {
            // For debugging print out exception
            System.out.println("Exception: " + ex.getMessage());
            return false;
        }
        
        byte apdu[] = new byte[CardMngr.HEADER_LENGTH + encryptedPin.length];
        apdu[CardMngr.OFFSET_CLA] = (byte) 0xB0;
        apdu[CardMngr.OFFSET_INS] = INS_VERIFYPIN;
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
}
