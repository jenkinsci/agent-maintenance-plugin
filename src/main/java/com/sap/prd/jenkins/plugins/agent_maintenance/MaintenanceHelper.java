package com.sap.prd.jenkins.plugins.agent_maintenance;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.XmlFile;
import hudson.model.Computer;
import hudson.model.Slave;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SlaveComputer;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/** Helper class to manage maintenance windows. */
@Restricted(NoExternalUse.class)
public class MaintenanceHelper {
  private static final Logger LOGGER = Logger.getLogger(MaintenanceHelper.class.getName());

  private static final MaintenanceHelper INSTANCE = new MaintenanceHelper();

  private Map<String, SortedSet<MaintenanceWindow>> cache = new ConcurrentHashMap<>();

  private MaintenanceHelper() {
  }

  public boolean hasMaintenanceWindows(String computerName) throws IOException {
    return cache.containsKey(computerName) && getMaintenanceWindows(computerName).size() > 0;
  }

  /**
   * Adds a maintenance window to a computer.
   *
   * @param computerName Name of the computer for which to add the maintenance
   *                     window
   * @param mw           The maintance windows
   * @throws IOException when writing the xml failed
   */
  public void addMaintenanceWindow(String computerName, MaintenanceWindow mw) throws IOException {
    LOGGER.log(Level.FINE, "Adding maintenance window for {0}: {1}", new Object[] { computerName, mw.getId() });
    SortedSet<MaintenanceWindow> maintenanceList = getMaintenanceWindows(computerName);
    synchronized (maintenanceList) {
      maintenanceList.add(mw);
      saveMaintenanceWindows(computerName, maintenanceList);
    }
  }

  /**
   * Delete maintenance window from computer.
   *
   * @param computerName Name of the computer
   * @param id           Id of the maintenance window
   * @throws IOException when writing the xml failed
   */
  public void deleteMaintenanceWindow(String computerName, String id) throws IOException {
    LOGGER.log(Level.FINE, "Deleting maintenance window for {0}: {1}", new Object[] { id });
    SortedSet<MaintenanceWindow> maintenanceList = getMaintenanceWindows(computerName);
    synchronized (maintenanceList) {
      maintenanceList.removeIf(mw -> Objects.equals(id, mw.getId()));
      saveMaintenanceWindows(computerName, maintenanceList);
    }
  }

  /**
   * Returns the list of all configured maintenance windows for the computer with
   * the given name.
   *
   * @param computerName name of the agent for which to return maintenance windows
   * @return Set of maintenance windows
   * @throws IOException when an error occured reading the xml
   */
  @SuppressWarnings("unchecked")
  @NonNull
  public SortedSet<MaintenanceWindow> getMaintenanceWindows(String computerName) throws IOException {

    SortedSet<MaintenanceWindow> list = cache.get(computerName);
    LOGGER.log(Level.FINEST, "Loading maintenance list for {0}", computerName);

    if (list == null) {
      File maintenanceFile = getMaintenanceWindowsFile(computerName);
      if (maintenanceFile.exists()) {
        LOGGER.log(Level.FINER, "Loading maintenance list from file for {0}", computerName);
        XmlFile xmlMaintenanceFile = new XmlFile(maintenanceFile);
        list = (SortedSet<MaintenanceWindow>) xmlMaintenanceFile.read();
      } else {
        LOGGER.log(Level.FINER, "Creating empty maintenance list for {0}", computerName);
        list = new TreeSet<>();
      }
      // Only add to cache when the computer really exists in Jenkins.
      if (Jenkins.get().getComputer(computerName) != null) {
        cache.put(computerName, list);
      }
    }
    return list;
  }

