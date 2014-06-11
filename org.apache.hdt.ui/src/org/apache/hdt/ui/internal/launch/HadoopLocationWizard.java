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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.hdt.core.internal.hdfs.HDFSManager;
import org.apache.hdt.core.launch.AbstractHadoopCluster;
import org.apache.hdt.core.launch.ConfProp;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

/**
 * Wizard for editing the settings of a Hadoop location
 * 
 * The wizard contains 3 tabs: General, Tunneling and Advanced. It edits
 * parameters of the location member which either a new location or a copy of an
 * existing registered location.
 */

public class HadoopLocationWizard extends WizardPage {

	public  static final String HADOOP_1 = "1.1";
	public  static final String HADOOP_2 = "2.2";
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

	/**
	 * New Hadoop location wizard
	 */
	public HadoopLocationWizard() {
		super("Hadoop Server", "New Hadoop Location", null);

		this.original = null;
		try {
			this.location = AbstractHadoopCluster.createCluster(ConfProp.PI_HADOOP_VERSION.defVal);
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
			this.location = AbstractHadoopCluster.createCluster(server);
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
								.getConfPropValue(ConfProp.PI_USER_NAME), null,location.getVersion());
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
									.getConfPropValue(ConfProp.PI_USER_NAME), null,location.getVersion());
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
	public void createControl(Composite parent) {
		setTitle("Define Hadoop location");
		setDescription("Define the location of a Hadoop infrastructure " + "for running MapReduce applications.");

		Composite panel = new Composite(parent, SWT.FILL);
		GridLayout glayout = new GridLayout(2, false);
		panel.setLayout(glayout);

		TabMediator mediator = new TabMediator(panel);
		{
			GridData gdata = new GridData(GridData.FILL_BOTH);
			gdata.horizontalSpan = 2;
			mediator.folder.setLayoutData(gdata);
		}
		this.setControl(panel /* mediator.folder */);
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
	}

	private interface TabListener {
		void notifyChange(ConfProp prop, String propValue);
		void reloadData();
	}

	/*
	 * Mediator pattern to keep tabs synchronized with each other and with the
	 * location state.
	 */

	private class TabMediator {
		TabFolder folder;

		private Set<TabListener> tabs = new HashSet<TabListener>();

		TabMediator(Composite parent) {
			folder = new TabFolder(parent, SWT.NONE);
			tabs.add(new TabMain(this));
			tabs.add(new TabAdvanced(this));
		}

