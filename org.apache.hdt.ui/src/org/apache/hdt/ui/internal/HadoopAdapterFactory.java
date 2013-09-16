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
package org.apache.hdt.ui.internal;

import org.apache.hdt.core.internal.hdfs.HDFSFileStore;
import org.apache.hdt.core.internal.model.ZNode;
import org.apache.hdt.ui.internal.hdfs.HDFSFileStorePropertySource;
import org.apache.hdt.ui.internal.zookeeper.ZNodePropertySource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;

public class HadoopAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof HDFSFileStore) {
			HDFSFileStore fs = (HDFSFileStore) adaptableObject;
			if (adapterType == IPropertySource.class)
				return new HDFSFileStorePropertySource(fs);
		} else if (adaptableObject instanceof ZNode) {
			ZNode z = (ZNode) adaptableObject;
			return new ZNodePropertySource(z);
		}
		return null;
	}

	@Override
	public Class[] getAdapterList() {
		return new Class[] { IPropertySource.class };
	}

}
