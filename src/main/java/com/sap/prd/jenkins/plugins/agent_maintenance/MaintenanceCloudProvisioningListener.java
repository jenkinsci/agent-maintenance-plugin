package com.sap.prd.jenkins.plugins.agent_maintenance;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.queue.CauseOfBlockage;
import hudson.slaves.Cloud;
import hudson.slaves.CloudProvisioningListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
/**
 * Blocks cloud provisioning when a maintenance window is active.
 */
@Extension
public class MaintenanceCloudProvisioningListener extends CloudProvisioningListener {

  private static final Logger LOGGER =
      Logger.getLogger(MaintenanceCloudProvisioningListener.class.getName());

  @Override
  @CheckForNull
  public CauseOfBlockage canProvision(
      Cloud cloud,
      Cloud.CloudState state,
      int numExecutors) {

    MaintenanceHelper helper = MaintenanceHelper.getInstance();

    for (Computer computer : Jenkins.get().getComputers()) {
      try {
        if (helper.hasActiveMaintenanceWindows(computer.getName())) {
          LOGGER.log(
              Level.INFO,
              "Blocking cloud provisioning due to active maintenance window on agent {0}",
              computer.getName());

          return new CauseOfBlockage() {
            @Override
            public String getShortDescription() {
              return "Cloud provisioning blocked due to active maintenance window";
            }
          };
        }
      } catch (IOException e) {
        LOGGER.log(
            Level.WARNING,
            "Failed to check maintenance windows for agent {0}",
            computer.getName());
      }
    }

    return null; // provisioning allowed
  }
}
