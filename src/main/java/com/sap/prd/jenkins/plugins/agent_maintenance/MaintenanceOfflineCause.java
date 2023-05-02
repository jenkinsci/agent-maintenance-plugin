package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.slaves.OfflineCause;

/** Offline cause because of a maintenance. */
public class MaintenanceOfflineCause extends OfflineCause {

  private final String maintenanceId;
  private final String computerName;

  public MaintenanceOfflineCause(String maintenanceId, String computerName) {
    this.maintenanceId = maintenanceId;
    this.computerName = computerName;
  }

  private MaintenanceWindow getMaintenanceWindow() {
    return MaintenanceHelper.getInstance().getMaintenanceWindow(computerName, maintenanceId);
  }
  public String getStartTime() {
    MaintenanceWindow maintenanceWindow = getMaintenanceWindow();
    if (maintenanceWindow == null) {
      return "not found";
    }
    return maintenanceWindow.getStartTime();
  }

  public String getEndTime() {
    MaintenanceWindow maintenanceWindow = getMaintenanceWindow();
    if (maintenanceWindow == null) {
      return "not found";
    }
    return maintenanceWindow.getEndTime();
  }

  public String getReason() {
    MaintenanceWindow maintenanceWindow = getMaintenanceWindow();
    if (maintenanceWindow == null) {
      return "not found";
    }
    return maintenanceWindow.getReason();
  }

  public boolean isTakeOnline() {
    MaintenanceWindow maintenanceWindow = getMaintenanceWindow();
    if (maintenanceWindow == null) {
      return true;
    }
    return maintenanceWindow.isTakeOnline();
  }

  @Override
  public String toString() {
    return getReason();
  }
}
