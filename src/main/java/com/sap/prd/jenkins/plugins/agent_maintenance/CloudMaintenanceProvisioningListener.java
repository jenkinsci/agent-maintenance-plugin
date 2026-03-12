package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.Extension;
import hudson.model.queue.CauseOfBlockage;
import hudson.slaves.Cloud;
import hudson.slaves.CloudProvisioningListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Blocks provisioning of nodes on the Cloud during maintenance.
 */
@Extension
public class CloudMaintenanceProvisioningListener extends CloudProvisioningListener {
  private static final Logger LOGGER = Logger.getLogger(CloudMaintenanceProvisioningListener.class.getName());

  @Override
  public CauseOfBlockage canProvision(Cloud cloud, Cloud.CloudState state, int numExecutors) {
    try {
      MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, cloud.name);
      LOGGER.log(Level.FINER, "Checking for Maintenance Window for cloud {0}", cloud.name);

      // Triggers automatic cleanup of expired windows.
      MaintenanceWindow mw = MaintenanceHelper.getInstance().getMaintenance(target.toKey());
      MaintenanceHelper.getInstance().checkRecurring(target.toKey());
      if (MaintenanceHelper.getInstance()
              .hasActiveMaintenanceWindows(target.toKey())) {
        return new CauseOfBlockage() {
          @Override
          public String getShortDescription() {
            return "Cloud is in maintenance: " + cloud.name;
          }
        };
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to check maintenance for cloud "
              + cloud.name, e);
    }
    return null; // Allow provisioning when no active maintenance window
  }
}