package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.model.Action;

/**
 * Persistent action that stores UUID for duplicate clouds.
 * This is a purely technical action --- not shown in UI.
 */
public class CloudUuidAction implements Action {

  private final String uuid;

  public CloudUuidAction(String uuid) {
    this.uuid = uuid;
  }

  public String getUuid() {
    return uuid;
  }

  @Override
  public String getIconFileName() {
    return null;
  }

  @Override
  public String getDisplayName() {
    return null;
  }

  @Override
  public String getUrlName() {
    return null;
  }
}
