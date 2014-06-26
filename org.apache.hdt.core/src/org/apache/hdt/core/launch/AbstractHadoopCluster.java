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

package org.apache.hdt.core.launch;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.hdt.core.Activator;
import org.apache.hdt.core.HadoopVersion;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public abstract class AbstractHadoopCluster {

	public interface ChangeListener {
		void notifyChange(ConfProp prop, String propValue);
	}

	public interface HadoopConfigurationBuilder {
		void buildControl(Composite panel);

		void notifyChange(ConfProp confProp, String text);

		void setChangeListener(ChangeListener l);
	}

	private static final Logger logger = Logger.getLogger(AbstractHadoopCluster.class);

	abstract public String getLocationName();

	abstract public void dispose();

	abstract public void storeSettingsToFile(File file) throws IOException;

	abstract public void saveConfiguration(File confDir, String jarFilePath) throws IOException;

	abstract public String getMasterHostName();

	abstract public void setLocationName(String string);

	abstract public void load(AbstractHadoopCluster server);

	abstract public String getConfPropValue(String propName);

	abstract public String getConfPropValue(ConfProp prop);

	abstract public void setConfPropValue(ConfProp prop, String propValue);

	abstract public void setConfPropValue(String propName, String propValue);

	abstract public Iterator<Entry<String, String>> getConfiguration();

	abstract public void purgeJob(IHadoopJob job);

	abstract public void addJobListener(IJobListener jobListener);

	abstract public Collection<? extends IHadoopJob> getJobs();

	abstract public String getState();

	abstract protected boolean loadConfiguration(Map<String, String> configuration);

	abstract public boolean isAvailable() throws CoreException;

	abstract public HadoopVersion getVersion();

	abstract public HadoopConfigurationBuilder getUIConfigurationBuilder();

	public static AbstractHadoopCluster createCluster(File file) throws CoreException, IOException {
		Map<String, String> configuration = loadXML(file);
		String version = configuration.get(ConfProp.PI_HADOOP_VERSION.name);
		AbstractHadoopCluster hadoopCluster = createCluster(version != null ? version : ConfProp.PI_HADOOP_VERSION.defVal);
		hadoopCluster.loadConfiguration(configuration);
		return hadoopCluster;
	}

	public static AbstractHadoopCluster createCluster(String hadoopVersion) throws CoreException {
		logger.debug("Creating client for version " + hadoopVersion);
		IConfigurationElement[] elementsFor = Platform.getExtensionRegistry().getConfigurationElementsFor("org.apache.hdt.core.hadoopCluster");
		for (IConfigurationElement configElement : elementsFor) {
			String version = configElement.getAttribute("protocolVersion");
			if (version.equalsIgnoreCase(hadoopVersion)) {
				return (AbstractHadoopCluster) configElement.createExecutableExtension("class");
			}
		}
		throw new CoreException(new Status(Status.ERROR, Activator.BUNDLE_ID, "No clinet found for hadoop version " + hadoopVersion));
	}

	public static AbstractHadoopCluster createCluster(AbstractHadoopCluster existing) throws CoreException {
		AbstractHadoopCluster hadoopCluster = createCluster(existing.getVersion().getDisplayName());
		hadoopCluster.load(existing);
		return hadoopCluster;
	}

	protected static Map<String, String> loadXML(File file) {
		DocumentBuilder builder;
		Document document;
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			document = builder.parse(file);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		} catch (SAXException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		Element root = document.getDocumentElement();
		if (!"configuration".equals(root.getTagName()))
			return null;
		NodeList props = root.getChildNodes();
		Map<String, String> configuration = new HashMap<String, String>();
		for (int i = 0; i < props.getLength(); i++) {
			Node propNode = props.item(i);
			if (!(propNode instanceof Element))
				continue;
			Element prop = (Element) propNode;
			if (!"property".equals(prop.getTagName()))
				return null;
			NodeList fields = prop.getChildNodes();
			String attr = null;
			String value = null;
			for (int j = 0; j < fields.getLength(); j++) {
				Node fieldNode = fields.item(j);
				if (!(fieldNode instanceof Element))
					continue;
				Element field = (Element) fieldNode;
				if ("name".equals(field.getTagName()))
					attr = ((Text) field.getFirstChild()).getData();
				if ("value".equals(field.getTagName()) && field.hasChildNodes())
					value = ((Text) field.getFirstChild()).getData();
			}
			if (attr != null && value != null)
				configuration.put(attr, value);
		}
		return configuration;
	}

	/**
	 * @param propName
	 * @return
	 */
	public ConfProp getConfPropForName(String propName) {
		return ConfProp.getByName(propName);
	}

	public String getConfPropName(ConfProp prop) {
		return prop.name;
	}

}
