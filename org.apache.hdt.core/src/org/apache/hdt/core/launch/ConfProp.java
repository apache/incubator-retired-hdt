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

import java.util.HashMap;
import java.util.Map;

public enum ConfProp {
	/**
	 * Property name for the Hadoop location name
	 */
	PI_LOCATION_NAME(true, "location.name", "New Hadoop location"),
	
	/**
	 * Property name for the Hadoop Version
	 */
	PI_HADOOP_VERSION(true, "hadoop.version", "1.1"),

	/**
	 * Property name for the master host name (the Job tracker)
	 */
	PI_JOB_TRACKER_HOST(true, "jobtracker.host", "localhost"),
	
	PI_RESOURCE_MGR_HOST(true, "rm.host", "localhost"),
	
	PI_JOB_HISTORY_HOST(true, "jobhistory.host", "localhost"),

	/**
	 * Property name for the DFS master host name (the Name node)
	 */
	PI_NAME_NODE_HOST(true, "namenode.host", "localhost"),

	/**
	 * User name to use for Hadoop operations
	 */
	PI_USER_NAME(true, "user.name", System.getProperty("user.name", "who are you?")),

	/**
	 * Property name for SOCKS proxy activation
	 */
	PI_SOCKS_PROXY_ENABLE(true, "socks.proxy.enable", "no"),

	/**
	 * Property name for the SOCKS proxy host
	 */
	PI_SOCKS_PROXY_HOST(true, "socks.proxy.host", "host"),

	/**
	 * Property name for the SOCKS proxy port
	 */
	PI_SOCKS_PROXY_PORT(true, "socks.proxy.port", "1080"),

	/**
	 * TCP port number for the name node
	 */
	PI_NAME_NODE_PORT(true, "namenode.port", "50040"),

	/**
	 * TCP port number for the job tracker
	 */
	PI_JOB_TRACKER_PORT(true, "jobtracker.port", "50020"),
	
	PI_RESOURCE_MGR_PORT(true, "rm.port", "8032"),
	
	PI_JOB_HISTORY_PORT(true, "jobhistory.port", "10020"),

	/**
	 * Are the Map/Reduce and the Distributed FS masters hosted on the same
	 * machine?
	 */
	PI_COLOCATE_MASTERS(true, "masters.colocate", "yes"),

	/**
	 * Property name for naming the job tracker (URI). This property is related
	 * to {@link #PI_MASTER_HOST_NAME}
	 */
	JOB_TRACKER_URI(false, "mapred.job.tracker", "localhost:50020"),

	/**
	 * Property name for naming the default file system (URI).
	 */
	FS_DEFAULT_URI(false, "fs.default.name", "hdfs://localhost:50040/"),
	
	RM_DEFAULT_URI(false, "yarn.resourcemanager.address", "localhost:8032"),
	
	JOB_HISTORY_DEFAULT_URI(false, "mapreduce.jobhistory.address", "localhost:10020"),

	/**
	 * Property name for the default socket factory:
	 */
	SOCKET_FACTORY_DEFAULT(false, "hadoop.rpc.socket.factory.class.default", "org.apache.hadoop.net.StandardSocketFactory"),

	/**
	 * Property name for the SOCKS server URI.
	 */
	SOCKS_SERVER(false, "hadoop.socks.server", "host:1080"),

	;

	/**
	 * Map <property name> -> ConfProp
	 */
	private static Map<String, ConfProp> map;

	private static synchronized void registerProperty(String name, ConfProp prop) {

		if (ConfProp.map == null)
			ConfProp.map = new HashMap<String, ConfProp>();

		ConfProp.map.put(name, prop);
	}

	public static ConfProp getByName(String propName) {
		return map.get(propName);
	}
	protected  final String name;

	public final String defVal;

	ConfProp(boolean internal, String name, String defVal) {
		if (internal)
			name = "eclipse.plug-in." + name;
		this.name = name;
		this.defVal = defVal;

		ConfProp.registerProperty(name, this);
	}

}
