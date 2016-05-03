package enotes.smartcard.test;

import enotes.smartcard.CardCommunication;
import java.security.Security;
import java.util.Arrays;

public class CardCommunicationTest {

    public static void main(String[] args) {
        changedPINVerification();

    }

    public static boolean connecting() {
        boolean connected = CardCommunication.connectToCard();
        if (connected) {
            println("Conected");
        } else {
            println("Connecting failed");
        }
        CardCommunication.disconnect();
        return connected;
    }

    public static boolean pinVerification() {
        CardCommunication.connectToCard();
        boolean PINverified = CardCommunication.verifyPIN(new byte[]{0, 0, 0, 0});
        if (PINverified) {
            System.out.println("PIN verified");
        } else {
            System.out.println("PIN not verified");
        }
        CardCommunication.disconnect();

        return PINverified;
    }

    public static boolean gettingKeyWithoutPINVerification() {
        CardCommunication.connectToCard();
        println("Trying to obtain secret key without veryfying PIN:");
        byte[] secKey = CardCommunication.getSecretKey();
        if (secKey != null) {
            println("Something went terribly wrong");
        } else {
            println("No key, it's OK");
        }
        CardCommunication.disconnect();

        return secKey == null;
    }

    public static boolean changePINwithoutPINverification() {
        CardCommunication.connectToCard();
        println("Trying to change PIN without verification:");
        boolean PINchanged = CardCommunication.changePIN(new byte[]{0, 1, 2, 3});
        if (PINchanged) {
            println("This shouldn't happen");
        } else {
            println("Couldn't change it, it's OK");
        }
        CardCommunication.disconnect();

        return PINchanged;
    }

    public static boolean getKey2TimesAndCompare() {
        CardCommunication.connectToCard();
        CardCommunication.verifyPIN(new byte[]{0, 0, 0, 0});
        println("Obtaining secret key 2 times and comparing if they are equal:");
        byte[] secKey = CardCommunication.getSecretKey();
        byte[] secKey2 = CardCommunication.getSecretKey();
        if (Arrays.equals(secKey, secKey2)) {
            println("Great, they are equal!");
        } else {
            println("Damn, they are not equal");
        }
        System.out.println("The size of the key is " + secKey.length + ", which is ");
        if (secKey.length == 16) {
            println("good.");
        } else {
            println("not good!");
        }
        CardCommunication.disconnect();

        return Arrays.equals(secKey, secKey2);
    }

    public static boolean changedPINVerification() {
        if(!CardCommunication.connectToCard())
            return false;
        byte[] oldPIN = new byte[]{0, 0, 0, 0};
        byte[] newPIN = new byte[]{1, 2, 3, 4};
        println("Verifying old pin:");
        boolean PINverified = CardCommunication.verifyPIN(oldPIN);
        if (!PINverified) {
            println("Couldn't verify old PIN");
            CardCommunication.disconnect();
            return false;
        } else {
            println("Old PIN verified");
        }
        println("Changing PIN:");
        boolean PINchanged = CardCommunication.changePIN(newPIN);
        if (!PINchanged) {
            println("Couldn't change PIN");
            CardCommunication.disconnect();
            return false;
        } else {
            println("PIN changed to 1234");
        }
        CardCommunication.disconnect();
        CardCommunication.connectToCard();
        println("Verifying new PIN:");
        PINverified = CardCommunication.verifyPIN(newPIN);
        if (!PINverified) {
            println("Couldn't verify new PIN");
            CardCommunication.disconnect();
            return false;
        } else {
            println("New PIN verified");
        }
        println("Changing PIN back to 0000:");
        PINchanged = CardCommunication.changePIN(oldPIN);
        if (!PINchanged) {
            println("Couldn't change PIN");
            CardCommunication.disconnect();
            return false;
        } else {
            println("PIN changed back to 0000");
        }

        CardCommunication.disconnect();
        return true;
    }

    public static void println(String msg) {
        System.out.println(msg);
    }
}

/*
1. connect + verify pin + change pin + verify pin
2. change pin (verify that fails)
3. verify pin + encrypt + decrypt (verify the same message as orignially encrypted)
4. encrypt (without verify)
5. decrypt (without verify)
 */
