package org.jpws.front.edit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.KeyStroke;

import org.jpws.pwslib.data.ContextFile;
import org.jpws.pwslib.data.PwsFile;
import org.jpws.pwslib.data.PwsFileFactory;
import org.jpws.pwslib.data.PwsPassphrase;
import org.jpws.pwslib.data.PwsRecord;
import org.jpws.pwslib.data.PwsRecordList;
import org.jpws.pwslib.exception.DuplicateEntryException;
import org.jpws.pwslib.exception.PasswordSafeException;
import org.jpws.pwslib.global.Global;
import org.jpws.pwslib.global.Util;
import org.jpws.pwslib.order.DefaultRecordWrapper;
import org.jpws.pwslib.persist.ApplicationAdapter;
import org.jpws.pwslib.persist.ByteArrayOutputStreamPws;
import org.junit.Test;

public class TestC_Editor {

@Test	
public void test_init () {

	PwsRecord record = new PwsRecord();
	record.setTitle("Am Unterbrunnen");
	record.setPassword(new PwsPassphrase("igfdi23zgf"));
	record.setNotes("Erbringung von Genußdiensten für Privat- und Geschäftskunden");
//	PwsRecord origRec = (PwsRecord)record.clone();
	byte[] sig1 = record.getSignature();
	
	// create dialog for editing record in PWS3 format, no container
	EditorDialog dlg = new EditorDialog(null, record, EditorDialog.FormatVariant.pws3);

	// test initial state (unmodified)
	assertTrue("false format variant on dialog", dlg.getFormatVariant() == EditorDialog.FormatVariant.pws3);
	assertFalse("isNewRecord false (init)", dlg.isNewRecord());
	assertFalse("isRecordModified false (init)", dlg.isRecordModified());
	assertNull("getFileContainer false (init)", dlg.getFileContainer());
	
	PwsRecord result = dlg.getRecord();
	byte[] sig2 = result.getSignature();
	assertTrue("getRecord same as parameter (init)", record != result);
	assertTrue("getRecord has different UUID as parameter (init)", record.equals(result));
	assertTrue("creation has modified record", Util.equalArrays(sig1, sig2));
	assertNotNull("initRecord not available (init)", dlg.getInitRecord());
	sig2 = dlg.getInitRecord().getSignature();
	assertTrue("init-record has different UUID (init)", record.equals(dlg.getInitRecord()));
	assertTrue("false init-record value", Util.equalArrays(sig1, sig2));

	// create dialog for a new record in PWS3 format
	String initGroup = "Katzenbrenner";
	dlg = new EditorDialog(null, initGroup, EditorDialog.FormatVariant.pws3);
	
	assertTrue("false format variant on dialog", dlg.getFormatVariant() == EditorDialog.FormatVariant.pws3);
	assertTrue("isNewRecord false (init)", dlg.isNewRecord());
	assertFalse("isRecordModified false (init)", dlg.isRecordModified());
	assertNull("getFileContainer false (init)", dlg.getFileContainer());
	
	result = dlg.getRecord();
	assertNotNull("new record missing (init)", result);
	assertNotNull("initRecord not available (init)", dlg.getInitRecord());
	assertTrue("result record loses GROUP value", initGroup.equals(result.getGroup()));
	sig1 = result.getSignature();
	sig2 = dlg.getInitRecord().getSignature();
	assertTrue("new record / initRec divergence", Util.equalArrays(sig1, sig2));
	
}

}
