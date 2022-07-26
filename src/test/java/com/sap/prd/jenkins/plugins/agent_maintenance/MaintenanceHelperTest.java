package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import hudson.model.Slave;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Test the helper.
 */
public class MaintenanceHelperTest {

  @Rule
  public JenkinsRule rule = new JenkinsRule();

  private MaintenanceHelper helper = MaintenanceHelper.getInstance();

  @Test
  public void getMaintenanceWindowsExistingAgent() throws Exception {
    Slave agent = rule.createOnlineSlave();
    String agentName = agent.getNodeName();
    Set<MaintenanceWindow> mwSet = helper.getMaintenanceWindows(agentName);
    assertThat(mwSet.size(), is(0));
    MaintenanceWindow mw = new MaintenanceWindow("1970-01-01 11:00", "2099-12-31 23:59", "test", true, true, "10", "user", null);
    mwSet.add(mw);
    mwSet = helper.getMaintenanceWindows(agentName);
    assertThat(mwSet.size(), is(1));
  }

  @Test
  public void getMaintenanceWindowsNonExistingAgent() throws Exception {
    String agentName = "notExisting";
    Set<MaintenanceWindow> mwSet = helper.getMaintenanceWindows(agentName);
    assertThat(helper.getMaintenanceWindows(agentName).size(), is(0));
    assertThat(mwSet.size(), is(0));
    MaintenanceWindow mw = new MaintenanceWindow("1970-01-01 11:00", "2099-12-31 23:59", "test", true, true, "10", "user", null);
    mwSet.add(mw);
    mwSet = helper.getMaintenanceWindows(agentName);
    assertThat(mwSet.size(), is(0));
  }
}
