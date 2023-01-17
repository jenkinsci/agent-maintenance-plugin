package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import hudson.model.Slave;
import org.junit.Test;

/** Test recurring maintenance windows. */
public class RecurringTest extends BaseIntegationTest {

  @Test
  public void recurringMaintenanceInjectsMaintenance() throws Exception {
    String agentName = "recurring";
    Slave agent = getAgent(agentName);
    RecurringMaintenanceWindow rw = new RecurringMaintenanceWindow("0 2 * * *",
        "test", true, true, "10m", "60m",  "test", null, 0);
    maintenanceHelper.addRecurringMaintenanceWindow(agent.getNodeName(), rw);
    triggerCheckCycle(agent);
    assertThat(maintenanceHelper.hasMaintenanceWindows(agentName), is(true));
  }
}
