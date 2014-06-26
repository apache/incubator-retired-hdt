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

package org.apache.hdt.core;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

public abstract class AbstractHadoopHomeReader {
	private static final Logger logger = Logger.getLogger(AbstractHadoopHomeReader.class);
	public abstract boolean validateHadoopHome(File location);
	public abstract List<File> getHadoopJars(File location);
	
	public static AbstractHadoopHomeReader createReader(String hadoopVersion) throws CoreException {
		logger.debug("Creating  hadoop home reader"); 
		IConfigurationElement[] elementsFor = Platform.getExtensionRegistry().getConfigurationElementsFor("org.apache.hdt.core.hadoopHomeReader");
		for (IConfigurationElement configElement : elementsFor) {
			String version = configElement.getAttribute("protocolVersion");
			if (version.equalsIgnoreCase(hadoopVersion)) {
				return (AbstractHadoopHomeReader)configElement.createExecutableExtension("class");
			}
		}
		throw new CoreException(new Status(Status.ERROR,Activator.BUNDLE_ID,"No Reader found for hadoop version"+hadoopVersion));
	}
}
