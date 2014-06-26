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

import java.util.Iterator;

import org.apache.hdt.core.internal.model.ZNode;
import org.apache.hdt.core.internal.model.ZooKeeperServer;
import org.apache.hdt.core.internal.zookeeper.ZooKeeperManager;
import org.apache.hdt.core.zookeeper.ZooKeeperClient;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.navigator.resources.ProjectExplorer;

public class DeleteAction implements IObjectActionDelegate {

	private final static Logger logger = Logger.getLogger(DeleteAction.class);
	private ISelection selection;
	private IWorkbenchPart targetPart;
	
	
	private void showError(String message) {
		MessageDialog.openError(Display.getDefault().getActiveShell(), 
				"ZooKeeper Delete Error", message);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				if (selection != null && !selection.isEmpty()) {
				IStructuredSelection sSelection = (IStructuredSelection) selection;
				@SuppressWarnings("rawtypes")
				Iterator itr = sSelection.iterator();
				while (itr.hasNext()) {
					Object object = itr.next();
					if (object instanceof ZooKeeperServer) {
						ZooKeeperServer r = (ZooKeeperServer) object;
						if (logger.isDebugEnabled())
							logger.debug("Deleting: " + r);
						try {
							ZooKeeperManager.INSTANCE.disconnect(r);
						} catch (CoreException e) {
							logger.error("Error occurred ", e);
						} finally {
							 try {
								ZooKeeperManager.INSTANCE.delete(r);
							} catch (CoreException e) {
								logger.error("Error occurred ", e);
								IStatus status = e.getStatus();
								showError(status.getException().getMessage());
							}
						}
						if (logger.isDebugEnabled())
							logger.debug("Deleted: " + r);
						if (targetPart instanceof ProjectExplorer) {
							ProjectExplorer pe = (ProjectExplorer) targetPart;
							pe.getCommonViewer().refresh();
						}
					} else if (object instanceof ZNode) {
						ZNode zkn = (ZNode) object;
						if (logger.isDebugEnabled())
							logger.debug("Deleting: " + zkn);
						try {
							ZooKeeperClient client = ZooKeeperManager.INSTANCE.getClient(zkn.getServer());
							client.delete(zkn);
						} catch (Exception e) {
							logger.error("Error occurred ", e);
							showError(e.getMessage());
						}
					}
				}
			}}
		});
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action
	 * .IAction, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
		boolean enabled = true;
		if (this.selection != null && !this.selection.isEmpty()) {
			IStructuredSelection sSelection = (IStructuredSelection) this.selection;
			@SuppressWarnings("rawtypes")
			Iterator itr = sSelection.iterator();
			while (itr.hasNext()) {
				Object object = itr.next();
				enabled = false;
				if (object instanceof ZooKeeperServer) {
					ZooKeeperServer server = (ZooKeeperServer) object;
					enabled = server != null;
				} else if (object instanceof ZNode) {
					ZNode zkn = (ZNode) object;
					enabled = zkn != null;
				}
			}
		} else
			enabled = false;
		action.setEnabled(enabled);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.
	 * action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

}
