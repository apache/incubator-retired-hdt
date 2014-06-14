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

package org.apache.hdt.hadoop2.release;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.hdt.core.launch.AbstractHadoopCluster;
import org.apache.hdt.core.launch.AbstractHadoopCluster.ChangeListener;
import org.apache.hdt.core.launch.AbstractHadoopCluster.HadoopConfigurationBuilder;
import org.apache.hdt.core.launch.ConfProp;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

class HadoopV2ConfigurationBuilder implements HadoopConfigurationBuilder {

    private AbstractHadoopCluster location;
    private TabMediator mediator;
    private ChangeListener changelistener;

    public HadoopV2ConfigurationBuilder(AbstractHadoopCluster location) {
        this.location = location;
    }

    @Override
    public void buildControl(Composite panel) {
        mediator = new TabMediator(panel);
        GridData gdata = new GridData(GridData.FILL_BOTH);
        gdata.horizontalSpan = 2;
        mediator.folder.setLayoutData(gdata);
    }

    private interface TabListener {
        void notifyChange(ConfProp prop, String propValue);
    }

    private class TabMediator {
        TabFolder folder;
        private Set<TabListener> tabs = new HashSet<TabListener>();

        TabMediator(Composite parent) {
            folder = new TabFolder(parent, SWT.NONE);
            tabs.add(new TabMain(this));
            tabs.add(new TabAdvanced(this));
        }

