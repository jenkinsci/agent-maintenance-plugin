package com.sap.prd.jenkins.plugins.agent_maintenance;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.slaves.Cloud;
import java.util.Collection;
import java.util.Collections;
import jenkins.model.TransientActionFactory;

/**
 * Injects the action link to all Clouds.
 */
@Extension
public class CloudMaintenanceActionFactory extends TransientActionFactory<Cloud> {

  @NonNull
  @Override
  public Collection<? extends Action> createFor(@NonNull Cloud target) {
    MaintenanceTarget mt = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, target.name);
    MaintenanceAction action = new MaintenanceAction(mt);

    return Collections.singletonList(action);
  }

  @Override
  public Class<Cloud> type() {
    return Cloud.class;
  }
}
