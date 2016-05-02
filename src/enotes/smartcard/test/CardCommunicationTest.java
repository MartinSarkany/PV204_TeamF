package enotes.smartcard.test;

import enotes.smartcard.CardCommunication;

public class CardCommunicationTest {
    public static void main(String[] args){
        CardCommunication cardcomm = new CardCommunication();
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
    }
    
    
    public static void println(String msg){
        System.out.println(msg);
    }
}
