package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.Extension;
import hudson.model.Node;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.Queue;
import hudson.model.Label;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;

@Extension
public class CloudMaintenanceQueueBlocker extends QueueTaskDispatcher {
  @Override
  public CauseOfBlockage canRun(Queue.Item item) {
    Label assignedLabel = item.getAssignedLabel();
    if (assignedLabel == null) {
      return null;
    }

    for (Cloud cloud : Jenkins.get().clouds) {
      // Universal: Check if cloud can provision this label
      try {
        String key = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, cloud.name).toKey();
        if (MaintenanceHelper.getInstance().hasActiveMaintenanceWindows(key)
                && cloud.canProvision(assignedLabel)) {
          return new CauseOfBlockage() {
            @Override
            public String getShortDescription() {
              return "Cloud '" + cloud.name + "' in maintenance";
            }
          };
        }
      } catch (Exception ignored) {}
    }
    return null;
  }

  @Override
  public CauseOfBlockage canTake(Node node, Queue.BuildableItem item) {
    return canRun(item);
  }
}
