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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.hdt.core.AbstractHadoopHomeReader;

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
	        File hadoopBin = new File(location, "bin");
	        File hadoopSBIn = new File(location, "sbin");
		FilenameFilter gotHadoopYarn = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.indexOf("yarn") != -1);
			}
		};
		return hadoopBin.exists() && (hadoopBin.list(gotHadoopYarn).length > 0) 
		        && hadoopSBIn.exists() && (hadoopSBIn.list(gotHadoopYarn).length > 0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.hdt.core.AbstractHadoopHomeReader#getHadoopJars(java.io.File)
	 */
	@Override
	public List<File> getHadoopJars(File hadoopHome) {
	        File mrCommonHome =  FileUtils.getFile(hadoopHome, "share","hadoop","common");
                File mrCommonLib =  FileUtils.getFile(mrCommonHome,"lib");
                File hdfsHome =  FileUtils.getFile(hadoopHome, "share","hadoop","hdfs");
                File hdfsLib =  FileUtils.getFile(hdfsHome,"lib");
                File yarnHome =  FileUtils.getFile(hadoopHome, "share","hadoop","yarn");
                File yarnLib =  FileUtils.getFile(yarnHome,"lib");
		File mrHome =  FileUtils.getFile(hadoopHome, "share","hadoop","mapreduce");
		File mrLib =  FileUtils.getFile(mrHome,"lib");
		
		FilenameFilter jarFileFilter = new FilenameFilter() {
		    Set<String> selectedFileName= new HashSet<String>();
                    @Override
                    public boolean accept(File dir, String name) {
                            boolean accept = name.endsWith(".jar") 
                                    && !selectedFileName.contains(name);
                            if(accept){
                                selectedFileName.add(name);
                            }
                            return accept;
                    }
            };
		final ArrayList<File> coreJars = new ArrayList<File>();
		coreJars.addAll(getJarFiles(mrCommonHome,jarFileFilter));
		coreJars.addAll(getJarFiles(mrCommonLib,jarFileFilter));
		coreJars.addAll(getJarFiles(hdfsHome,jarFileFilter));
                coreJars.addAll(getJarFiles(hdfsLib,jarFileFilter));
                coreJars.addAll(getJarFiles(yarnHome,jarFileFilter));
                coreJars.addAll(getJarFiles(yarnLib,jarFileFilter));
		coreJars.addAll(getJarFiles(mrHome,jarFileFilter));
		coreJars.addAll(getJarFiles(mrLib,jarFileFilter));
		return coreJars;
	}
	
	private ArrayList<File> getJarFiles(File hadoopHome, FilenameFilter jarFileFilter) {
		final ArrayList<File> jars = new ArrayList<File>();
		for (String hadopCoreLibFileName : hadoopHome.list(jarFileFilter)) {
			jars.add(new File(hadoopHome, hadopCoreLibFileName));
		}
		return jars;
	}

}
