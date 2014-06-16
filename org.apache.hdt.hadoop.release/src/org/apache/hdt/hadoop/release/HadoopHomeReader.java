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
package org.apache.hdt.hadoop.release;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.apache.hdt.core.AbstractHadoopHomeReader;
import org.eclipse.core.runtime.Path;

public class HadoopHomeReader extends AbstractHadoopHomeReader {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.hdt.core.AbstractHadoopHomeReader#validateHadoopHome(java.
	 * io.File)
	 */
	@Override
	public boolean validateHadoopHome(File location) {
		FilenameFilter gotHadoopJar = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.startsWith("hadoop") && name.endsWith(".jar") && (name.indexOf("test") == -1) && (name.indexOf("examples") == -1));
			}
		};
		return location.exists() && (location.list(gotHadoopJar).length > 0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.hdt.core.AbstractHadoopHomeReader#getHadoopJars(java.io.File)
	 */
	@Override
	public List<File> getHadoopJars(File hadoopHome) {
		File hadoopLib = new File(hadoopHome, "lib");

		final ArrayList<File> coreJars = new ArrayList<File>();
		coreJars.addAll(getJarFiles(hadoopHome));
		coreJars.addAll(getJarFiles(hadoopLib));
		return coreJars;
	}
	
	private ArrayList<File> getJarFiles(File hadoopHome) {
		FilenameFilter jarFileFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		};
		final ArrayList<File> jars = new ArrayList<File>();
		for (String hadopCoreLibFileName : hadoopHome.list(jarFileFilter)) {
			jars.add(new File(hadoopHome, hadopCoreLibFileName));
		}
		return jars;
	}

}
