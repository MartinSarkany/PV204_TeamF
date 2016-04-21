package enotes.smartcard;

import enotes.smartcard.applet.EnotesApplet;
import javacard.framework.Util;
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

    public boolean connectToCard()throws Exception{
        try {
            if( !cardManager.ConnectToCard()){
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
}
