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
