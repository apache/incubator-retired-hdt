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

package org.apache.hdt.core.internal.hdfs;

import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;

public class HDFSTeamRepositoryProvider extends RepositoryProvider {

	public static final String ID = "org.apache.hadoop.hdfs";
	private HDFSMoveDeleteHook moveDeleteHook = new HDFSMoveDeleteHook();

	public HDFSTeamRepositoryProvider() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void deconfigure() throws CoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public void configureProject() throws CoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getID() {
		return ID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.RepositoryProvider#getMoveDeleteHook()
	 */
	@Override
	public IMoveDeleteHook getMoveDeleteHook() {
		return moveDeleteHook;
	}

}
