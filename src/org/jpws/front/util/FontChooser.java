/*BEGIN_COPYRIGHT_BLOCK
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS WITH THE SOFTWARE.
 *
END_COPYRIGHT_BLOCK*/

package org.jpws.front.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * 
 * @since 0-5-0 subclass of ButtonBarDialog
 */
public class FontChooser extends ButtonBarDialog 
{
  /**
   * Available font styles.
   */
  private static final String[] STYLES =
      new String[] { "Plain", "Bold", "Italic", "Bold Italic" };

  /**
   * Available font sizes.
   */
  private static final String[] SIZES =
      new String[] { "3", "4", "5", "6", "7", "8", "9", "10", "11", "12",
                     "13", "14", "15", "16", "17", "18", "19", "20", "22",
                     "24", "27", "30", "34", "39", "45", "51", "60"};

  // Lists to display
  private NwList _styleList;
  private NwList _fontList;
  private NwList _sizeList;

  // Swing elements
  private JLabel _sampleText = new JLabel();

//  private boolean _clickedOK = false;

  /**
   * Constructs a new modal FontChooser for the given frame,
   * using the specified font.
   */
  private FontChooser ( Dialog parent, Font font ) 
  {
    super(parent, DialogButtonBar.OK_CANCEL_BUTTON, true);
    initAll( font );
  }

  //  private boolean _clickedOK = false;

  /**
   * Constructs a new modal FontChooser for the given frame,
   * using the specified font.
   */
  private FontChooser ( Frame parent, Font font ) 
  {
    super(parent, DialogButtonBar.OK_CANCEL_BUTTON, true);
    initAll( font );
  }

/**
   * Method used to show the font chooser, and select a new font.
   *
   * @param parent the parent component of the dialog to be shown (Frame or Dialog) 
   * @param title  The title for this window.
   * @param font   The previously chosen font.
   * 
   * @return the newly chosen font or <b>null</b> if dialog aborted
   */
  public static Font showDialog ( Component parent, String title, Font font ) 
  {
    FontChooser fd;
    
    fd = parent instanceof Frame ? new FontChooser( (Frame)parent, font )
                                 : new FontChooser( (Dialog)parent, font );
    fd.setTitle(title);
    fd.setVisible(true);
    Font chosenFont = null;

    if ( fd.isOkPressed() ) 
    {
      chosenFont = fd.getFont();
    }
    return (chosenFont);
  }

  /**
   * Shows the font chooser with a standard title ("Font Chooser") and 
   * the current VM default Font.
   * 
   * @param frame the parent frame
   * @param parent the component into which to center this window 
   */
  public static Font showDialog ( Component parent ) 
  {
    return showDialog( parent, "Font Chooser", null );
  }

  private void initAll ( Font font ) 
  {
     JPanel mainPanel, listPanel, panel;
     String fName;
    
    ((JPanel)getContentPane()).setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
    
    mainPanel = new JPanel( new BorderLayout() );
    
    // sample text label
    _sampleText = new JLabel();
    _sampleText.setForeground(Color.black);
    _sampleText.setBorder( BorderFactory.createEmptyBorder( 20, 0, 10, 0 ) );
    mainPanel.add(_sampleText, BorderLayout.SOUTH );

    // add lists
    _fontList = new NwList( GraphicsEnvironment.getLocalGraphicsEnvironment()
          .getAvailableFontFamilyNames() );
    _styleList = new NwList(STYLES);
    _sizeList = new NwList(SIZES);

    listPanel = new JPanel( new BorderLayout( 3, 3 ) );
    listPanel.setPreferredSize( new Dimension( 400, 250 ) );
    mainPanel.add( listPanel, BorderLayout.CENTER );

    panel = new JPanel( new BorderLayout( 3, 3 ) );
    listPanel.add( _fontList, BorderLayout.CENTER );
    panel.add( _styleList, BorderLayout.CENTER );
    panel.add( _sizeList, BorderLayout.EAST );
    listPanel.add( panel, BorderLayout.EAST );

    if (font == null) 
       font = _sampleText.getFont();
    
    fName = font.getFamily();
    if ( fName.equals( "dialog" ) )
       fName = "Dialog";
    _fontList.setSelectedItem( fName );
    _sizeList.setSelectedItem( String.valueOf( font.getSize() ));
    _styleList.setSelectedItem( STYLES[font.getStyle()] );

    setDialogPanel( mainPanel );
  }  // initAll

  private void showSample () {
    int g = 0;
    try {
      g = Integer.parseInt(_sizeList.getSelectedValue());
    }
    catch (NumberFormatException nfe) {
    }
    String st = _styleList.getSelectedValue();
    int s = Font.PLAIN;
    if (st.equalsIgnoreCase("Bold")) s = Font.BOLD;
    if (st.equalsIgnoreCase("Italic")) s = Font.ITALIC;
    if (st.equalsIgnoreCase("Bold Italic")) s = Font.BOLD | Font.ITALIC;
    _sampleText.setFont(new Font(_fontList.getSelectedValue(), s, g));
    _sampleText.setText("Sample Text to be used in this JPasswords Category");
    _sampleText.setVerticalAlignment(SwingConstants.TOP);
  }

  /**
   * Returns the currently selected Font.
   */
  public Font getFont () 
  {
    return _sampleText == null ? null : _sampleText.getFont();
  }

  /**
   * Private inner class for a list which displays something else in addition to a label
   * indicating the currently selected item.
   */
  public class NwList extends JPanel 
  {
    Border border = BorderFactory.createEmptyBorder( 1, 5, 1, 5 );  
    ListCellRenderer cellRenderer = new DefaultListCellRenderer()
    {
      /* 
       * Overridden: @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
       */
      public Component getListCellRendererComponent ( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
      {
         JLabel c;
         
         c = (JLabel)super.getListCellRendererComponent( list, value, index, isSelected,
               cellHasFocus );
         c.setBorder( border );
         return c;
      }
    };
    JList jlist;
    JScrollPane scroll;
    JLabel jt;
    String si = " ";

    public NwList(String[] values) 
    {
      setLayout( new BorderLayout( 0, 3 ) );

      jlist = new JList(values);
      jlist.setCellRenderer( cellRenderer );
//      jlist.setBorder( BorderFactory.createEmptyBorder( 0, 3, 0, 3 ) ); 
      scroll = new JScrollPane( jlist );
      
      jt = new JLabel();
      jt.setBackground(Color.white);
      jt.setForeground(Color.black);
      jt.setOpaque(true);
      jt.setBorder(new JTextField().getBorder());
      jt.setFont(getFont());
      
      jlist.addListSelectionListener(new ListSelectionListener() 
      {
        public void valueChanged(ListSelectionEvent e) 
        {
           String text = (String)jlist.getSelectedValue();
          jt.setText( text );
          si = text;
          showSample();
        }
      });
      
      add(scroll, BorderLayout.CENTER );
      add(jt, BorderLayout.NORTH );
    }

    public String getSelectedValue() 
    {
      return si;
    }

    public void setSelectedItem(String s)
    {
       jlist.setSelectedValue(s, false);
       jlist.ensureIndexIsVisible( Math.min( jlist.getSelectedIndex()+2, 
             jlist.getModel().getSize()-1 ) );
    }

  }
}
