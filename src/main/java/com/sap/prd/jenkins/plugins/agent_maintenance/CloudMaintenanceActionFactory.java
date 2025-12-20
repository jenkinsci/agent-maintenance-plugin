package com.sap.prd.jenkins.plugins.agent_maintenance;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import jenkins.model.TransientActionFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Extension
public class CloudMaintenanceActionFactory extends TransientActionFactory<Cloud> {

  @NonNull
  @Override
  public Collection<? extends Action> createFor(@NonNull Cloud target) {
    if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
      return Collections.emptyList();
    }
    MaintenanceTarget mt = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, target.name);
    MaintenanceAction action = new MaintenanceAction(mt);
//    target.addAction(action);

    return Collections.singletonList(action);
  }

  @Override
  public Class<Cloud> type() {
    return Cloud.class;
  }
}
