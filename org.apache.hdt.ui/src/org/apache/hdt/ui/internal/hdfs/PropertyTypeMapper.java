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
 *  
 */
package org.apache.hdt.ui.internal.hdfs;

import org.apache.hdt.core.internal.hdfs.HDFSFileSystem;
import org.apache.log4j.Logger;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.views.properties.tabbed.ITypeMapper;

public class PropertyTypeMapper implements ITypeMapper {
	private static final Logger logger = Logger.getLogger(PropertyTypeMapper.class);

	@Override
	public Class mapType(Object object) {
		if (object instanceof IResource) {
			IResource resource = (IResource) object;
			if(HDFSFileSystem.SCHEME.equals(resource.getLocationURI().getScheme())){
				// This is a HDFS resource - only show the HDFS tab
				try {
					return EFS.getStore(resource.getLocationURI()).getClass();
				} catch (CoreException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
		return object.getClass();
	}

}
