package enotes.smartcard.applet;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;

public class EnotesApplet  extends javacard.framework.Applet
{
    // MAIN INSTRUCTION CLASS
    final static byte CLA_ENOTESAPPLET                = (byte) 0xB0;

    // INSTRUCTIONS
    final static byte INS_GEN_PUB_KEY_MOD            = (byte) 0x50;
    final static byte INS_RET_PUB_EXP                = (byte) 0x51;
    final static byte INS_GEN_SEC_KEY                = (byte) 0x52;
    final static byte INS_SET_MOD                    = (byte) 0x53;
    final static byte INS_SET_EXP_SEND_SEC_KEY       = (byte) 0x54;
    final static byte INS_VERIFYPIN                  = (byte) 0x55;
    final static byte INS_CHANGEPIN                  = (byte) 0x56;
    
    final static short ARRAY_LENGTH                  = (short) 0xff;
    
    final static short SW_CIPHER_DATA_LENGTH_BAD     = (short) 0x6710;
    final static short SW_BAD_PIN                    = (short) 0x6900;
    final static short SW_CONDITIONS_NOT_SATISFIED   = (short) 0x6985;
    final static short PIN_REQUIRED                  = (short) 0x6982;
    final static short WRONG_P1P2                    = (short) 0x6B00;

    private   AESKey         m_aesKey = null;
    private   RandomData     m_secureRandom = null;
    private   OwnerPIN       m_pin = null;
    private   KeyPair        m_keyPair = null;
    private   RSAPrivateKey  m_privateKey = null;
    private   RSAPublicKey   m_publicKey = null;
    private   Cipher         m_rsaCipher = null;

    // TEMPORARRY ARRAY IN RAM
    private byte  m_ramArray[] = null;
    
    //indicates whether secret key was generated
    private boolean m_secretKeyIsSet = false;

    protected EnotesApplet(byte[] buffer, short offset, byte length) 
    {
        if(length > 9) {
            
            // TEMPORARY BUFFER USED FOR FAST OPERATION WITH MEMORY LOCATED IN RAM
            m_ramArray = JCSystem.makeTransientByteArray((short) 260, JCSystem.CLEAR_ON_DESELECT);
            Util.arrayFillNonAtomic(m_ramArray, (short) 0, ARRAY_LENGTH, (byte) 0);
            
            // CREATE AES KEY OBJECT
            m_aesKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
            
            // CREATE RANDOM DATA GENERATORS
            m_secureRandom = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);

            m_pin = new OwnerPIN((byte) 5, (byte) 4);
            m_pin.update(m_ramArray, (byte) 0, (byte) 4);

            // CREATE RSA KEYS AND PAIR
            m_keyPair = new KeyPair(KeyPair.ALG_RSA_CRT, KeyBuilder.LENGTH_RSA_1024);
            m_rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
           
        }

