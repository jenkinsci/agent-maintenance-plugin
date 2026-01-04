package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.model.Computer;
import hudson.model.Slave;
import hudson.slaves.Cloud;
import hudson.slaves.RetentionStrategy.Always;
import hudson.slaves.RetentionStrategy.Demand;
import org.jenkinsci.plugins.matrixauth.AuthorizationMatrixNodeProperty;
import org.junit.jupiter.api.BeforeEach;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;

/** Base class for permission checks. */
@WithJenkins
abstract class BasePermissionChecks extends PermissionSetup {

  // Agent fields
  protected static Slave agent;
  protected static Slave agentRestricted;
  protected static String maintenanceId;
  protected static String maintenanceIdRestricted;
  protected static String maintenanceIdToDelete;
  protected static MaintenanceWindow maintenanceWindow;
  protected static MaintenanceWindow maintenanceWindowRestricted;
  protected static MaintenanceWindow maintenanceWindowToDelete;
  protected static String agentMaintenanceUrl;

  // Cloud fields
  protected static Cloud cloud;
  protected static String cloudMaintenanceId;
  protected static String cloudMaintenanceIdToDelete;
  protected static MaintenanceWindow cloudMaintenanceWindow;
  protected static MaintenanceWindow cloudMaintenanceWindowToDelete;
  protected static String cloudMaintenanceUrl;

  protected static MaintenanceWindow createMaintenanceWindow(Slave agent, String reason) throws IOException {

    MaintenanceWindow maintenanceWindow =
        new MaintenanceWindow(
            "1970-01-01 11:00", "2099-12-31 23:59", reason, true, true, "10", CONFIGURE, null);
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.AGENT, agent.getNodeName());
    MaintenanceHelper.getInstance().addMaintenanceWindow(target.toKey(), maintenanceWindow);
    return maintenanceWindow;
  }

  protected static MaintenanceWindow createMaintenanceWindow(Cloud cloud, String reason) throws IOException {
    MaintenanceWindow maintenanceWindow =
            new MaintenanceWindow("1970-01-01 11:00", "2099-12-31 23:59", reason);
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, cloud.name);
    MaintenanceHelper.getInstance().addMaintenanceWindow(target.toKey(), maintenanceWindow);
    return maintenanceWindow;
  }

  /**
   * Setup tests.
   *
   * @throws Exception when something goes wrong
   */
  @BeforeEach
  public void setupAgents() throws Exception {
    agent = rule.createOnlineSlave();
    agentRestricted = rule.createOnlineSlave();
    agent.setRetentionStrategy(new AgentMaintenanceRetentionStrategy(new Demand(1, 2)));
    agentRestricted.setRetentionStrategy(new AgentMaintenanceRetentionStrategy(new Always()));

    maintenanceWindow = createMaintenanceWindow(agent, "test");

    maintenanceId = maintenanceWindow.getId();
    agentMaintenanceUrl = agent.toComputer().getUrl() + "maintenanceWindows";

    maintenanceWindowRestricted = createMaintenanceWindow(agentRestricted, "test Restricted");
    maintenanceIdRestricted = maintenanceWindowRestricted.getId();

    maintenanceWindowToDelete = createMaintenanceWindow(agent, "test to delete");
    maintenanceIdToDelete = maintenanceWindowToDelete.getId();

    AuthorizationMatrixNodeProperty nodeProp = new AuthorizationMatrixNodeProperty();
    AuthorizationMatrixNodeProperty nodePropRestricted = new AuthorizationMatrixNodeProperty();

    // System read and computer configure on agent, but not on agentRestricted
    nodeProp.add(Computer.CONFIGURE, configure);
    nodeProp.add(Computer.DISCONNECT, disconnect);
    nodePropRestricted.add(Computer.EXTENDED_READ, configure);

    agent.getNodeProperties().add(nodeProp);
    agentRestricted.getNodeProperties().add(nodePropRestricted);
  }

  /**
   * Setting up test clouds.
   *
   * @throws IOException when something goes wrong
   */
  @BeforeEach
  public void setupClouds() throws IOException {
    cloud = new TestCloud("test-cloud");

    cloudMaintenanceWindow = createMaintenanceWindow(cloud, "Test");

    cloudMaintenanceId = cloudMaintenanceWindow.getId();
    cloudMaintenanceUrl = cloud.getUrl() + "maintenanceWindows";

    cloudMaintenanceWindowToDelete = createMaintenanceWindow(cloud, "Test to delete");
    cloudMaintenanceIdToDelete = cloudMaintenanceWindowToDelete.getId();
  }

  protected String getMaintenanceUrl(MaintenanceTarget.TargetType type) {
    return (type == MaintenanceTarget.TargetType.AGENT) ? agentMaintenanceUrl : cloudMaintenanceUrl;
  }

  protected String getMaintenanceId(MaintenanceTarget.TargetType type) {
    return (type == MaintenanceTarget.TargetType.AGENT) ? maintenanceId : cloudMaintenanceId;
  }

  protected String getMaintenanceIdToDelete(MaintenanceTarget.TargetType type) {
    return (type == MaintenanceTarget.TargetType.AGENT) ? maintenanceIdToDelete : cloudMaintenanceIdToDelete;
  }

  protected MaintenanceWindow getMaintenanceWindow(MaintenanceTarget.TargetType type) {
    return (type == MaintenanceTarget.TargetType.AGENT) ? maintenanceWindow : cloudMaintenanceWindow;
  }

  protected MaintenanceWindow getMaintenanceWindowToDelete(MaintenanceTarget.TargetType type) {
    return (type == MaintenanceTarget.TargetType.AGENT) ? maintenanceWindowToDelete : cloudMaintenanceWindowToDelete;
  }

  protected MaintenanceTarget createTarget(MaintenanceTarget.TargetType type) {
    String name = (type == MaintenanceTarget.TargetType.AGENT) ? agent.getNodeName() : cloud.name;
    return new MaintenanceTarget(type, name);
  }
}
