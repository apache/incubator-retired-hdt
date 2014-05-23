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
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.hdt.core.Activator;
import org.apache.hdt.core.internal.HadoopManager;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

public abstract class AbstractHadoopCluster {
	
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

	abstract public boolean loadFromXML(File file) throws IOException;
	
	abstract public boolean isAvailable() throws CoreException;
	
	abstract public String getVersion();
	
	public static AbstractHadoopCluster createCluster(File file) throws CoreException, IOException {
		AbstractHadoopCluster hadoopCluster = createCluster(ConfProp.PI_HADOOP_VERSION.defVal);
		hadoopCluster.loadFromXML(file);
		return hadoopCluster;
	}

	public static AbstractHadoopCluster createCluster(String hadoopVersion) throws CoreException {
		logger.debug("Creating client for version "+hadoopVersion); 
		IConfigurationElement[] elementsFor = Platform.getExtensionRegistry().getConfigurationElementsFor("org.apache.hdt.core.hadoopCluster");
		for (IConfigurationElement configElement : elementsFor) {
			String version = configElement.getAttribute("protocolVersion");
			if(version.equalsIgnoreCase(hadoopVersion)){
				return (AbstractHadoopCluster)configElement.createExecutableExtension("class");
			}
		}
		throw new CoreException(new Status(Status.ERROR,Activator.BUNDLE_ID,"No clinet found for hadoop version "+hadoopVersion));
	}

	public static AbstractHadoopCluster createCluster(AbstractHadoopCluster existing) throws CoreException {
		AbstractHadoopCluster hadoopCluster = createCluster(existing.getVersion());
		hadoopCluster.load(existing);
		return hadoopCluster;
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
