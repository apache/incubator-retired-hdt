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
package org.apache.hdt.ui.internal.zookeeper;

import org.apache.hdt.core.internal.model.ServerStatus;
import org.apache.hdt.core.internal.model.ZNode;
import org.apache.hdt.core.internal.model.ZooKeeperServer;
import org.apache.hdt.ui.Activator;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

public class ZooKeeperLightweightLabelDecorator implements ILightweightLabelDecorator {

	@Override
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang
	 * .Object, org.eclipse.jface.viewers.IDecoration)
	 */
	@Override
	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof ZooKeeperServer) {
			ZooKeeperServer zks = (ZooKeeperServer) element;

			// Image decorations
			if (zks.getStatusCode() == ServerStatus.DISCONNECTED_VALUE)
				decoration.addOverlay(org.apache.hdt.ui.Activator.IMAGE_OFFLINE_OVR);
			else
				decoration.addOverlay(org.apache.hdt.ui.Activator.IMAGE_ONLINE_OVR);

			// Text decorations
			decoration.addSuffix("  " + zks.getUri());
		} else if (element instanceof ZNode) {
			ZNode zkn = (ZNode) element;
			if (zkn.getVersion() > -1) {
				decoration.addSuffix("  [v=" + zkn.getVersion() + "]");
			}
			if (zkn.isEphermeral())
				decoration.addOverlay(Activator.IMAGE_ZOOKEEPER_EPHERMERAL, IDecoration.BOTTOM_RIGHT);
		}
	}

}
