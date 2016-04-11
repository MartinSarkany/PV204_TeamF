package org.jpws.front.util;

/* From http://java.sun.com/docs/books/tutorial/index.html */

/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any
 * nuclear facility.
 */
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import org.jpws.pwslib.global.Log;


public class AutoTextField extends JTextField {
   private AutoComboBox autoComboBox;
   private List<Object> dataList;
   private boolean isCaseSensitive;
   private boolean isStrict;
   private boolean isSetup;

  class AutoDocument extends PlainDocument {

    @Override
	public void replace(int i, int j, String s, AttributeSet attributeset)
        throws BadLocationException {
      super.remove(i, j);
      insertString(i, s, attributeset);
    }

    @Override
	public void insertString(int i, String s, AttributeSet attributeset)
        throws BadLocationException {
       if ( isSetup )
          super.insertString(i, s, attributeset);
       else
       {
         if (s == null || "".equals(s))
           return;
         String s1 = getText(0, i);
         String s2 = getMatch(s1 + s);
         int j = (i + s.length()) - 1;
         if (isStrict && s2 == null) {
           s2 = getMatch(s1);
           j--;
         } else if (!isStrict && s2 == null) {
           super.insertString(i, s, attributeset);
           return;
         }
         if (autoComboBox != null && s2 != null)
           autoComboBox.setSelectedValue(s2);
         super.remove(0, getLength());
         super.insertString(0, s2, attributeset);
         setSelectionStart(j + 1);
         setSelectionEnd(getLength());
       }
    }
    
    @Override
	public void remove(int i, int j) throws BadLocationException {
       if ( !isStrict | isSetup )
          super.remove(i, j);
       else
       {
          int k = getSelectionStart();
          if (k > 0)
            k--;
          String s = getMatch(getText(0, k));
          if ( s != null )
          {
             super.remove(0, getLength());
             super.insertString(0, s, null);
             if (autoComboBox != null)
                autoComboBox.setSelectedValue(s);
          }
          try {
            setSelectionStart(k);
            setSelectionEnd(getLength());
          } catch (Exception exception) {
          }
       }
     }
/*
    public void remove(int i, int j) throws BadLocationException {
      int k = getSelectionStart();
      if (k > 0)
        k--;
      String s = getMatch(getText(0, k));
      if ( i == 0 || (!isStrict & s == null) ) {
        super.remove(i, j);
      } else {
        super.remove(0, getLength());
        super.insertString(0, s, null);
      }
      if (autoComboBox != null && s != null)
        autoComboBox.setSelectedValue(s);
      try {
        setSelectionStart(k);
        setSelectionEnd(getLength());
      } catch (Exception exception) {
      }
    }
*/
  }

  public AutoTextField(List<Object> list) {
    setDataList( list );
    init();
  }

  public AutoTextField(Object[] list) {
     setDataList( list );
     init();
   }

  AutoTextField(List<Object> list, AutoComboBox b) {
    setDataList( list );
    autoComboBox = b;
    init();
  }

  private void init() {
    setDocument(new AutoDocument());
//    if (isStrict && dataList.size() > 0)
//      setText(dataList.get(0).toString());
  }

  private String getMatch(String s) {
    for (int i = 0; i < dataList.size(); i++) {
      String s1 = dataList.get(i).toString();
      if (s1 != null) {
        if (!isCaseSensitive
            && s1.toLowerCase().startsWith(s.toLowerCase()))
          return s1;
        if (isCaseSensitive && s1.startsWith(s))
          return s1;
      }
    }

    return null;
  }

  @Override
  public void replaceSelection(String s) {
    AutoDocument _lb = (AutoDocument) getDocument();
    if (_lb != null)
      try {
        int i = Math.min(getCaret().getDot(), getCaret().getMark());
        int j = Math.max(getCaret().getDot(), getCaret().getMark());
        _lb.replace(i, j - i, s, null);
      } catch (Exception exception) {
      }
  }

  
  @Override
  public synchronized void setText ( String t )
  {
     boolean oldSetup = isSetup;;
     isSetup = true;
     super.setText( t );
     isSetup = oldSetup;
  }

  public boolean isCaseSensitive() {
    return isCaseSensitive;
  }

  public void setCaseSensitive(boolean flag) {
    isCaseSensitive = flag;
  }

  public boolean isStrict() {
    return isStrict;
  }

  public void setStrict(boolean flag) {
    isStrict = flag;
  }

  public List<Object> getDataList() {
    return dataList;
  }

  public void setDataList (List<Object> list) {
    if (list == null)
      throw new IllegalArgumentException("values can not be null");
    
    dataList = list;
    
    StringBuffer b = new StringBuffer(list.size());
    for ( Object obj : list )
       { b.append( obj ); b.append(';'); }
    Log.debug( 9, "(AutoTextField.setDataList) set list of values: " + b);
  }

  public void setDataList ( Object[] list) {
     if (list == null)
       throw new IllegalArgumentException("values can not be null");
       
     List<Object> dList = new ArrayList<Object>();
     for ( Object object : list )
        dList.add( object );
     
     setDataList( dList );
   }
  
}

