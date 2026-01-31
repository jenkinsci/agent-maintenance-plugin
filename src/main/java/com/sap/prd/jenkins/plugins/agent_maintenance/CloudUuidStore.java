package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.model.Action;
import hudson.slaves.Cloud;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 * Utility class to store and retrieve UUIDs for duplicate clouds.
 */
public final class CloudUuidStore {

  private static final CloudUuidStore INSTANCE = new CloudUuidStore();
  private static final Logger LOGGER = Logger.getLogger(CloudUuidStore.class.getName());

  private CloudUuidStore() {}

  public static CloudUuidStore getInstance() {
    return INSTANCE;
  }

  public String getOrCreateUuid(Cloud cloud) {
    String name = cloud.name;
    CloudUuidAction persistentAction = getPersistentAction(cloud);
    if (!hasDuplicates(name)) {
      if (persistentAction != null) {
        // Remove persisted action when duplicacy no longer exists
        cloud.getActions().remove(persistentAction);
        saveConfig();
      }
      return null;
    }

    if (persistentAction != null) {
      return persistentAction.getUuid();
    }

    String uuid = UUID.randomUUID().toString();
    CloudUuidAction newAction = new CloudUuidAction(uuid);
    cloud.getActions().add(newAction);
    saveConfig();

    return uuid;
  }

  public String getUuidIfPresent(Cloud cloud) {
    CloudUuidAction action = getPersistentAction(cloud);
    if (action != null) {
      return action.getUuid();
    }
    return null;
  }

  /**
   * Removes the persisted CloudUuidAction from a cloud if present.
   * Used when a cloud no longer needs UUID tracking -> no more duplicates.
   *
   * @param cloud The cloud
   * @return true if action was removed, false if not present
   */
  public boolean removeUuidIfPresent(Cloud cloud) {
    CloudUuidAction action = getPersistentAction(cloud);
    if (action != null) {
      cloud.getActions().remove(action);
      saveConfig();
      return true;
    }
    return false;
  }

  /**
   * Gets the CloudUuidAction.
   *
   * @param cloud The cloud
   * @return Persistent CloudUuidAction if present, null otherwise
   */
  private CloudUuidAction getPersistentAction(Cloud cloud) {
    var actions = cloud.getActions();
    for (Action a : actions) {
      if (a instanceof CloudUuidAction cua) {
        return cua;
      }
    }
    return null;
  }

  private void saveConfig() {
    try {
      Jenkins.get().save();
    } catch (IOException e) {
      LOGGER.warning("Failed to save configuration for cloud UUID.");
    }
  }

  public Cloud getCloudByTarget(MaintenanceTarget target) {
    if (target.getType() != MaintenanceTarget.TargetType.CLOUD) {
      throw new IllegalArgumentException("Target type must be CLOUD");
    }

    String name = target.getName();
    String uuid = target.getUuid();
    Jenkins j = Jenkins.get();
    if (uuid == null) {
      return j.getCloud(name);
    }

    for (Cloud cloud : j.clouds) {
      if (name.equals(cloud.name) && uuid.equals(getUuidIfPresent(cloud))) {
        return cloud;
      }
    }

    LOGGER.warning("Could not find cloud with name " + name + " and UUID " + uuid);
    return null;
  }

  /**
   * Counts the number of clouds with the given name.
   *
   * @param cloudName Name of the cloud
   * @return Number of clouds with the given name
   */
  public int cloudDuplicates(String cloudName) {
    Jenkins j = Jenkins.get();
    return (int) j.clouds.stream()
        .filter(c -> Objects.equals(c.name, cloudName))
        .count();
  }

  /**
   * Checks if there are duplicate clouds.
   */
  public boolean hasDuplicates(String cloudName) {
    return cloudDuplicates(cloudName) > 1;
  }
}
