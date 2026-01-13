package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import hudson.model.Slave;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Test the helper.
 */
@WithJenkins
class MaintenanceHelperTest extends BaseIntegrationTest {

  @Test
  void getMaintenanceWindowsExistingAgent() throws Exception {
    Slave agent = rule.createOnlineSlave();
    String agentName = agent.getNodeName();
    Set<MaintenanceWindow> mwSet = maintenanceHelper.getMaintenanceWindows(agentName);
    assertThat(mwSet.size(), is(0));
    MaintenanceWindow mw = new MaintenanceWindow("1970-01-01 11:00", "2099-12-31 23:59", "test", true, true, "10", "user", null);
    mwSet.add(mw);
    mwSet = maintenanceHelper.getMaintenanceWindows(agentName);
    assertThat(mwSet.size(), is(1));
  }

  @Test
  void getMaintenanceWindowsNonExistingAgent() throws Exception {
    String agentName = "notExisting";
    Set<MaintenanceWindow> mwSet = maintenanceHelper.getMaintenanceWindows(agentName);
    assertThat(mwSet.size(), is(0));
    MaintenanceWindow mw = new MaintenanceWindow("1970-01-01 11:00", "2099-12-31 23:59", "test", true, true, "10", "user", null);
    mwSet.add(mw);
    mwSet = maintenanceHelper.getMaintenanceWindows(agentName);
    assertThat(mwSet.size(), is(0));
  }

  @Test
  void parseDurationString() {
    assertThat(MaintenanceHelper.parseDurationString("10"), is(10));
    assertThat(MaintenanceHelper.parseDurationString("10m"), is(10));
    assertThat(MaintenanceHelper.parseDurationString("1h"), is(60));
    assertThat(MaintenanceHelper.parseDurationString("2h 10m"), is(130));
    assertThat(MaintenanceHelper.parseDurationString("1d 1h 30m"), is(1530));
  }
}
