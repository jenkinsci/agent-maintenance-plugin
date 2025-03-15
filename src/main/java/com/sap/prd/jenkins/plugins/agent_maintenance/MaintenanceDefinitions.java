package com.sap.prd.jenkins.plugins.agent_maintenance;

import java.util.Set;
import java.util.SortedSet;

/**
 * Container that holds the scheduled and recurring maintenance windows for an agent.
 */
public class MaintenanceDefinitions {
  private final SortedSet<MaintenanceWindow> scheduled;
  private final Set<RecurringMaintenanceWindow> recurring;

  /**
   * Create definitions container.
   *
   * @param scheduled A set of scheduled maintenance windows
   * @param recurring A set of recurring maintenance windows
   */
  public MaintenanceDefinitions(SortedSet<MaintenanceWindow> scheduled, Set<RecurringMaintenanceWindow> recurring) {
    this.scheduled = scheduled;
    this.recurring = recurring;
  }

  /**
   * Get the scheduled maintenance windows.
   *
   * @return Set of scheduled maintenance windows
   */
  public SortedSet<MaintenanceWindow> getScheduled() {
    return scheduled;
  }

  /**
   * Get the recurring maintenance windows.
   *
   * @return Set of recurring maintenance windows
   */
  public Set<RecurringMaintenanceWindow> getRecurring() {
    return recurring;
  }
}
