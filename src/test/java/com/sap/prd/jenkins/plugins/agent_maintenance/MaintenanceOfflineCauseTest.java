package com.sap.prd.jenkins.plugins.agent_maintenance;

import static com.sap.prd.jenkins.plugins.agent_maintenance.MaintenanceWindow.DATE_FORMATTER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import hudson.model.Slave;
import hudson.slaves.RetentionStrategy;
import java.time.LocalDateTime;
import java.util.SortedSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** Tests the offline cause. */
@WithJenkins
class MaintenanceOfflineCauseTest extends BaseIntegrationTest {

  private Slave agent;

  /**
   * Setup agent.
   *
   * @throws Exception in case of an error
   */
  @Override
  @BeforeEach
  void setup(JenkinsRule rule) throws Exception {
    super.setup(rule);
    agent = rule.createOnlineSlave();
    AgentMaintenanceRetentionStrategy strategy =
        new AgentMaintenanceRetentionStrategy(new RetentionStrategy.Always());
    agent.setRetentionStrategy(strategy);
  }

  @Test
  void offlineCauseIsUpdated() throws Exception {
    LocalDateTime start = LocalDateTime.now().minusMinutes(1);
    LocalDateTime end = start.plusMinutes(5);
    MaintenanceWindow mw =
        new MaintenanceWindow(
            start.format(DATE_FORMATTER),
            end.format(DATE_FORMATTER),
            "test",
            true,
            true,
            "5",
            "test",
            null);
    maintenanceHelper.addMaintenanceWindow(agent.getNodeName(), mw);
    MaintenanceOfflineCause moc = (MaintenanceOfflineCause) mw.getOfflineCause(agent.getNodeName());
    assertThat(moc.getReason(), is("test"));
    MaintenanceWindow updated =
        new MaintenanceWindow(
            start.format(DATE_FORMATTER),
            end.format(DATE_FORMATTER),
            "changed",
            true,
            true,
            "5",
            "test",
            mw.getId());

    MaintenanceDefinitions mwdefinitions = MaintenanceHelper.getInstance().getMaintenanceDefinitions(agent.getNodeName());
    synchronized (mwdefinitions) {
      SortedSet<MaintenanceWindow> mwList = mwdefinitions.getScheduled();
      mwList.clear();
      mwList.add(updated);
      MaintenanceHelper.getInstance().saveMaintenanceWindows(agent.getNodeName(), mwdefinitions);
    }
    assertThat(moc.getReason(), is("changed"));
  }
}
