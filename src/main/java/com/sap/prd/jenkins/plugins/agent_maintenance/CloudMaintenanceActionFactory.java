package com.sap.prd.jenkins.plugins.agent_maintenance;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.slaves.Cloud;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.TransientActionFactory;

/**
 * Injects the action link to all Clouds.
 */
@Extension
public class CloudMaintenanceActionFactory extends TransientActionFactory<Cloud> {

  private static final Logger LOGGER = Logger.getLogger(CloudMaintenanceActionFactory.class.getName());

  @NonNull
  @Override
  public Collection<? extends Action> createFor(@NonNull Cloud target) {
    try {
      String uuid = CloudUuidStore.getInstance().getOrCreateUuid(target);
      MaintenanceTarget mt = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, target.name, uuid);
      MaintenanceAction action = new MaintenanceAction(mt);

      return Collections.singletonList(action);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to resolve UUID for cloud: " + target.name, e);
      return Collections.emptyList();
    }
  }

  @Override
  public Class<Cloud> type() {
    return Cloud.class;
  }
}
