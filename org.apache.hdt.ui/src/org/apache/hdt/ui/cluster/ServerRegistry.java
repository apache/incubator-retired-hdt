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

package org.apache.hdt.ui.cluster;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hdt.core.cluster.HadoopCluster;
import org.apache.hdt.core.cluster.IHadoopClusterListener;
import org.apache.hdt.ui.Activator;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * Register of Hadoop locations.
 * 
 * Each location corresponds to a Hadoop {@link Configuration} stored as an
 * XML file in the workspace plug-in configuration directory:
 * <p>
 * <tt>
 * &lt;workspace-dir&gt;/.metadata/.plugins/org.apache.hadoop.eclipse/locations/*.xml
 * </tt>
 * 
 */
public class ServerRegistry {

  private static final ServerRegistry INSTANCE = new ServerRegistry();

  public static final int SERVER_ADDED = 0;

  public static final int SERVER_REMOVED = 1;

  public static final int SERVER_STATE_CHANGED = 2;

  private final File baseDir =
      Activator.getDefault().getStateLocation().toFile();

  private final File saveDir = new File(baseDir, "locations");

  private ServerRegistry() {
    if (saveDir.exists() && !saveDir.isDirectory())
      saveDir.delete();
    if (!saveDir.exists())
      saveDir.mkdirs();

    load();
  }

  private Map<String, HadoopCluster> servers;

  private Set<IHadoopClusterListener> listeners =
      new HashSet<IHadoopClusterListener>();

  public static ServerRegistry getInstance() {
    return INSTANCE;
  }

  public synchronized Collection<HadoopCluster> getServers() {
    return Collections.unmodifiableCollection(servers.values());
  }

  /**
   * Load all available locations from the workspace configuration directory.
   */
  private synchronized void load() {
    Map<String, HadoopCluster> map = new TreeMap<String, HadoopCluster>();
    for (File file : saveDir.listFiles()) {
      try {
        HadoopCluster server = new HadoopCluster(file);
        map.put(server.getLocationName(), server);

      } catch (Exception exn) {
        System.err.println(exn);
      }
    }
    this.servers = map;
  }

  private synchronized void store() {
    try {
      File dir = File.createTempFile("locations", "new", baseDir);
      dir.delete();
      dir.mkdirs();

      for (HadoopCluster server : servers.values()) {
        server.storeSettingsToFile(new File(dir, server.getLocationName()
            + ".xml"));
      }

      FilenameFilter XMLFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
          String lower = name.toLowerCase();
          return lower.endsWith(".xml");
        }
      };

      File backup = new File(baseDir, "locations.backup");
      if (backup.exists()) {
        for (File file : backup.listFiles(XMLFilter))
          if (!file.delete())
            throw new IOException("Unable to delete backup location file: "
                + file);
        if (!backup.delete())
          throw new IOException(
              "Unable to delete backup location directory: " + backup);
      }

      saveDir.renameTo(backup);
      dir.renameTo(saveDir);

    } catch (IOException ioe) {
      ioe.printStackTrace();
      MessageDialog.openError(null,
          "Saving configuration of Hadoop locations failed", ioe.toString());
    }
  }

  public void dispose() {
    for (HadoopCluster server : getServers()) {
      server.dispose();
    }
  }

  public synchronized HadoopCluster getServer(String location) {
    return servers.get(location);
  }

  /*
   * HadoopServer map listeners
   */

  public void addListener(IHadoopClusterListener l) {
    synchronized (listeners) {
      listeners.add(l);
    }
  }

  public void removeListener(IHadoopClusterListener l) {
    synchronized (listeners) {
      listeners.remove(l);
    }
  }

  private void fireListeners(HadoopCluster location, int kind) {
    synchronized (listeners) {
      for (IHadoopClusterListener listener : listeners) {
        listener.serverChanged(location, kind);
      }
    }
  }

  public synchronized void removeServer(HadoopCluster server) {
    this.servers.remove(server.getLocationName());
    store();
    fireListeners(server, SERVER_REMOVED);
  }

  public synchronized void addServer(HadoopCluster server) {
    this.servers.put(server.getLocationName(), server);
    store();
    fireListeners(server, SERVER_ADDED);
  }

  /**
   * Update one Hadoop location
   * 
   * @param originalName the original location name (might have changed)
   * @param server the location
   */
  public synchronized void updateServer(String originalName,
      HadoopCluster server) {

    // Update the map if the location name has changed
    if (!server.getLocationName().equals(originalName)) {
      servers.remove(originalName);
      servers.put(server.getLocationName(), server);
    }
    store();
    fireListeners(server, SERVER_STATE_CHANGED);
  }
}