  /**
   * Returns the first maintenance that is currently active or <code>null</code>
   * if configured maintenance windows are not active. All maintenance windows
   * that are finished are removed.
   *
   * @param computerName Name of computer
   * @return active maintenance or null
   */
  public @CheckForNull MaintenanceWindow getMaintenance(String computerName) {
    SortedSet<MaintenanceWindow> maintenanceList;
    try {
      maintenanceList = getMaintenanceWindows(computerName);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to read maintenance window list for {0}", computerName);
      return null;
    }
    MaintenanceWindow active = null;
    synchronized (maintenanceList) {
      Iterator<MaintenanceWindow> iter = maintenanceList.iterator();
      boolean changed = false;
      try {
        while (iter.hasNext()) {
          MaintenanceWindow m = iter.next();
          if (m.isMaintenanceScheduled() && active == null) {
            active = m;
            continue;
          }
          if (m.isMaintenanceOver()) {
            iter.remove();
            changed = true;
          }
        }
      } finally {
        if (changed) {
          try {
            saveMaintenanceWindows(computerName, maintenanceList);
          } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save maintenancelist for agent {0}", computerName);
          }
        }
      }
    }
    return active;
  }

  public static MaintenanceHelper getInstance() {
    return INSTANCE;
  }

  /**
   * Save maintenance window for computer.
   *
   * @param computerName       Name of computer
   * @param maintenanceWindows A set of maintenance windows
   * @throws IOException when writing the xml failed
   */
  public void saveMaintenanceWindows(String computerName, SortedSet<MaintenanceWindow> maintenanceWindows) throws IOException {
    LOGGER.log(Level.FINER, "Saving maintenance window for {0}: {1}", new Object[] { computerName, maintenanceWindows.size() });
    XmlFile xmlMaintenanceFile = new XmlFile(getMaintenanceWindowsFile(computerName));
    xmlMaintenanceFile.write(maintenanceWindows);
  }

  private File getMaintenanceWindowsFile(String computerName) throws IOException {
    return new File(new File(getNodesDirectory(), computerName), "maintenance-windows.xml");
  }

  private File getNodesDirectory() throws IOException {
    // jenkins.model.Nodes#getNodesDirectory() is private, so we have to duplicate
    // it here.
    File nodesDir = new File(Jenkins.get().getRootDir(), "nodes");
    if (!nodesDir.exists() || !nodesDir.isDirectory()) {
      throw new IOException("Nodes directory does not exist");
    }
    return nodesDir;
  }

  public void deleteAgent(String computerName) {
    cache.remove(computerName);
  }

  /**
   * Keeps track of agent renames.
   *
   * @param oldName Old name of the agent
   * @param newName new name of the agent
   */
  public void renameAgent(String oldName, String newName) {
    SortedSet<MaintenanceWindow> list = cache.get(oldName);
    if (list != null) {
      LOGGER.log(Level.FINEST, "Persisting existing maintenance windows after agent rename");
      cache.remove(oldName);
      cache.put(newName, list);
      try {
        saveMaintenanceWindows(newName, list);
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Failed to persists agent maintenance windows after agent rename {0}", newName);
      }
    }
  }

  public void createAgent(String nodeName) {
    cache.put(nodeName, Collections.synchronizedSortedSet(new TreeSet<>()));
  }

  /**
   * Inject the agent maintenance retention strategy from the given computer.
   *
   * @param c The computer for which to inject the strategy
   * @return true if strategy was injected, false otherwise
   */
  public boolean injectRetentionStrategy(Computer c) {
    if (c != null && c instanceof SlaveComputer) {
      SlaveComputer computer = (SlaveComputer) c;
      @SuppressWarnings("unchecked")
      RetentionStrategy<SlaveComputer> strategy = computer.getRetentionStrategy();
      if (!(strategy instanceof AgentMaintenanceRetentionStrategy)) {
        AgentMaintenanceRetentionStrategy maintenanceStrategy = new AgentMaintenanceRetentionStrategy(strategy);
        Slave node = computer.getNode();
        if (node != null) {
          node.setRetentionStrategy(maintenanceStrategy);
          try {
            node.save();
          } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save Node while injecting retention strategy: ", e);
          }
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Remove the agent maintenance retention strategy from the given computer.
   *
   * @param c The computer for which to remove the strategy
   * @return true if strategy was removed, false otherwise
   */
  @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  public boolean removeRetentionStrategy(Computer c) {
    if (c != null && c instanceof SlaveComputer) {
      SlaveComputer computer = (SlaveComputer) c;
      String computerName = computer.getName();
      @SuppressWarnings("unchecked")
      RetentionStrategy<SlaveComputer> strategy = computer.getRetentionStrategy();
      if (strategy instanceof AgentMaintenanceRetentionStrategy) {
        AgentMaintenanceRetentionStrategy maintenanceStrategy = (AgentMaintenanceRetentionStrategy) strategy;
        Slave node = computer.getNode();
        if (node != null) {
          node.setRetentionStrategy(maintenanceStrategy.getRegularRetentionStrategy());
          try {
            node.save();
            deleteAgent(computerName);
            File maintenanceFile = getMaintenanceWindowsFile(computerName);
            if (maintenanceFile.isFile()) {
              maintenanceFile.delete();
            }
          } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save node or remove file with maintenance windows while removing retention strategy: ", e);
          }
          return true;
        }
      }
    }
    return false;
  }
}
