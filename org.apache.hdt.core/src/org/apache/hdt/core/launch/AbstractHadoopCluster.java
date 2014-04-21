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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public abstract class AbstractHadoopCluster {

	abstract public String getLocationName();

	abstract public void dispose();

	abstract public void storeSettingsToFile(File file) throws IOException;

	abstract public void saveConfiguration(File confDir, String jarFilePath) throws IOException;

	abstract public String getMasterHostName();

	abstract public void setLocationName(String string);

	abstract public void load(AbstractHadoopCluster server);

	abstract public String getConfProp(String propName);

	abstract public String getConfProp(ConfProp prop);

	abstract public void setConfProp(ConfProp prop, String propValue);

	abstract public void setConfProp(String propName, String propValue);

	abstract public Iterator<Entry<String, String>> getConfiguration();

	abstract public void purgeJob(IHadoopJob job);

	abstract public void addJobListener(IJobListener jobListener);

	abstract public Collection<? extends IHadoopJob> getJobs();

	abstract public String getState();

	abstract public boolean loadFromXML(File file) throws IOException;

	public static AbstractHadoopCluster createCluster(File file) throws CoreException, IOException {
		AbstractHadoopCluster hadoopCluster = createCluster();
		hadoopCluster.loadFromXML(file);
		return hadoopCluster;
	}

	public static AbstractHadoopCluster createCluster() throws CoreException {
		IConfigurationElement[] elementsFor = Platform.getExtensionRegistry().getConfigurationElementsFor("org.apache.hdt.core.hadoopCluster");
		return (AbstractHadoopCluster) elementsFor[0].createExecutableExtension("class");
	}

	public static AbstractHadoopCluster createCluster(AbstractHadoopCluster existing) throws CoreException {
		AbstractHadoopCluster hadoopCluster = createCluster();
		hadoopCluster.load(existing);
		return hadoopCluster;
	}

}
