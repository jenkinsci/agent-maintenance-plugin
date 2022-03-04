package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.Extension;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.slaves.AbstractCloudSlave;
import jenkins.model.NodeListener;

/** Listener to react on events for agents. */
@Extension
public class MaintenanceNodeListener extends NodeListener {
  MaintenanceHelper helper = MaintenanceHelper.getInstance();

  @Override
  protected void onCreated(Node node) {
    if (node instanceof Slave && !(node instanceof AbstractCloudSlave)) {
      helper.createAgent(node.getNodeName());
      if (MaintenanceConfiguration.getInstance().isInjectRetentionStrategy()) {
        helper.injectRetentionStrategy(node.toComputer());
      }
    }
  }

  @Override
  protected void onDeleted(Node node) {
    if (node instanceof Slave) {
      helper.deleteAgent(node.getNodeName());
    }
  }

  @Override
  protected void onUpdated(Node oldNode, Node newNode) {
    if (newNode instanceof Slave && !(newNode instanceof AbstractCloudSlave)) {
      if (!oldNode.getNodeName().equals(newNode.getNodeName())) {
        helper.renameAgent(oldNode.getNodeName(), newNode.getNodeName());
      }
      if (MaintenanceConfiguration.getInstance().isInjectRetentionStrategy()) {
        helper.injectRetentionStrategy(newNode.toComputer());
      }
    }
  }
}
