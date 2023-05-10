package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.slaves.OfflineCause;

/** Offline cause because of a maintenance. */
public class MaintenanceOfflineCause extends OfflineCause {

  private MaintenanceWindow maintenanceWindow;
  private final String computerName;

  public MaintenanceOfflineCause(MaintenanceWindow maintenanceWindow, String computerName) {
    this.maintenanceWindow = maintenanceWindow;
    this.computerName = computerName;
  }

  private void updateMaintenanceWindow() {
    MaintenanceWindow newMaintenanceWindow = MaintenanceHelper.getInstance().getMaintenanceWindow(computerName, maintenanceWindow.getId());
    if (newMaintenanceWindow != null) {
      maintenanceWindow = newMaintenanceWindow;
    }
  }

  public String getStartTime() {
    updateMaintenanceWindow();
    return maintenanceWindow.getStartTime();
  }

  public String getEndTime() {
    updateMaintenanceWindow();
    return maintenanceWindow.getEndTime();
  }

  public String getReason() {
    updateMaintenanceWindow();
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