        /**
         * Implements change notifications from any tab: update the
         * location state and other tabs
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
            changelistener.notifyChange(prop, propValue);

            this.fireChange(source, prop, propValue);

            /*
             * Now we deal with dependencies between settings
             */
            final String rmHost = location.getConfPropValue(ConfProp.PI_RESOURCE_MGR_HOST);
            final String rmPort = location.getConfPropValue(ConfProp.PI_RESOURCE_MGR_PORT);
            final String jhHost = location.getConfPropValue(ConfProp.PI_JOB_HISTORY_HOST);
            final String jhPort = location.getConfPropValue(ConfProp.PI_JOB_HISTORY_PORT);
            final String nameNodeHost = location.getConfPropValue(ConfProp.PI_NAME_NODE_HOST);
            final String nameNodePort = location.getConfPropValue(ConfProp.PI_NAME_NODE_PORT);
            final boolean colocate = location.getConfPropValue(ConfProp.PI_COLOCATE_MASTERS).equalsIgnoreCase("yes");
            final String rmDefaultURI = location.getConfPropValue(ConfProp.RM_DEFAULT_URI);
            final String jhDefaultURI = location.getConfPropValue(ConfProp.JOB_HISTORY_DEFAULT_URI);
            final String fsDefaultURI = location.getConfPropValue(ConfProp.FS_DEFAULT_URI);
            final String socksServerURI = location.getConfPropValue(ConfProp.SOCKS_SERVER);
            final boolean socksProxyEnable = location.getConfPropValue(ConfProp.PI_SOCKS_PROXY_ENABLE).equalsIgnoreCase("yes");
            final String socksProxyHost = location.getConfPropValue(ConfProp.PI_SOCKS_PROXY_HOST);
            final String socksProxyPort = location.getConfPropValue(ConfProp.PI_SOCKS_PROXY_PORT);

            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    switch (prop) {
                    case PI_RESOURCE_MGR_HOST: {
                        if (colocate) {
                            notifyChange(null, ConfProp.PI_NAME_NODE_HOST, rmHost);
                            notifyChange(null, ConfProp.PI_JOB_HISTORY_HOST, rmHost);
                        }
                        String newJobTrackerURI = String.format("%s:%s", rmHost, rmPort);
                        notifyChange(null, ConfProp.RM_DEFAULT_URI, newJobTrackerURI);
                        break;
                    }
                    case PI_RESOURCE_MGR_PORT: {
                        String newJobTrackerURI = String.format("%s:%s", rmHost, rmPort);
                        notifyChange(null, ConfProp.RM_DEFAULT_URI, newJobTrackerURI);
                        break;
                    }
                    case PI_NAME_NODE_HOST: {
                        String newHDFSURI = String.format("hdfs://%s:%s/", nameNodeHost, nameNodePort);
                        notifyChange(null, ConfProp.FS_DEFAULT_URI, newHDFSURI);

                        // Break colocation if someone force the DFS Master
                        if (!colocate && !nameNodeHost.equals(rmHost))
                            notifyChange(null, ConfProp.PI_COLOCATE_MASTERS, "no");
                        break;
                    }
                    case PI_NAME_NODE_PORT: {
                        String newHDFSURI = String.format("hdfs://%s:%s/", nameNodeHost, nameNodePort);
                        notifyChange(null, ConfProp.FS_DEFAULT_URI, newHDFSURI);
                        break;
                    }

                    case PI_JOB_HISTORY_HOST: {
                        String newJobHistoryURI = String.format("%s:%s", jhHost, jhPort);
                        notifyChange(null, ConfProp.JOB_HISTORY_DEFAULT_URI, newJobHistoryURI);

                        // Break colocation if someone force the DFS Master
                        if (!colocate && !nameNodeHost.equals(rmHost))
                            notifyChange(null, ConfProp.PI_COLOCATE_MASTERS, "no");
                        break;
                    }
                    case PI_JOB_HISTORY_PORT: {
                        String newJobHistoryURI = String.format("%s:%s", jhHost, jhPort);
                        notifyChange(null, ConfProp.JOB_HISTORY_DEFAULT_URI, newJobHistoryURI);
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
                    case RM_DEFAULT_URI: {
                        String[] strs = rmDefaultURI.split(":", 2);
                        String host = strs[0];
                        String port = (strs.length == 2) ? strs[1] : "";
                        notifyChange(null, ConfProp.PI_RESOURCE_MGR_HOST, host);
                        notifyChange(null, ConfProp.PI_RESOURCE_MGR_PORT, port);
                        break;
                    }
                    case JOB_HISTORY_DEFAULT_URI: {
                        String[] strs = jhDefaultURI.split(":", 2);
                        String host = strs[0];
                        String port = (strs.length == 2) ? strs[1] : "";
                        notifyChange(null, ConfProp.PI_JOB_HISTORY_HOST, host);
                        notifyChange(null, ConfProp.PI_JOB_HISTORY_PORT, port);
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
                        if (colocate) {
                            notifyChange(null, ConfProp.PI_NAME_NODE_HOST, rmHost);
                            notifyChange(null, ConfProp.PI_JOB_HISTORY_HOST, rmHost);
                        }
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
         * Change notifications on properties (by name). A property might
         * not be reflected as a ConfProp enum. If it is, the notification
         * is forwarded to the ConfProp notifyChange method. If not, it is
         * processed here.
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
        text.setData("hProp", prop);
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
     * Create editor entry for the given configuration property. The editor
     * is a couple (Label, Text).
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
     * @param labelTextRACKER_HOST
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
     * Map/Reduce master (Job tracker) <li>host and port of the DFS master
     * (Name node) <li>SOCKS proxy
     */
    private class TabMain implements TabListener, ModifyListener, SelectionListener {

        TabMediator mediator;

        Text textRMHost;

        Text textNNHost;

        Button colocateMasters;

        Text textJTPort;

        Text textNNPort;

        Text userName;

        Button useSocksProxy;

        Text socksProxyHost;

        Text socksProxyPort;

        private Group groupMR;

        private Text textJHHost;

        private Text textJHPort;

        TabMain(TabMediator mediator) {
            this.mediator = mediator;
            TabItem tab = new TabItem(mediator.folder, SWT.NONE);
            tab.setText("General");
            tab.setToolTipText("General location parameters");
            tab.setControl(createControl(mediator.folder));
        }

        private Control createControl(Composite parent) {

            Composite panel = new Composite(parent, SWT.FILL);
            panel.setLayout(new GridLayout(2, false));

            GridData data;

            /*
             * Map/Reduce group
             */
            {
                groupMR = new Group(panel, SWT.SHADOW_NONE);
                groupMR.setText("Resource Manager Node");
                groupMR.setToolTipText("Address of the Resource Manager node.");
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

                textRMHost = createConfText(this, groupMR, ConfProp.PI_RESOURCE_MGR_HOST);
                data = new GridData(GridData.FILL, GridData.CENTER, true, true);
                textRMHost.setLayoutData(data);

                colocateMasters = createConfCheckButton(this, groupMR, ConfProp.PI_COLOCATE_MASTERS, "Use RM host for other services.");
                data = new GridData();
                data.horizontalSpan = 2;
                colocateMasters.setLayoutData(data);

                // Job Tracker port
                label = new Label(groupMR, SWT.NONE);
                label.setText("Port:");
                data = new GridData(GridData.BEGINNING, GridData.CENTER, false, true);
                label.setLayoutData(data);

                textJTPort = createConfText(this, groupMR, ConfProp.PI_RESOURCE_MGR_PORT);
                data = new GridData(GridData.FILL, GridData.CENTER, true, true);
                textJTPort.setLayoutData(data);
            }

            /*
             * Job history Server
             */
            {
                Group groupDFS = new Group(panel, SWT.SHADOW_NONE);
                groupDFS.setText("Job History Node");
                groupDFS.setToolTipText("Address of the Job Histroy Node.");
                GridLayout layout = new GridLayout(2, false);
                groupDFS.setLayout(layout);
                data = new GridData();
                data.horizontalAlignment = SWT.CENTER;
                data.verticalAlignment = SWT.FILL;
                data.widthHint = 250;
                groupDFS.setLayoutData(data);

                // Job Tracker host
                Label label = new Label(groupDFS, SWT.NONE);
                data = new GridData();
                label.setText("Host:");
                label.setLayoutData(data);

                textJHHost = createConfText(this, groupDFS, ConfProp.PI_JOB_HISTORY_HOST);

                // Job Tracker port
                label = new Label(groupDFS, SWT.NONE);
                data = new GridData();
                label.setText("Port:");
                label.setLayoutData(data);

                textJHPort = createConfText(this, groupDFS, ConfProp.PI_JOB_HISTORY_PORT);
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
                data.verticalAlignment = SWT.FILL;
                data.widthHint = 250;
                groupDFS.setLayoutData(data);

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

            // SOCKS proxy group
            {
                Group groupSOCKS = new Group(panel, SWT.SHADOW_NONE);
                groupSOCKS.setText("SOCKS proxy");
                groupSOCKS.setToolTipText("Address of the SOCKS proxy to use " + "to connect to the infrastructure.");
                GridLayout layout = new GridLayout(2, false);
                groupSOCKS.setLayout(layout);
                data = new GridData();
                data.horizontalAlignment = SWT.CENTER;
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

            // Update the state of all widgets according to the current
            // values!
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

        public void notifyChange(ConfProp prop, String propValue) {
            switch (prop) {
            case PI_RESOURCE_MGR_HOST: {
                textRMHost.setText(propValue);
                break;
            }
            case PI_RESOURCE_MGR_PORT: {
                textJTPort.setText(propValue);
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
                    if (textJHHost != null) {
                        textJHHost.setEnabled(!colocate);
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
            case PI_JOB_HISTORY_HOST: {
                textJHHost.setText(propValue);
                break;
            }
            case PI_JOB_HISTORY_PORT: {
                textJHPort.setText(propValue);
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
            tab.setControl(createControl(mediator.folder));

        }

        private Control createControl(Composite parent) {
            ScrolledComposite sc = new ScrolledComposite(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
            panel = buildPanel(sc);
            sc.setContent(panel);
            sc.setExpandHorizontal(true);
            sc.setExpandVertical(true);
            sc.setMinSize(640, 480);
            sc.setMinSize(panel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            return sc;
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

    @Override
    public void notifyChange(ConfProp confProp, String text) {
        mediator.notifyChange(null, ConfProp.PI_LOCATION_NAME, text);
    }

    @Override
    public void setChangeListener(ChangeListener l) {
        changelistener=l;
    }

}