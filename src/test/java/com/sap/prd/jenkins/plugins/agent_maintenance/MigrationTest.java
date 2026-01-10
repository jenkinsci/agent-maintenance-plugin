package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import hudson.model.Node;
import hudson.slaves.RetentionStrategy;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;
import java.io.IOException;

/**
 * Tests that the old data format for maintenance windows is properly read.
 */
@WithJenkins
class MigrationTest extends BaseIntegrationTest {

  @Test
  @LocalData
  void readOldData() throws IOException {
    MaintenanceHelper.getInstance().clearCache();
    Node agent = rule.jenkins.getNode("agent");
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.AGENT, "agent");
    RetentionStrategy retentionStrategy = agent.toComputer().getRetentionStrategy();
    assertThat(retentionStrategy, instanceOf(AgentMaintenanceRetentionStrategy.class));
    assertThat(MaintenanceHelper.getInstance().hasMaintenanceWindows(target.toKey()), is(true));
  }
}
