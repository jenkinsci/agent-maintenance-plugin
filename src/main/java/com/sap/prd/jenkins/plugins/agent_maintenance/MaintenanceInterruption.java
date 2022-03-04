package com.sap.prd.jenkins.plugins.agent_maintenance;

import jenkins.model.CauseOfInterruption;

/** Agent is down for maintenance. */
public class MaintenanceInterruption extends CauseOfInterruption {
  private static final long serialVersionUID = 1L;

  @Override
  public String getShortDescription() {
    return "Agent is going down for scheduled maintenance";
  }
}