        register();
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) throws ISOException 
    {
        new EnotesApplet (bArray, bOffset, bLength);
    }

    public boolean select()
    {
        m_pin.reset();
        return true;
    }

    public void deselect() 
    {
        m_pin.reset();
    }
  
    public void process(APDU apdu) throws ISOException 
    {
        
        byte[] apduBuffer = apdu.getBuffer();
        
        // ignore the applet select command dispached to the process
        if (selectingApplet())
            return;

        // APDU instruction parser
        if (apduBuffer[ISO7816.OFFSET_CLA] == CLA_ENOTESAPPLET) {
            switch ( apduBuffer[ISO7816.OFFSET_INS] )
            {
                case INS_GEN_PUB_KEY_MOD: genKeypairAndReturnModulus(apdu); break;
                case INS_RET_PUB_EXP: returnPubExponent(apdu); break;
                case INS_GEN_SEC_KEY: generateSecretKey(); break;
                case INS_SET_MOD: setModulus(apdu); break;
                case INS_SET_EXP_SEND_SEC_KEY: setExponentAndReturnEncryptedKey(apdu); break;
                case INS_CHANGEPIN: changePIN(apdu); break;
                case INS_VERIFYPIN: verifyPIN(apdu); break;
                default :
                    // The INS code is not supported by the dispatcher
                    ISOException.throwIt( ISO7816.SW_INS_NOT_SUPPORTED ) ;
                break ;
            }
        }
        else ISOException.throwIt( ISO7816.SW_CLA_NOT_SUPPORTED);
    }

    void verifyPIN(APDU apdu) 
    {
        byte[]    apdubuf = apdu.getBuffer();
        short     dataLen = apdu.setIncomingAndReceive();     
        
        decryptPIN(apdubuf, dataLen);
        
        if(m_pin.getTriesRemaining() == 0)
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);

        if (m_pin.check(m_ramArray, (byte) 0, (byte) 4) == false)
            ISOException.throwIt(SW_BAD_PIN);
    }

    void changePIN(APDU apdu) 
    {
        byte[]    apdubuf = apdu.getBuffer();
        short     dataLen = apdu.setIncomingAndReceive();

        decryptPIN(apdubuf, dataLen);
        
        if(!m_pin.isValidated())
            ISOException.throwIt(PIN_REQUIRED);
        m_pin.update(apdubuf, ISO7816.OFFSET_CDATA, (byte) dataLen);
    }
    
    void genKeypairAndReturnModulus(APDU apdu)
    {
        m_keyPair.genKeyPair();
        m_privateKey = (RSAPrivateKey)m_keyPair.getPrivate();
        /*m_sign = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
        m_sign.init(m_privateKey, Signature.MODE_SIGN);*/
        
        m_publicKey = (RSAPublicKey)m_keyPair.getPublic();
        m_publicKey.getModulus(apdu.getBuffer(), ISO7816.OFFSET_CDATA);
        
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (short) 128);
    }
    
    void returnPubExponent(APDU apdu){
        m_publicKey.getExponent(apdu.getBuffer(), ISO7816.OFFSET_CDATA);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (short) 128);
    }
    
    void setModulus(APDU apdu)
    {
        byte[] apdubuf = apdu.getBuffer();        
        short  dataLen = apdu.setIncomingAndReceive();
        
        if(!m_pin.isValidated())
            ISOException.throwIt(PIN_REQUIRED);
        
        //if not the right length
        if(dataLen != 128)
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        
        m_publicKey.setExponent(apdubuf, ISO7816.OFFSET_CDATA, dataLen);        
    }
    
    void setExponentAndReturnEncryptedKey(APDU apdu)
    {
        byte[] apdubuf = apdu.getBuffer();        
        short  dataLen = apdu.setIncomingAndReceive();
        short encryptedLen;
        
        if(!m_pin.isValidated())
            ISOException.throwIt(PIN_REQUIRED);
        
        //if modulus is not set
        if(m_publicKey.getModulus(m_ramArray,(short) 0) == 0)
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        
        //if not the right length
        if(dataLen != 128)
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        
        //set public key exponent (modulus is already set)
        m_publicKey.setExponent(apdubuf, ISO7816.OFFSET_CDATA, dataLen);
        //init cipher object with public key
        m_rsaCipher.init(m_publicKey, Cipher.MODE_ENCRYPT);
        
        if(!m_secretKeyIsSet){
            generateSecretKey();
        }
        //copy secret key from AES object to RAM array
        m_aesKey.getKey(m_ramArray, (short) 0);
        //encrypt secret key by public key, store result in APDU buffer
        encryptedLen = m_rsaCipher.doFinal(m_ramArray, (short) 0, (short)128, apdubuf, ISO7816.OFFSET_CDATA);
        
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, encryptedLen);        
    }
    
    
    /*void encryptSecretKey(APDU apdu)
    {
        byte[]    apdubuf = apdu.getBuffer();
        short     dataLen = apdu.setIncomingAndReceive();
        //extract pub mod
        //extract pub exp
        
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, dataLen);
    }*/

    void decryptPIN(byte[] apdubuf, short dataLen)
    {
        m_rsaCipher.init(m_privateKey, Cipher.MODE_DECRYPT);
        m_rsaCipher.doFinal(apdubuf, ISO7816.OFFSET_CDATA, dataLen, m_ramArray, (short) 0);
        m_privateKey.clearKey();
        m_publicKey.clearKey();
    }
    
    void generateSecretKey(){
        if(m_secretKeyIsSet)
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        if(!m_pin.isValidated())
            ISOException.throwIt(PIN_REQUIRED);
                
        //generate crypt. secure random data
        m_secureRandom.generateData(m_ramArray, (short) 0, KeyBuilder.LENGTH_AES_128);
        m_aesKey.setKey(m_ramArray, (short) 0);
        m_secretKeyIsSet = true;
    }
    
  
}
