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

package org.apache.hdt.ui.internal.launch;

import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.event.ChangeEvent;

import org.apache.hdt.core.HadoopVersion;
import org.apache.hdt.core.internal.hdfs.HDFSManager;
import org.apache.hdt.core.launch.AbstractHadoopCluster;
import org.apache.hdt.core.launch.AbstractHadoopCluster.ChangeListener;
import org.apache.hdt.core.launch.AbstractHadoopCluster.HadoopConfigurationBuilder;
import org.apache.hdt.core.launch.ConfProp;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * Wizard for editing the settings of a Hadoop location
 * 
 * The wizard contains 3 tabs: General, Tunneling and Advanced. It edits
 * parameters of the location member which either a new location or a copy of an
 * existing registered location.
 */

public class HadoopLocationWizard extends WizardPage {
	
	Image circle;

	/**
	 * The location effectively edited by the wizard. This location is a copy or
	 * a new one.
	 */
	private AbstractHadoopCluster location;

	/**
	 * The original location being edited by the wizard (null if we create a new
	 * instance).
	 */
	private AbstractHadoopCluster original;
	private Text locationName;
	private Combo hadoopVersion;

	/**
	 * New Hadoop location wizard
	 */
	public HadoopLocationWizard() {
		super("Hadoop Server", "New Hadoop Location", null);

		this.original = null;
		try {
			this.location = AbstractHadoopCluster.createCluster(HadoopVersion.Version1.getDisplayName());
		} catch (CoreException e) {
			e.printStackTrace();
		}
		this.location.setLocationName("");
	}

