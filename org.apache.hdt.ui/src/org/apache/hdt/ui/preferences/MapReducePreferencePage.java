/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hdt.ui.preferences;

import org.apache.hdt.core.AbstractHadoopHomeReader;
import org.apache.hdt.core.HadoopVersion;
import org.apache.hdt.ui.Activator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By sub-classing <tt>FieldEditorPreferencePage</tt>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * 
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class MapReducePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private StringFieldEditor hadoopHomeDirEditor;
	private ComboFieldEditor hadoopVersionEditor;
	private String hadoopVersionValue;
	private String hadoopHomeValue;

	public MapReducePreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setTitle("Hadoop Map/Reduce Tools");
		// setDescription("Hadoop Map/Reduce Preferences");
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	@Override
	public void createFieldEditors() {
		DirectoryFieldEditor editor = new DirectoryFieldEditor(PreferenceConstants.P_PATH, "&Hadoop installation directory:", getFieldEditorParent());
		addField(editor);
		HadoopVersion[] versions = HadoopVersion.values();
		String[][] values = new String[versions.length][2];
		int pos = 0;
		for (HadoopVersion ver : versions) {
			values[pos][0] = values[pos][1] = ver.getDisplayName();
			pos++;
		}
		ComboFieldEditor options = new ComboFieldEditor(PreferenceConstants.P_VERSION, "&Hadoop Version:", values, getFieldEditorParent());
		addField(options);
		hadoopVersionEditor = options;
		hadoopHomeDirEditor = editor;
		hadoopVersionValue = HadoopVersion.Version1.getDisplayName();
	}

	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		if (event.getSource().equals(hadoopVersionEditor)) {
			hadoopVersionValue = event.getNewValue().toString();
		}
		if (event.getSource().equals(hadoopHomeDirEditor)) {
			hadoopHomeValue = event.getNewValue().toString();
		}
		if (event.getProperty().equals(FieldEditor.VALUE)) {
			checkState();
		}
	}

	@Override
	protected void checkState() {
		super.checkState();
		if(hadoopHomeValue==null || hadoopVersionValue==null){
			setErrorMessage("Please set Hadoop Home/Version.");
			setValid(false);
			return;
		}
		AbstractHadoopHomeReader homeReader;
		try {
			homeReader = AbstractHadoopHomeReader.createReader(hadoopVersionValue);
			if (!homeReader.validateHadoopHome(new Path(hadoopHomeValue).toFile())) {
				setErrorMessage("Invalid Hadoop Home.");
				setValid(false);
			} else {
				setErrorMessage(null);
				setValid(true);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub

	}
}
