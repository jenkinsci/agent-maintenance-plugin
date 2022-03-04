package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.slaves.OfflineCause;

/** Offline cause because of a maintenance. */
public class MaintenanceOfflineCause extends OfflineCause {

  private final MaintenanceWindow maintenanceWindow;

  public MaintenanceOfflineCause(MaintenanceWindow maintenanceWindow) {
    this.maintenanceWindow = maintenanceWindow;
  }

  public String getStartTime() {
    return maintenanceWindow.getStartTime();
  }

  public String getEndTime() {
    return maintenanceWindow.getEndTime();
  }

  public String getReason() {
    return maintenanceWindow.getReason();
  }

  public boolean isTakeOnline() {
    return maintenanceWindow.isTakeOnline();
  }

  @Override
  public String toString() {
    return getReason();
  }
}
