package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import hudson.model.Node;
import hudson.slaves.RetentionStrategy;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * Tests that the old data format for maintenance windows is properly read.
 */
@WithJenkins
class MigrationTest extends BaseIntegrationTest {

  @Test
  @LocalData
  void readOldData() throws IOException {
    MaintenanceHelper.getInstance().clearCache();
    System.out.println("=== MIGRATION TEST DEBUG ===");

    Node agent = rule.jenkins.getNode("agent");
    System.out.println("Agent: " + agent);
    System.out.println("Agent name: " + (agent != null ? agent.getNodeName() : "NULL"));

    MaintenanceTarget target = getTarget(MaintenanceTarget.TargetType.AGENT, agent.getNodeName());
    System.out.println("Target: " + target);
    System.out.println("Target key: " + target.toKey());

    // Check if file exists
    java.io.File jenkinsHome = rule.jenkins.getRootDir();
    System.out.println("Jenkins home: " + jenkinsHome);

    java.io.File nodesDir = new java.io.File(jenkinsHome, "nodes");
    System.out.println("Nodes dir exists: " + nodesDir.exists());

    java.io.File agentDir = new java.io.File(nodesDir, agent.getNodeName());
    System.out.println("Agent dir exists: " + agentDir.exists());

    java.io.File maintenanceFile = new java.io.File(agentDir, "maintenance-windows.xml");
    System.out.println("Maintenance file exists: " + maintenanceFile.exists());

    if (maintenanceFile.exists()) {
      System.out.println("File size: " + maintenanceFile.length() + " bytes");
    }

    // Try to load
    boolean hasWindows = MaintenanceHelper.getInstance().hasMaintenanceWindows(target.toKey());
    System.out.println("hasMaintenanceWindows result: " + hasWindows);

    try {
      MaintenanceDefinitions md = MaintenanceHelper.getInstance().getMaintenanceDefinitions(target.toKey());
      System.out.println("MaintenanceDefinitions: " + md);
      System.out.println("Scheduled count: " + (md != null ? md.getScheduled().size() : "NULL"));
    } catch (Exception e) {
      System.out.println("ERROR loading: " + e.getMessage());
      e.printStackTrace();
    }

    System.out.println("=== END DEBUG ===");

    RetentionStrategy retentionStrategy = agent.toComputer().getRetentionStrategy();
    assertThat(retentionStrategy, instanceOf(AgentMaintenanceRetentionStrategy.class));
    assertThat(MaintenanceHelper.getInstance().hasMaintenanceWindows2(target.toKey()), is(true));
  }
}
