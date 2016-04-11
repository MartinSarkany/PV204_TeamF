package org.jpws.front.util;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.awt.font.TextHitInfo;
import java.text.AttributedCharacterIterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jpws.pwslib.global.Log;

public class PasswordField extends JComponent {

    private static final char echoChar = '*';
    
    private List<Character> clist = new Vector<Character>();
    private JLabel tf;
    
    public PasswordField() {
        super();
        tf = new JLabel();
        init();
    }

    public PasswordField (int columns) {
        this();
        tf = new JLabel();
        init();
    }
    
    private void init () {
        // attributes of Textfield
        tf.setForeground( Color.BLUE );
//        tf.setEditable(false);
        
        enableEvents(AWTEvent.KEY_EVENT_MASK | AWTEvent.INPUT_METHOD_EVENT_MASK);
        setLayout( new BorderLayout() );
        add( tf );
    }


    @Override
    protected void processInputMethodEvent(InputMethodEvent e) {

        if (!e.isConsumed()) {
            switch (e.getID()) {
            case InputMethodEvent.INPUT_METHOD_TEXT_CHANGED:
                AttributedCharacterIterator text = e.getText();
                char c = text.first();
                Log.debug(10, "(PasswordField.processInputMethodEvent) INPUT CHAR == " + c );
                
                // fall through

            case InputMethodEvent.CARET_POSITION_CHANGED:
                TextHitInfo caretPos = e.getCaret();
                int caretIndex = caretPos.getInsertionIndex();
                Log.debug(10, "(PasswordField.processInputMethodEvent) NEW CARET POS == " + caretIndex );
//                    setCaretPosition( caretIndex );
                break;
            }
        }
        e.consume();
        
        // TODO process input method
    }

    
    @Override
    protected void processComponentKeyEvent(KeyEvent e) {
        // TODO Auto-generated method stub
        super.processComponentKeyEvent(e);
        Log.debug(10, "(PasswordField.ComponentKeyEvent) INPUT KEY == " + e.getKeyChar() );
    }

    public char[] getPassword () {
       char[] res = new char[clist.size()];
       int i = 0;
       for ( char c : clist ) {
           res[i++] = c; 
       }
       return res;
    }
    
    public void destroyValue () {
        clist.clear();
        tf.setText(null);
    }

    public void setValue(char[] value) {
        clist.clear();
        for ( char c : value ) {
            clist.add( c );
        }
        tf.setText( Util.iterString( echoChar, value.length ));
    }
    
}
