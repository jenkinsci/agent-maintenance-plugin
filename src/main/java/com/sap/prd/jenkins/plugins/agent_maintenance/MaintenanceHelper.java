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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/** Helper class to manage maintenance windows. */
@Restricted(NoExternalUse.class)
public class MaintenanceHelper {
  private static final Logger LOGGER = Logger.getLogger(MaintenanceHelper.class.getName());

  private static final MaintenanceHelper INSTANCE = new MaintenanceHelper();

  private Map<String, MaintenanceDefinitions> cache = new ConcurrentHashMap<>();

  private MaintenanceHelper() {
  }

  /**
   * Checks it the given string is a valid UUID.
   * Returns the id if valid, otherwise it returns a new random UUID string.
   *
   * @param id The id to check
   * @return A valid UUID string
   */
  public static String getUuid(String id) {
    UUID uuid;
    try {
      uuid = UUID.fromString(id);
      return uuid.toString();
    } catch (IllegalArgumentException iae) {
      return UUID.randomUUID().toString();
    }
  }

  /**
   * Checks if the given string is a valid UUID.
   *
   * @param id The id to check
   * @return true if id is a valid UUID
   */
  private boolean isValidUuid(String id) {
    try {
      UUID.fromString(id);
      return true;
    } catch (IllegalArgumentException iae) {
      return false;
    }
  }

  private boolean isValidComputerName(String computerName) throws IOException {
    Jenkins jenkins = Jenkins.get();
    Computer computer = jenkins.getComputer(computerName);
    return computer != null;
  }

  private String getSafeComputerName(String computerName) {
    Jenkins jenkins = Jenkins.get();
    Computer computer = jenkins.getComputer(computerName);
    return computer != null ? computerName : "unknown";
  }

  public boolean hasMaintenanceWindows(String computerName) throws IOException {
    return cache.containsKey(computerName) && getMaintenanceWindows(computerName).size() > 0;
  }

  /**
   * Return whether there are active maintenance windows for a computer.
   *
   * @param computerName The computer to check
   * @return true when the given computer has an active maintenance window
   * @throws IOException when reading the xml failed
   */
  public boolean hasActiveMaintenanceWindows(String computerName) throws IOException {
    if (!cache.containsKey(computerName)) {
      return false;
    }
    SortedSet<MaintenanceWindow> maintenanceList;
    try {
      maintenanceList = getMaintenanceWindows(computerName);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to read maintenance window list for {0}", computerName);
      return false;
    }

    return maintenanceList.stream().anyMatch(MaintenanceWindow::isMaintenanceScheduled);
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
    LOGGER.log(Level.FINE, "Adding maintenance window for {0}: {1}", new Object[] { getSafeComputerName(computerName), mw.getId() });
    MaintenanceDefinitions md = getMaintenanceDefinitions(computerName);
    synchronized (md) {
      md.getScheduled().add(mw);
      saveMaintenanceWindows(computerName, md);
    }
  }

