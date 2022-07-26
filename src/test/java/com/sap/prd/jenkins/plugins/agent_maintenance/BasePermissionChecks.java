package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.model.Computer;
import hudson.model.Slave;
import hudson.slaves.RetentionStrategy.Always;
import hudson.slaves.RetentionStrategy.Demand;
import java.io.IOException;
import org.jenkinsci.plugins.matrixauth.AuthorizationMatrixNodeProperty;
import org.junit.Before;

/** Base class for permission checks. */
public abstract class BasePermissionChecks extends PermissionSetup {

  protected static Slave agent;
  protected static Slave agentRestricted;
  protected static String maintenanceId;
  protected static String maintenanceIdRestricted;
  protected static String maintenanceIdToDelete;
  protected static MaintenanceWindow maintenanceWindow;
  protected static MaintenanceWindow maintenanceWindowRestricted;
  protected static MaintenanceWindow maintenanceWindowToDelete;
  protected static String agentMaintenanceUrl;

  protected static MaintenanceWindow createMaintenanceWindow(Slave agent) throws IOException {

    MaintenanceWindow maintenanceWindow =
        new MaintenanceWindow(
            "1970-01-01 11:00", "2099-12-31 23:59", "test", true, true, "10", CONFIGURE, null);
    MaintenanceHelper.getInstance().addMaintenanceWindow(agent.getNodeName(), maintenanceWindow);
    return maintenanceWindow;
  }

  /**
   * Setup tests.
   *
   * @throws Exception when something goes wrong
   */
  @Before
  public void setupAgents() throws Exception {
    agent = rule.createOnlineSlave();
    agentRestricted = rule.createOnlineSlave();
    agent.setRetentionStrategy(new AgentMaintenanceRetentionStrategy(new Demand(1, 2)));
    agentRestricted.setRetentionStrategy(new AgentMaintenanceRetentionStrategy(new Always()));

    maintenanceWindow = createMaintenanceWindow(agent);

    maintenanceId = maintenanceWindow.getId();
    agentMaintenanceUrl = agent.toComputer().getUrl() + "maintenanceWindows";

    maintenanceWindowRestricted = createMaintenanceWindow(agentRestricted);
    maintenanceIdRestricted = maintenanceWindowRestricted.getId();

    maintenanceWindowToDelete = createMaintenanceWindow(agent);
    maintenanceIdToDelete = maintenanceWindowToDelete.getId();

    AuthorizationMatrixNodeProperty nodeProp = new AuthorizationMatrixNodeProperty();
    AuthorizationMatrixNodeProperty nodePropRestricted = new AuthorizationMatrixNodeProperty();

    // System read and computer configure on agent, but not on agentRestricted
    nodeProp.add(Computer.CONFIGURE, CONFIGURE);
    nodeProp.add(Computer.DISCONNECT, DISCONNECT);
    nodePropRestricted.add(Computer.EXTENDED_READ, CONFIGURE);

    agent.getNodeProperties().add(nodeProp);
    agentRestricted.getNodeProperties().add(nodePropRestricted);
  }
}