	/**
	 * Constructor to edit the parameters of an existing Hadoop server
	 * 
	 * @param server
	 */
	public HadoopLocationWizard(AbstractHadoopCluster server) {
		super("Create a new Hadoop location", "Edit Hadoop Location", null);
		this.original = server;
		try {
			this.location = AbstractHadoopCluster.createCluster(server.getVersion().getDisplayName());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Performs any actions appropriate in response to the user having pressed
	 * the Finish button, or refuse if finishing now is not permitted.
	 * 
	 * @return the created or updated Hadoop location
	 */

	public AbstractHadoopCluster performFinish() {
		try {
			if (this.original == null) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						HDFSManager.addServer(location.getLocationName(),
								location.getConfPropValue(ConfProp.FS_DEFAULT_URI), location
								.getConfPropValue(ConfProp.PI_USER_NAME), null,location.getVersion().getDisplayName());
					}
				});
				// New location
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
							ServerRegistry.getInstance().addServer(HadoopLocationWizard.this.location);
					}
				});
				return this.location;

			} else {
				
				// Update location
				final String originalName = this.original.getLocationName();
				final String originalLoc = this.original.getConfPropValue(ConfProp.FS_DEFAULT_URI);
				final String newName = this.location.getLocationName();
				final String newLoc = this.location.getConfPropValue(ConfProp.FS_DEFAULT_URI);
				
				if (!originalName.equals(newName) || !originalLoc.equals(newLoc)){
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					final IProject project = root.getProject(originalName);
					
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							if(project.exists()){
								try {
									project.close(null);
									project.delete(true, null);
								} catch (CoreException e) {
									e.printStackTrace();
								}
							}
							HDFSManager.addServer(location.getLocationName(),
									location.getConfPropValue(ConfProp.FS_DEFAULT_URI), location
									.getConfPropValue(ConfProp.PI_USER_NAME), null,location.getVersion().getDisplayName());
						}
					});
				}
				this.original.load(this.location);

				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						ServerRegistry.getInstance().updateServer(originalName, HadoopLocationWizard.this.location);
					}
				});
				return this.original;

			}
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
			setMessage("Invalid server location values", IMessageProvider.ERROR);
			return null;
		}
	}

	/**
	 * Validates the current Hadoop location settings (look for Hadoop
	 * installation directory).
	 * 
	 */
	private void testLocation() {
		setMessage("Not implemented yet", IMessageProvider.WARNING);
	}

	/**
	 * Location is not complete (and finish button not available) until a host
	 * name is specified.
	 * 
	 * @inheritDoc
	 */
	@Override
	public boolean isPageComplete() {

		{
			String locName = location.getConfPropValue(ConfProp.PI_LOCATION_NAME);
			if ((locName == null) || (locName.length() == 0) || locName.contains("/")) {

				setMessage("Bad location name: " + "the location name should not contain " + "any character prohibited in a file name.", WARNING);

				return false;
			}
		}

		{
			String master = location.getConfPropValue(ConfProp.PI_JOB_TRACKER_HOST);
			if ((master == null) || (master.length() == 0)) {

				setMessage("Bad master host name: " + "the master host name refers to the machine " + "that runs the Job tracker.", WARNING);

				return false;
			}
		}

		{
			String jobTracker = location.getConfPropValue(ConfProp.JOB_TRACKER_URI);
			String[] strs = jobTracker.split(":");
			boolean ok = (strs.length == 2);
			if (ok) {
				try {
					int port = Integer.parseInt(strs[1]);
					ok = (port >= 0) && (port < 65536);
				} catch (NumberFormatException nfe) {
					ok = false;
				}
			}
			if (!ok) {
				setMessage("The job tracker information is invalid. " + "This usually looks like \"host:port\"",
						WARNING);
				return false;
			}
		}

		{
			String fsDefaultURI = location.getConfPropValue(ConfProp.FS_DEFAULT_URI);
			try {
				URI uri = new URI(fsDefaultURI);
			} catch (URISyntaxException e) {

				setMessage("The default file system URI is invalid. " + "This usually looks like \"hdfs://host:port/\" " + "or \"file:///dir/\"", WARNING);
			}
		}

		setMessage("Define the location of a Hadoop infrastructure " + "for running MapReduce applications.");
		return true;
	}

	/**
	 * Create the wizard
	 */
	/* @inheritDoc */
	public void createControl(final Composite parent) {
		setTitle("Define Hadoop location");
		setDescription("Define the location of a Hadoop infrastructure " + "for running MapReduce applications.");

		final Composite panel = new Composite(parent, SWT.FILL);
		GridLayout glayout = new GridLayout(2, false);
		panel.setLayout(glayout);
		final HadoopConfigurationBuilder uiConfigurationBuilder = location.getUIConfigurationBuilder();
		uiConfigurationBuilder.setChangeListener(new ChangeListener() {
			
			@Override
			public void notifyChange(ConfProp prop, String propValue) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						 getContainer().updateButtons();
					}});
			}
		});
		/*
		 * Location name
		 */
		{
			Label label = new Label(panel, SWT.NONE);
			label.setText( "&Location name:");
			Text text = new Text(panel, SWT.SINGLE | SWT.BORDER);
			GridData data = new GridData(GridData.FILL_HORIZONTAL);
			text.setLayoutData(data);
			text.setText(location.getConfPropValue(ConfProp.PI_LOCATION_NAME));
			text.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					final Text text = (Text) e.widget;
					final ConfProp prop = (ConfProp) text.getData("hProp");
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							uiConfigurationBuilder.notifyChange(ConfProp.PI_LOCATION_NAME,text.getText());
						}
					});
				}
			});
			locationName=text;
		}
		/*
		 * Hadoop version
		 */
		{
			Label label = new Label(panel, SWT.NONE);
			label.setText("&Hadoop Version:");
			Combo options = new Combo(panel, SWT.BORDER | SWT.READ_ONLY);
			for(HadoopVersion ver:HadoopVersion.values()){
				options.add(ver.getDisplayName());
			}
			int pos=0;
			for(String item:options.getItems()){
				if(item.equalsIgnoreCase(location.getVersion().getDisplayName())){
					options.select(pos);
					break;
				}
				pos++;
			}
			options.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			options.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					final String selection = hadoopVersion.getText();
					if (location == null || !selection.equals(location.getVersion())) {
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								try {
									location = AbstractHadoopCluster.createCluster(selection);
									location.setConfPropValue(ConfProp.PI_HADOOP_VERSION, selection);
									location.setConfPropValue(ConfProp.PI_LOCATION_NAME, locationName.getText());
									panel.dispose();
									createControl(parent);
									parent.pack();
									parent.getParent().layout(true);
								} catch (CoreException e) {
									MessageDialog.openError(Display.getDefault().getActiveShell(), "HDFS Error", "Unable to create HDFS site :"
											+ e.getMessage());
								}
							}
						});
					}

				}
			});
			hadoopVersion=options;
		}
		{
			uiConfigurationBuilder.buildControl(panel);
			this.setControl(panel);
		}
		
		{
			final Button btn = new Button(panel, SWT.NONE);
			btn.setText("&Load from file");
			btn.setEnabled(false);
			btn.setToolTipText("Not yet implemented");
			btn.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					// TODO
				}
			});
		}
		{
			final Button validate = new Button(panel, SWT.NONE);
			validate.setText("&Validate location");
			validate.setEnabled(false);
			validate.setToolTipText("Not yet implemented");
			validate.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					testLocation();
				}
			});
		}
		
		this.setControl(panel);
	}

}
