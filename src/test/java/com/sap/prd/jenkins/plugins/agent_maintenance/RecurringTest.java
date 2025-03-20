package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import hudson.model.Slave;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** Test recurring maintenance windows. */
@WithJenkins
class RecurringTest extends BaseIntegrationTest {

  @Test
  void recurringMaintenanceInjectsMaintenance() throws Exception {
    String agentName = "recurring";
    Slave agent = getAgent(agentName);
    RecurringMaintenanceWindow rw = new RecurringMaintenanceWindow("0 2 * * *",
        "test", true, true, "10m", "60m",  "test", null, 0);
    maintenanceHelper.addRecurringMaintenanceWindow(agent.getNodeName(), rw);
    triggerCheckCycle(agent);
    assertThat(maintenanceHelper.hasMaintenanceWindows(agentName), is(true));
  }
}