		/**
		 * Implements change notifications from any tab: update the location
		 * state and other tabs
		 * 
		 * @param source
		 *            origin of the notification (one of the tree tabs)
		 * @param propName
		 *            modified property
		 * @param propValue
		 *            new value
		 */
		void notifyChange(TabListener source, final ConfProp prop, final String propValue) {
			// Ignore notification when no change
			String oldValue = location.getConfPropValue(prop);
			if ((oldValue != null) && oldValue.equals(propValue))
				return;

			location.setConfPropValue(prop, propValue);
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					getContainer().updateButtons();
				}
			});

			this.fireChange(source, prop, propValue);

			/*
			 * Now we deal with dependencies between settings
			 */
			final String jobTrackerHost = location.getConfPropValue(ConfProp.PI_JOB_TRACKER_HOST);
			final String jobTrackerPort = location.getConfPropValue(ConfProp.PI_JOB_TRACKER_PORT);
			final String nameNodeHost = location.getConfPropValue(ConfProp.PI_NAME_NODE_HOST);
			final String nameNodePort = location.getConfPropValue(ConfProp.PI_NAME_NODE_PORT);
			final boolean colocate = location.getConfPropValue(ConfProp.PI_COLOCATE_MASTERS).equalsIgnoreCase("yes");
			final String jobTrackerURI = location.getConfPropValue(ConfProp.JOB_TRACKER_URI) ;
			final String fsDefaultURI = location.getConfPropValue(ConfProp.FS_DEFAULT_URI);
			final String socksServerURI = location.getConfPropValue(ConfProp.SOCKS_SERVER);
			final boolean socksProxyEnable = location.getConfPropValue(ConfProp.PI_SOCKS_PROXY_ENABLE).equalsIgnoreCase("yes");
			final String socksProxyHost = location.getConfPropValue(ConfProp.PI_SOCKS_PROXY_HOST);
			final String socksProxyPort = location.getConfPropValue(ConfProp.PI_SOCKS_PROXY_PORT);

			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					switch (prop) {
					case PI_JOB_TRACKER_HOST: {
						if (colocate)
							notifyChange(null, ConfProp.PI_NAME_NODE_HOST, jobTrackerHost);
						String newJobTrackerURI = String.format("%s:%s", jobTrackerHost, jobTrackerPort);
						notifyChange(null, ConfProp.JOB_TRACKER_URI, newJobTrackerURI);
						break;
					}
					case PI_JOB_TRACKER_PORT: {
						String newJobTrackerURI = String.format("%s:%s", jobTrackerHost, jobTrackerPort);
						notifyChange(null, ConfProp.JOB_TRACKER_URI, newJobTrackerURI);
						break;
					}
					case PI_NAME_NODE_HOST: {
						String newHDFSURI = String.format("hdfs://%s:%s/", nameNodeHost, nameNodePort);
						notifyChange(null, ConfProp.FS_DEFAULT_URI, newHDFSURI);

						// Break colocation if someone force the DFS Master
						if (!colocate && !nameNodeHost.equals(jobTrackerHost))
							notifyChange(null, ConfProp.PI_COLOCATE_MASTERS, "no");
						break;
					}
					case PI_NAME_NODE_PORT: {
						String newHDFSURI = String.format("hdfs://%s:%s/", nameNodeHost, nameNodePort);
						notifyChange(null, ConfProp.FS_DEFAULT_URI, newHDFSURI);
						break;
					}
					case PI_SOCKS_PROXY_HOST: {
						String newSocksProxyURI = String.format("%s:%s", socksProxyHost, socksProxyPort);
						notifyChange(null, ConfProp.SOCKS_SERVER, newSocksProxyURI);
						break;
					}
					case PI_SOCKS_PROXY_PORT: {
						String newSocksProxyURI = String.format("%s:%s", socksProxyHost, socksProxyPort);
						notifyChange(null, ConfProp.SOCKS_SERVER, newSocksProxyURI);
						break;
					}
					case JOB_TRACKER_URI: {
						String[] strs = jobTrackerURI.split(":", 2);
						String host = strs[0];
						String port = (strs.length == 2) ? strs[1] : "";
						notifyChange(null, ConfProp.PI_JOB_TRACKER_HOST, host);
						notifyChange(null, ConfProp.PI_JOB_TRACKER_PORT, port);
						break;
					}
					case FS_DEFAULT_URI: {
						try {
							URI uri = new URI(fsDefaultURI);
							if (uri.getScheme().equals("hdfs")) {
								String host = uri.getHost();
								String port = Integer.toString(uri.getPort());
								notifyChange(null, ConfProp.PI_NAME_NODE_HOST, host);
								notifyChange(null, ConfProp.PI_NAME_NODE_PORT, port);
							}
						} catch (URISyntaxException use) {
							// Ignore the update!
						}
						break;
					}
					case SOCKS_SERVER: {
						String[] strs = socksServerURI.split(":", 2);
						String host = strs[0];
						String port = (strs.length == 2) ? strs[1] : "";
						notifyChange(null, ConfProp.PI_SOCKS_PROXY_HOST, host);
						notifyChange(null, ConfProp.PI_SOCKS_PROXY_PORT, port);
						break;
					}
					case PI_COLOCATE_MASTERS: {
						if (colocate)
							notifyChange(null, ConfProp.PI_NAME_NODE_HOST, jobTrackerHost);
						break;
					}
					case PI_SOCKS_PROXY_ENABLE: {
						if (socksProxyEnable) {
							notifyChange(null, ConfProp.SOCKET_FACTORY_DEFAULT, "org.apache.hadoop.net.SocksSocketFactory");
						} else {
							notifyChange(null, ConfProp.SOCKET_FACTORY_DEFAULT, "org.apache.hadoop.net.StandardSocketFactory");
						}
						break;
					}					
					}
				}
			});

		}

		/**
		 * Change notifications on properties (by name). A property might not be
		 * reflected as a ConfProp enum. If it is, the notification is forwarded
		 * to the ConfProp notifyChange method. If not, it is processed here.
		 * 
		 * @param source
		 * @param propName
		 * @param propValue
		 */
		void notifyChange(TabListener source, String propName, String propValue) {
			ConfProp prop = location.getConfPropForName(propName);
			if (prop != null)
				notifyChange(source, prop, propValue);
			else
				location.setConfPropValue(propName, propValue);
		}

		/**
		 * Broadcast a property change to all registered tabs. If a tab is
		 * identified as the source of the change, this tab will not be
		 * notified.
		 * 
		 * @param source
		 *            TODO
		 * @param prop
		 * @param value
		 */
		private void fireChange(TabListener source, ConfProp prop, String value) {
			for (TabListener tab : tabs) {
				if (tab != source)
					tab.notifyChange(prop, value);
			}
		}

	}

	/**
	 * Create a SWT Text component for the given {@link ConfProp} text
	 * configuration property.
	 * 
	 * @param listener
	 * @param parent
	 * @param prop
	 * @return
	 */
	private Text createConfText(ModifyListener listener, Composite parent, ConfProp prop) {
		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		text.setLayoutData(data);
		text.setData("hProp",prop);
		text.setText(location.getConfPropValue(prop));
		text.addModifyListener(listener);

		return text;
	}

	/**
	 * Create a SWT Checked Button component for the given {@link ConfProp}
	 * boolean configuration property.
	 * 
	 * @param listener
	 * @param parent
	 * @param prop
	 * @return
	 */
	private Button createConfCheckButton(SelectionListener listener, Composite parent, ConfProp prop, String text) {
		Button button = new Button(parent, SWT.CHECK);
		button.setText(text);
		button.setData("hProp", prop);
		button.setSelection(location.getConfPropValue(prop).equalsIgnoreCase("yes"));
		button.addSelectionListener(listener);
		return button;
	}

	/**
	 * Create editor entry for the given configuration property. The editor is a
	 * couple (Label, Text).
	 * 
	 * @param listener
	 *            the listener to trigger on property change
	 * @param parent
	 *            the SWT parent container
	 * @param prop
	 *            the property to create an editor for
	 * @param labelText
	 *            a label (null will defaults to the property name)
	 * 
	 * @return a SWT Text field
	 */
	private Text createConfLabelText(ModifyListener listener, Composite parent, ConfProp prop, String labelText) {
		Label label = new Label(parent, SWT.NONE);
		if (labelText == null)
			labelText = location.getConfPropName(prop);
		label.setText(labelText);
		return createConfText(listener, parent, prop);
	}

	/**
	 * Create an editor entry for the given configuration name
	 * 
	 * @param listener
	 *            the listener to trigger on property change
	 * @param parent
	 *            the SWT parent container
	 * @param propName
	 *            the name of the property to create an editor for
	 * @param labelText
	 *            a label (null will defaults to the property name)
	 * 
	 * @return a SWT Text field
	 */
	private Text createConfNameEditor(ModifyListener listener, Composite parent, String propName, String labelText) {

		{
			ConfProp prop = location.getConfPropForName(propName);
			if (prop != null)
				return createConfLabelText(listener, parent, prop, labelText);
		}

		Label label = new Label(parent, SWT.NONE);
		if (labelText == null)
			labelText = propName;
		label.setText(labelText);

		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		text.setLayoutData(data);
		text.setData("hPropName", propName);
		text.setText(location.getConfPropValue(propName));
		text.addModifyListener(listener);

		return text;
	}

	/**
	 * Main parameters of the Hadoop location: <li>host and port of the
	 * Map/Reduce master (Job tracker) <li>host and port of the DFS master (Name
	 * node) <li>SOCKS proxy
	 */
	private class TabMain implements TabListener, ModifyListener, SelectionListener {

		/**
		 * 
		 */
		

		TabMediator mediator;

		Text locationName;
		
		Combo hadoopVersion;

		Text textJTHost;
		

		Text textNNHost;

		Button colocateMasters;

		Text textJTPort;

		Text textNNPort;

		Text userName;

		Button useSocksProxy;

		Text socksProxyHost;

		Text socksProxyPort;

		private Group groupMR;

		TabMain(TabMediator mediator) {
			this.mediator = mediator;
			TabItem tab = new TabItem(mediator.folder, SWT.NONE);
			tab.setText("General");
			tab.setToolTipText("General location parameters");
			tab.setImage(circle);
			tab.setControl(createControl(mediator.folder));
		}

		private Control createControl(Composite parent) {

			Composite panel = new Composite(parent, SWT.FILL);
			panel.setLayout(new GridLayout(2, false));

			GridData data;

			/*
			 * Location name
			 */
			{
				Composite subpanel = new Composite(panel, SWT.FILL);
				subpanel.setLayout(new GridLayout(2, false));
				data = new GridData();
				data.horizontalSpan = 2;
				data.horizontalAlignment = SWT.FILL;
				subpanel.setLayoutData(data);

				locationName = createConfLabelText(this, subpanel, ConfProp.PI_LOCATION_NAME, "&Location name:");
			}
			/*
			 * Hadoop version
			 */
			{
				Composite subpanel = new Composite(panel, SWT.FILL);
				subpanel.setLayout(new GridLayout(2, false));
				data = new GridData();
				data.horizontalSpan = 2;
				data.horizontalAlignment = SWT.FILL;
				subpanel.setLayoutData(data);
				
				Label label = new Label(subpanel, SWT.NONE);
				label.setText("&Hadoop Version:");
				Combo options =  new Combo (subpanel, SWT.BORDER | SWT.READ_ONLY);
				options.add (HADOOP_1);
				options.add (HADOOP_2);
				options.select(0);
				options.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				options.addListener (SWT.Selection, new Listener () {
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
										location.setConfPropValue(ConfProp.PI_LOCATION_NAME, "");
										for (TabListener tab : mediator.tabs) {
											tab.reloadData();
										}
									} catch (CoreException e) {
										MessageDialog.openError(Display.getDefault().getActiveShell(), "HDFS Error", "Unable to create HDFS site :"
												+ e.getMessage());
									}
								}
							});
						}

					}
				});
				hadoopVersion = options;
			}
			
			/*
			 * Map/Reduce group
			 */
			{
				groupMR = new Group(panel, SWT.SHADOW_NONE);
				groupMR.setText("Map/Reduce Master");
				groupMR.setToolTipText("Address of the Map/Reduce master node " + "(the Job Tracker).");
				GridLayout layout = new GridLayout(2, false);
				groupMR.setLayout(layout);
				data = new GridData();
				data.verticalAlignment = SWT.FILL;
				data.horizontalAlignment = SWT.CENTER;
				data.widthHint = 250;
				groupMR.setLayoutData(data);

				// Job Tracker host
				Label label = new Label(groupMR, SWT.NONE);
				label.setText("Host:");
				data = new GridData(GridData.BEGINNING, GridData.CENTER, false, true);
				label.setLayoutData(data);

				textJTHost = createConfText(this, groupMR, ConfProp.PI_JOB_TRACKER_HOST);
				data = new GridData(GridData.FILL, GridData.CENTER, true, true);
				textJTHost.setLayoutData(data);

				// Job Tracker port
				label = new Label(groupMR, SWT.NONE);
				label.setText("Port:");
				data = new GridData(GridData.BEGINNING, GridData.CENTER, false, true);
				label.setLayoutData(data);

				textJTPort = createConfText(this, groupMR, ConfProp.PI_JOB_TRACKER_PORT);
				data = new GridData(GridData.FILL, GridData.CENTER, true, true);
				textJTPort.setLayoutData(data);
			}

			/*
			 * DFS group
			 */
			{
				Group groupDFS = new Group(panel, SWT.SHADOW_NONE);
				groupDFS.setText("DFS Master");
				groupDFS.setToolTipText("Address of the Distributed FileSystem " + "master node (the Name Node).");
				GridLayout layout = new GridLayout(2, false);
				groupDFS.setLayout(layout);
				data = new GridData();
				data.horizontalAlignment = SWT.CENTER;
				data.widthHint = 250;
				groupDFS.setLayoutData(data);

				colocateMasters = createConfCheckButton(this, groupDFS, ConfProp.PI_COLOCATE_MASTERS, "Use M/R Master host");
				data = new GridData();
				data.horizontalSpan = 2;
				colocateMasters.setLayoutData(data);

				// Job Tracker host
				Label label = new Label(groupDFS, SWT.NONE);
				data = new GridData();
				label.setText("Host:");
				label.setLayoutData(data);

				textNNHost = createConfText(this, groupDFS, ConfProp.PI_NAME_NODE_HOST);

				// Job Tracker port
				label = new Label(groupDFS, SWT.NONE);
				data = new GridData();
				label.setText("Port:");
				label.setLayoutData(data);

				textNNPort = createConfText(this, groupDFS, ConfProp.PI_NAME_NODE_PORT);
			}

			{
				Composite subpanel = new Composite(panel, SWT.FILL);
				subpanel.setLayout(new GridLayout(2, false));
				data = new GridData();
				data.horizontalSpan = 2;
				data.horizontalAlignment = SWT.FILL;
				subpanel.setLayoutData(data);

				userName = createConfLabelText(this, subpanel, ConfProp.PI_USER_NAME, "&User name:");
			}

			// SOCKS proxy group
			{
				Group groupSOCKS = new Group(panel, SWT.SHADOW_NONE);
				groupSOCKS.setText("SOCKS proxy");
				groupSOCKS.setToolTipText("Address of the SOCKS proxy to use " + "to connect to the infrastructure.");
				GridLayout layout = new GridLayout(2, false);
				groupSOCKS.setLayout(layout);
				data = new GridData();
				data.horizontalAlignment = SWT.CENTER;
				data.horizontalSpan = 2;
				data.widthHint = 250;
				groupSOCKS.setLayoutData(data);

				useSocksProxy = createConfCheckButton(this, groupSOCKS, ConfProp.PI_SOCKS_PROXY_ENABLE, "Enable SOCKS proxy");
				data = new GridData();
				data.horizontalSpan = 2;
				useSocksProxy.setLayoutData(data);

				// SOCKS proxy host
				Label label = new Label(groupSOCKS, SWT.NONE);
				data = new GridData();
				label.setText("Host:");
				label.setLayoutData(data);

				socksProxyHost = createConfText(this, groupSOCKS, ConfProp.PI_SOCKS_PROXY_HOST);

				// SOCKS proxy port
				label = new Label(groupSOCKS, SWT.NONE);
				data = new GridData();
				label.setText("Port:");
				label.setLayoutData(data);

				socksProxyPort = createConfText(this, groupSOCKS, ConfProp.PI_SOCKS_PROXY_PORT);
			}

			// Update the state of all widgets according to the current values!
			reloadConfProp(ConfProp.PI_COLOCATE_MASTERS);
			reloadConfProp(ConfProp.PI_SOCKS_PROXY_ENABLE);
			reloadConfProp(ConfProp.PI_HADOOP_VERSION);

			return panel;
		}

		/**
		 * Reload the given configuration property value
		 * 
		 * @param prop
		 */
		private void reloadConfProp(ConfProp prop) {
			this.notifyChange(prop, location.getConfPropValue(prop));
		}
		
		@Override
		public void reloadData() {
			if (HADOOP_2.equals(hadoopVersion.getText())) {
				groupMR.setText("Resource Manager Master");
				groupMR.setToolTipText("Address of the Resouce manager node ");
			} else {
				groupMR.setText("Map/Reduce Master");
				groupMR.setToolTipText("Address of the Map/Reduce master node " + "(the Job Tracker).");
			}
			groupMR.layout(true);
			notifyChange(ConfProp.PI_JOB_TRACKER_HOST,location.getConfPropValue(ConfProp.PI_JOB_TRACKER_HOST));
			notifyChange(ConfProp.PI_JOB_TRACKER_PORT,location.getConfPropValue(ConfProp.PI_JOB_TRACKER_PORT));
			notifyChange(ConfProp.PI_USER_NAME,location.getConfPropValue(ConfProp.PI_USER_NAME));
			notifyChange(ConfProp.PI_NAME_NODE_HOST,location.getConfPropValue(ConfProp.PI_NAME_NODE_HOST));
			notifyChange(ConfProp.PI_USER_NAME,location.getConfPropValue(ConfProp.PI_USER_NAME));
			notifyChange(ConfProp.PI_COLOCATE_MASTERS,location.getConfPropValue(ConfProp.PI_COLOCATE_MASTERS));
			notifyChange(ConfProp.PI_SOCKS_PROXY_ENABLE,location.getConfPropValue(ConfProp.PI_SOCKS_PROXY_ENABLE));
			notifyChange(ConfProp.PI_SOCKS_PROXY_HOST,location.getConfPropValue(ConfProp.PI_SOCKS_PROXY_HOST));
			notifyChange(ConfProp.PI_SOCKS_PROXY_PORT,location.getConfPropValue(ConfProp.PI_SOCKS_PROXY_PORT));
			notifyChange(ConfProp.PI_LOCATION_NAME,location.getConfPropValue(ConfProp.PI_LOCATION_NAME));
		}

		public void notifyChange(ConfProp prop, String propValue) {
			switch (prop) {
			case PI_JOB_TRACKER_HOST: {
				textJTHost.setText(propValue);
				break;
			}
			case PI_JOB_TRACKER_PORT: {
				textJTPort.setText(propValue);
				break;
			}
			case PI_LOCATION_NAME: {
				locationName.setText(propValue);
				break;
			}
			case PI_USER_NAME: {
				userName.setText(propValue);
				break;
			}
			case PI_COLOCATE_MASTERS: {
				if (colocateMasters != null) {
					boolean colocate = propValue.equalsIgnoreCase("yes");
					colocateMasters.setSelection(colocate);
					if (textNNHost != null) {
						textNNHost.setEnabled(!colocate);
					}
				}
				break;
			}
			case PI_NAME_NODE_HOST: {
				textNNHost.setText(propValue);
				break;
			}
			case PI_NAME_NODE_PORT: {
				textNNPort.setText(propValue);
				break;
			}
			case PI_SOCKS_PROXY_ENABLE: {
				if (useSocksProxy != null) {
					boolean useProxy = propValue.equalsIgnoreCase("yes");
					useSocksProxy.setSelection(useProxy);
					if (socksProxyHost != null)
						socksProxyHost.setEnabled(useProxy);
					if (socksProxyPort != null)
						socksProxyPort.setEnabled(useProxy);
				}
				break;
			}
			case PI_SOCKS_PROXY_HOST: {
				socksProxyHost.setText(propValue);
				break;
			}
			case PI_SOCKS_PROXY_PORT: {
				socksProxyPort.setText(propValue);
				break;
			}			
			}
		}

		
		/* @inheritDoc */
		public void modifyText(ModifyEvent e) {
			final Text text = (Text) e.widget;
			final ConfProp prop = (ConfProp) text.getData("hProp");
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					mediator.notifyChange(TabMain.this, prop, text.getText());
				}
			});
		}

		/* @inheritDoc */
		public void widgetDefaultSelected(SelectionEvent e) {
			this.widgetSelected(e);
		}

		/* @inheritDoc */
		public void widgetSelected(SelectionEvent e) {
			final Button button = (Button) e.widget;
			final ConfProp prop = (ConfProp) button.getData("hProp");

			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					// We want to receive the update also!
					mediator.notifyChange(null, prop, button.getSelection() ? "yes" : "no");
				}
			});
		}

	}

	private class TabAdvanced implements TabListener, ModifyListener {
		TabMediator mediator;
		private Composite panel;
		private Map<String, Text> textMap = new TreeMap<String, Text>();

		TabAdvanced(TabMediator mediator) {
			this.mediator = mediator;
			TabItem tab = new TabItem(mediator.folder, SWT.NONE);
			tab.setText("Advanced parameters");
			tab.setToolTipText("Access to advanced Hadoop parameters");
			tab.setImage(circle);
			tab.setControl(createControl(mediator.folder));

		}

		private Control createControl(Composite parent) {
			ScrolledComposite sc = new ScrolledComposite(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
			panel=buildPanel(sc);
			sc.setContent(panel);
			sc.setExpandHorizontal(true);
			sc.setExpandVertical(true);
			sc.setMinSize(640, 480);
			sc.setMinSize(panel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			return sc;
		}
		
		@Override
		public void reloadData() {
			ScrolledComposite parent = (ScrolledComposite)panel.getParent();
			panel.dispose();
			Composite panel = buildPanel(parent);
			parent.setContent(panel);
			parent.setMinSize(panel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			parent.pack();
			parent.layout(true);
			this.panel=panel;
		}

		private Composite buildPanel(Composite parent) {
			Composite panel = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			layout.makeColumnsEqualWidth = false;
			panel.setLayout(layout);
			panel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 1, 1));

			// Sort by property name
			SortedMap<String, String> map = new TreeMap<String, String>();
			Iterator<Entry<String, String>> it = location.getConfiguration();
			while (it.hasNext()) {
				Entry<String, String> entry = it.next();
				map.put(entry.getKey(), entry.getValue());
			}

			for (Entry<String, String> entry : map.entrySet()) {
				Text text = createConfNameEditor(this, panel, entry.getKey(), null);
				textMap.put(entry.getKey(), text);
			}
			return panel;
		}
		

		public void notifyChange(ConfProp prop, final String propValue) {
			Text text = textMap.get(location.getConfPropName(prop));
			text.setText(propValue);
		}

		public void modifyText(ModifyEvent e) {
			final Text text = (Text) e.widget;
			Object hProp = text.getData("hProp");
			final ConfProp prop = (hProp != null) ? (ConfProp) hProp : null;
			Object hPropName = text.getData("hPropName");
			final String propName = (hPropName != null) ? (String) hPropName : null;

			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					if (prop != null)
						mediator.notifyChange(TabAdvanced.this, prop, text.getText());
					else
						mediator.notifyChange(TabAdvanced.this, propName, text.getText());
				}
			});
		}

	
	}

}