  /**
   * Adds a maintenance window to a computer.
   *
   * @param computerName Name of the computer for which to add the maintenance
   *                     window
   * @param mw           The maintance windows
   * @throws IOException when writing the xml failed
   */
  public void addRecurringMaintenanceWindow(String computerName, RecurringMaintenanceWindow mw) throws IOException {
    LOGGER.log(Level.FINE, "Adding maintenance window for {0}: {1}", new Object[] { getSafeComputerName(computerName), mw.getId() });
    MaintenanceDefinitions md = getMaintenanceDefinitions(computerName);
    synchronized (md) {
      md.getRecurring().add(mw);
      saveMaintenanceWindows(computerName, md);
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
    if (isValidUuid(id) && isValidComputerName(computerName)) {
      LOGGER.log(Level.FINE, "Deleting maintenance window for {0}: {1}", new Object[]{getSafeComputerName(computerName), id});
      MaintenanceDefinitions md = getMaintenanceDefinitions(computerName);
      synchronized (md) {
        md.getScheduled().removeIf(mw -> Objects.equals(id, mw.getId()));
        saveMaintenanceWindows(computerName, md);
      }
    }
  }

  /**
   * Delete maintenance window from computer.
   *
   * @param computerName Name of the computer
   * @param id           Id of the maintenance window
   * @throws IOException when writing the xml failed
   */
  public void deleteRecurringMaintenanceWindow(String computerName, String id) throws IOException {
    if (isValidUuid(id) && isValidComputerName(computerName)) {
      LOGGER.log(Level.FINE, "Deleting maintenance window for {0}: {1}", new Object[]{getSafeComputerName(computerName), id});
      MaintenanceDefinitions md = getMaintenanceDefinitions(computerName);
      synchronized (md) {
        md.getRecurring().removeIf(mw -> Objects.equals(id, mw.getId()));
        saveMaintenanceWindows(computerName, md);
      }
    }
  }

  /**
   * Returns the list of all configured maintenance windows for the computer with
   * the given name.
   *
   * @param computerName name of the agent for which to return maintenance windows
   * @return Set of maintenance windows
   * @throws IOException when an error occurred reading the xml
   */
  @NonNull
  public SortedSet<MaintenanceWindow> getMaintenanceWindows(String computerName) throws IOException {
    LOGGER.log(Level.FINEST, "Loading maintenance list for {0}", getSafeComputerName(computerName));
    return getMaintenanceDefinitions(computerName).getScheduled();
  }

  /**
   * Returns the list of all configured recurring maintenance windows for the computer with
   * the given name.
   *
   * @param computerName name of the agent for which to return maintenance windows
   * @return Set of recurring maintenance windows
   * @throws IOException when an error occurred reading the xml
   */
  public Set<RecurringMaintenanceWindow> getRecurringMaintenanceWindows(String computerName) throws IOException {
    LOGGER.log(Level.FINEST, "Loading recurring maintenance definitions for {0}", getSafeComputerName(computerName));
    return getMaintenanceDefinitions(computerName).getRecurring();
  }

  /**
   * Returns the maintenance definitions for an agent.
   *
   * @param computerName name of the agent for which to return maintenance definitions
   * @return The {@link MaintenanceDefinitions} of the agent
   * @throws IOException when an error occurred reading the xml
   */
  public MaintenanceDefinitions getMaintenanceDefinitions(String computerName) throws IOException {

    LOGGER.log(Level.FINEST, "Loading maintenance list for {0}", getSafeComputerName(computerName));
    if (Jenkins.get().getComputer(computerName) == null) {
      return new MaintenanceDefinitions(new TreeSet<>(), new HashSet<>());
    }

    MaintenanceDefinitions md = cache.get(computerName);

    if (md == null) {
      XmlFile xmlMaintenanceFile = getMaintenanceWindowsFile(computerName);
      if (xmlMaintenanceFile.exists()) {
        LOGGER.log(Level.FINER, "Loading maintenance list from file for {0}", getSafeComputerName(computerName));
        try {
          md = (MaintenanceDefinitions) xmlMaintenanceFile.read();
          cache.put(computerName, md);
          return md;
        } catch (ClassCastException cce) {
          LOGGER.log(Level.WARNING, "Failed loading maintenance definition file for {0}. Trying to read old format",
                  getSafeComputerName(computerName));
        }
        SortedSet<MaintenanceWindow> scheduled = (SortedSet<MaintenanceWindow>) xmlMaintenanceFile.read();
        md = new MaintenanceDefinitions(scheduled, new HashSet<>());
        saveMaintenanceWindows(computerName, md);
      } else {
        LOGGER.log(Level.FINER, "Creating empty maintenance list for {0}", getSafeComputerName(computerName));
        md = new MaintenanceDefinitions(new TreeSet<>(), new HashSet<>());
      }
      if (Jenkins.get().getComputer(computerName) != null) {
        cache.put(computerName, md);
      }
    }
    return md;
  }

  /**
   * Returns the maintenance window with the given id that is connected to the given computer.
   *
   * @param computerName name of the computer
   * @param id id of the maintenance
   * @return The maintenance window or null if not found
   */
  @CheckForNull
  public MaintenanceWindow getMaintenanceWindow(String computerName, String id) {
    SortedSet<MaintenanceWindow> mwSet = null;
    try {
      mwSet = getMaintenanceWindows(computerName);
    } catch (IOException e) {
      return null;
    }
    Optional<MaintenanceWindow> mw = mwSet.stream().filter(w -> w.getId().equals(id)).findFirst();
    return mw.orElse(null);
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
    MaintenanceDefinitions md;
    try {
      md = getMaintenanceDefinitions(computerName);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to read maintenance window list for {0}", getSafeComputerName(computerName));
      return null;
    }
    MaintenanceWindow active = null;
    synchronized (md) {
      Iterator<MaintenanceWindow> iter = md.getScheduled().iterator();
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
            saveMaintenanceWindows(computerName, md);
          } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save maintenance definitions for agent {0}", getSafeComputerName(computerName));
          }
        }
      }
    }
    return active;
  }

  /**
   * Converts for an agent any of the recurring maintenance windows into scheduled maintenance windows if
   * the lead time is reached.
   *
   * @param computerName name of the agent to check
   */
  public void checkRecurring(String computerName) {
    LOGGER.log(Level.FINER, "Checking for recurring maintenance windows for {0}", getSafeComputerName(computerName));
    MaintenanceDefinitions md;
    try {
      md = getMaintenanceDefinitions(computerName);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to read maintenance definitions for {0}", getSafeComputerName(computerName));
      return;
    }

    boolean added = false;
    synchronized (md) {
      for (RecurringMaintenanceWindow rmw : md.getRecurring()) {
        Set<MaintenanceWindow> fmw = rmw.getFutureMaintenanceWindows();
        if (fmw.size() > 0) {
          LOGGER.log(Level.FINER, "Found future maintenance windows for {0}", getSafeComputerName(computerName));
          md.getScheduled().addAll(fmw);
          added = true;
        }
      }

      if (added) {
        try {
          saveMaintenanceWindows(computerName, md);
        } catch (IOException e) {
          LOGGER.log(Level.WARNING, "Failed to save maintenance definitions for agent {0}", getSafeComputerName(computerName));
        }
      }
    }
  }

  public static MaintenanceHelper getInstance() {
    return INSTANCE;
  }

  /**
   * Save maintenance window for computer.
   *
   * @param computerName       Name of computer
   * @param md A set of maintenance windows
   * @throws IOException when writing the xml failed
   */
  public void saveMaintenanceWindows(String computerName, MaintenanceDefinitions md) throws IOException {
    LOGGER.log(Level.FINER, "Saving maintenance window for {0}", getSafeComputerName(computerName));
    XmlFile xmlMaintenanceFile = getMaintenanceWindowsFile(computerName);
    xmlMaintenanceFile.write(md);
  }

  private XmlFile getMaintenanceWindowsFile(String computerName) throws IOException {
    return new XmlFile(new File(new File(getNodesDirectory(), computerName), "maintenance-windows.xml"));
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
    MaintenanceDefinitions md = cache.get(oldName);
    if (md != null) {
      LOGGER.log(Level.FINEST, "Persisting existing maintenance windows after agent rename");
      cache.remove(oldName);
      cache.put(newName, md);
      try {
        saveMaintenanceWindows(newName, md);
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Failed to persists agent maintenance windows after agent rename {0}", newName);
      }
    }
  }

  public void createAgent(String nodeName) {
    cache.put(nodeName, new MaintenanceDefinitions(new TreeSet<>(), new HashSet<>()));
  }

  /**
   * Inject the agent maintenance retention strategy from the given computer.
   *
   * @param c The computer for which to inject the strategy
   * @return true if strategy was injected, false otherwise
   */
  public boolean injectRetentionStrategy(Computer c) {
    if (c instanceof SlaveComputer) {
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
    if (c instanceof SlaveComputer) {
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
            XmlFile maintenanceFile = getMaintenanceWindowsFile(computerName);
            if (maintenanceFile.exists()) {
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


  /**
   * Parses a duration like string into an integer with the corresponding minutes.
   * Takes a plain number or any combination of <code>&lt;int&gt;&lt;unit&gt;</code> with unit
   * being one of <code>m (minutes)</code>, <code>h (hours)</code> or <code>d (days)</code>.
   * Each unit must appear not more than once.
   *
   * @param input The string to parse
   * @return the parsed minutes
   */
  public static int parseDurationString(String input) {
    Pattern dayRegex = Pattern.compile("(\\d{1,4})d");
    Pattern hourRegex = Pattern.compile("(\\d{1,2})h");
    Pattern minRegex = Pattern.compile("(\\d{1,2})m");

    Matcher dayMatch = dayRegex.matcher(input);
    Matcher hourMatch = hourRegex.matcher(input);
    Matcher minMatch = minRegex.matcher(input);

    boolean hourMatched = hourMatch.find();
    boolean minMatched = minMatch.find();
    boolean dayMatched = dayMatch.find();
    int waitMinutes;

    if (hourMatched || minMatched || dayMatched) {
      int hour = 0;
      int min = 0;
      int day = 0;

      if (dayMatched) {
        day = Integer.parseInt(dayMatch.group(1));
      }

      if (hourMatched) {
        hour = Integer.parseInt(hourMatch.group(1));
      }

      if (minMatched) {
        min = Integer.parseInt(minMatch.group(1));
      }

      return day * 60 * 24 + hour * 60 + min;
    }
    try {
      waitMinutes = Integer.parseInt(input);
    } catch (NumberFormatException nfe) {
      return -1;
    }
    return waitMinutes;
  }

}
