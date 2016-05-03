package enotes.smartcard.test;

import enotes.smartcard.CardCommunication;
import java.security.Security;
import java.util.Arrays;

public class CardCommunicationTest {
    public static void main(String[] args){
        if(CardCommunication.connectToCard())
            println("Conected");
        else
            println("Connecting failed");
        
        println("Trying to obtain secret key without veryfying PIN:");
        byte[] secKey0 = CardCommunication.getSecretKey();
        if(secKey0 != null)
            println("Something went terribly wrong");
        else
            println("No key, it's OK");
        
        println("Trying to change PIN without verification:");
        if(CardCommunication.changePIN(new byte[] {0,1,2,3}))
            println("This shouldn't happen");
        else
            println("Couldn't change it, it's OK");
        
        boolean PINverified = CardCommunication.verifyPIN(new byte [] {0,0,0,0});
        if(PINverified){
            System.out.println("PIN verified");
        }else{
            System.out.println("PIN not verified");
        }
        
        println("Obtaining secret key 2 times and comparing if they are equal:");
        byte[] secKey = CardCommunication.getSecretKey();
        byte[] secKey2 = CardCommunication.getSecretKey();
        if(Arrays.equals(secKey, secKey2))
            println("Great, they are equal!");
        else
            println("Damn, they are not equal");
        System.out.println("The size of the key is " + secKey.length + ", which is ");
        if(secKey.length == 16)
            println("good.");
        else
            println("not good!");
        
        
    }
    
    
    public static void println(String msg){
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