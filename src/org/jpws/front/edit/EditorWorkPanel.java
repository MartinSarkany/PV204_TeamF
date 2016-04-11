package org.jpws.front.edit;

import java.awt.BorderLayout;
import java.util.EventObject;

import javax.swing.JPanel;

import org.jpws.front.edit.EditorDialog.PanelType;
import org.jpws.front.util.ActivityListener;
import org.jpws.front.util.ActivitySource;

public abstract class EditorWorkPanel implements ActivitySource {

	private String name;
	private PanelType type;
	private JPanel viewPanel = new JPanel( new BorderLayout() );
	private boolean enabled = true;

/** Creates a new work panel with the given individual and type name.
 *  	
 * @param name String panel name
 * @param type String panel type name
 */
public EditorWorkPanel (String name, PanelType type) {
	if ( name == null || name.isEmpty() )
		throw new IllegalArgumentException("name is null or empty");
	if ( type == null )
		throw new IllegalArgumentException("type is null");

	setName(name);
	setType(type);
}

/** Sets the ENABLED state of this work panel.
 *  
 * @param enable boolean true == set panel enabled
 */
public void setEnabled ( boolean enabled ) {
	this.enabled = enabled;
}

/** Whether this work panel is in ENABLED state. This affects GUI status and
 * whether components are editable.
 * 
 * @return boolean true == enabled
 */
public boolean isEnabled () {
	return enabled; 
}

/** Reads all data from required for this panel from the editor's source.
 */
public abstract void readData ();

/** Writes editable data of this panel to the editor's sink. This may
 * be restricted to data which has been modified since last write or read.
 */
public abstract void writeData ();

/** Returns true if the editable data set of this panel is in a consistent
 * state.
 * 
 * @return boolean true == data consistent
 */
public boolean verify () {
	return true;
}

/** Returns the individual name of this work panel.
 * 
 * @return String name
 */
public String getName () {
	return name;
}

/** Returns the GUI view of this work panel. The default JPanel returned carries
 * the individual name of this work panel.
 *  
 * @return JPanel
 */
public JPanel getView () {
	return viewPanel;
}

/** Sets the JPanel which serves as view of this work panel. If the panel does 
 * not carry a individual name, this work panel's name is assigned to it.
 *  
 * @param view JPanel
 */
public void setView (JPanel view) {
	if ( view == null )
		throw new NullPointerException();
	
	if ( view.getName() == null ) {
		view.setName(getName());
	}
	viewPanel = view;
}

/** Returns the type name of this work panel.
 * 
 * @return String name
 */
public PanelType getType () {
	return type;
}

/** Set the individual name of this work panel.
 *  
 * @param name String
 */
public void setName ( String name ) {
	this.name = name;
	viewPanel.setName(name);
}

/** Set the type name of this work panel.
 *  
 * @param name String
 */
public void setType ( PanelType name ) {
	this.type = name;
}

/** Called to invoke termination of ongoing editing tasks and saving all
 * modified data of this panel to its sink.
 */
public void endEdit () {
}

/** Called after a panel was brought to display.
 */
public void panelShown () {
}

/** Called immediately before a panel is stepping into the background. Normally
 * after <code>endEdit()</code> was called. This method has the option to
 * veto the process of bringing the panel to background.
 * 
 * @return boolean true == ready, false == break this task (do not hide!) 
 */
public boolean panelHiding () {
	return true;
}

/** Called immediately before a panel is terminally closed. This method has the 
 * option to veto the process of closing the panel.
 * 
 * @return boolean true == ready, false == break this task (do not close!) 
 */
public boolean panelClosing () {
	return true;
}

/** Called to bring the named data field to editable display.
 * 
 * @param field String data field name
 */
public void displayValue (String field) {
}

/** Whether this panel is currently showing in the GUI. 
 * 
 * @return boolean true == panel showing
 */
public boolean isShowing() {
	return getView().isShowing();
}

/** Called to indicate a system wide editor event.
 *   
 * @param evt <code>EditorPanelEvent</code>
 */
public void eventOccurred ( EditorPanelEvent evt) {
}

/** Called to inform this panel about a modified field value. The given value
 * is the new value. The field name can be any name, not restricted to fitting
 * to this panel.
 * 
 * @param field String field name
 * @param value Object new field value
 */
public void notifyFieldValue (String field, Object value, EditorWorkPanel source) {
}

@Override
public void addActivityListener(ActivityListener listener) {
	// TODO Auto-generated method stub
	
}

@Override
public void removeActivityListener(ActivityListener listener) {
	// TODO Auto-generated method stub
	
}


// -------------- INNER CLASSES ---------------

public static class EditorPanelEvent extends EventObject {

	public EditorPanelEvent(Object source, int type, String field ) {
		super(source);
	}

	
}

}
