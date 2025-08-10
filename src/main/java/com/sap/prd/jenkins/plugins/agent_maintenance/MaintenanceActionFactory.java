package com.sap.prd.jenkins.plugins.agent_maintenance;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.SlaveComputer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jenkins.model.TransientActionFactory;

/** Inject the action link to agents. */
@Extension
public class MaintenanceActionFactory extends TransientActionFactory<SlaveComputer> {

  @Override
  @NonNull
  public Collection<? extends Action> createFor(@NonNull SlaveComputer target) {
    List<Action> result = new ArrayList<>();
    if (!(target instanceof AbstractCloudComputer)
        && target.getActions().stream().noneMatch(x -> x instanceof MaintenanceAction)) {
      MaintenanceAction action = new MaintenanceAction(target);
      result.add(action);
      target.addAction(new MaintenanceAction(target));
    }
    return result;
  }

  @Override
  public Class<SlaveComputer> type() {
    return SlaveComputer.class;
  }
}
