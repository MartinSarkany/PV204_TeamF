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
        boolean PINverified = CardCommunication.verifyPIN(new byte [] {0,0,0,0});
        if(PINverified){
            System.out.println("PIN verified");
        }else{
            System.out.println("PIN not verified");
        }
        
        println("Obtain secret key 2 times and compare if they are equal:");
        byte[] secKey = CardCommunication.getSecretKey();
        byte[] secKey2 = CardCommunication.getSecretKey();
        if(Arrays.equals(secKey, secKey2))
            println("Great, they are equal!");
        else
            println("Damn, they are not equal");
        
        
    }
    
    
    public static void println(String msg){
        System.out.println(msg);
    }
}
