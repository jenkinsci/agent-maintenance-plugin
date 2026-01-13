package com.sap.prd.jenkins.plugins.agent_maintenance;

import static com.sap.prd.jenkins.plugins.agent_maintenance.MaintenanceWindow.DATE_FORMATTER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import hudson.model.Slave;
import hudson.slaves.OfflineCause;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Integration tests for the maintenance strategy. */
class AgentMaintenanceRetentionStrategyTest extends BaseIntegrationTest {

  private void waitForMaintenanceEnd(MaintenanceWindow mw, Slave agent) throws InterruptedException {
    while (mw.isMaintenanceScheduled()) {
      TimeUnit.SECONDS.sleep(10);
    }
    triggerCheckCycle(agent);
  }

  @Test
  @Timeout(600)
  void activeMaintenanceWindow() throws Exception {
    Slave agent = getAgent("activeMaintenanceWindow");
    LocalDateTime start = LocalDateTime.now().minusMinutes(1);
    LocalDateTime end = start.plusMinutes(2);
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
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.AGENT, agent.getNodeName());
    assertThat(agent.toComputer().isAcceptingTasks(), is(true));
    assertThat(agent.toComputer().isManualLaunchAllowed(), is(true));
    maintenanceHelper.addMaintenanceWindow(target.toKey(), mw);
    assertThat(agent.toComputer().isAcceptingTasks(), is(false));
    assertThat(agent.toComputer().isManualLaunchAllowed(), is(false));
    waitForMaintenanceEnd(mw, agent);
    assertThat(maintenanceHelper.getMaintenanceWindows(target.toKey()).size(), is(0));
  }

  @Test
  @Timeout(600)
  void agentGetsDisconnected() throws Exception {
    Slave agent = getAgent("agentGetsDisconnected");
    LocalDateTime start = LocalDateTime.now().minusMinutes(1);
    LocalDateTime end = start.plusMinutes(15);
    MaintenanceWindow mw =
        new MaintenanceWindow(
            start.format(DATE_FORMATTER),
            end.format(DATE_FORMATTER),
            "test",
            true,
            false,
            "0",
            "test",
            null);
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.AGENT, agent.getNodeName());
    String id = mw.getId();
    assertThat(agent.getChannel(), is(notNullValue()));
    maintenanceHelper.addMaintenanceWindow(target.toKey(), mw);
    assertThat(mw.isMaintenanceScheduled(), is(true));
    waitForDisconnect(agent, mw);
    assertThat(agent.getChannel(), is(nullValue()));
    maintenanceHelper.deleteMaintenanceWindow(target.toKey(), id);
  }

  @Test
  @Timeout(600)
  void agentComesBackOnline() throws Exception {
    String agentName = "agentComesBackOnline";
    Slave agent = getAgent(agentName);
    LocalDateTime start = LocalDateTime.now().minusMinutes(1);
    // the duration should be sufficiently long, Jenkins is normally calling once
    // per minute
    // the check method of the retention strategy, but it happens sporadically that
    // it takes 2 minutes and then it can happen that the maintenance window is
    // already over
    // when set to only 4 minutes
    LocalDateTime end = start.plusMinutes(15);
    MaintenanceWindow mw =
        new MaintenanceWindow(
            start.format(DATE_FORMATTER),
            end.format(DATE_FORMATTER),
            "test",
            true,
            false,
            "0",
            "test",
            null);
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.AGENT, agentName);
    assertThat(agent.toComputer().isOnline(), is(true));
    maintenanceHelper.addMaintenanceWindow(target.toKey(), mw);
    waitForDisconnect(agent, mw);
    // Instead of waiting for the maintenance window to be over just delete it
    maintenanceHelper.deleteMaintenanceWindow(target.toKey(), mw.getId());
    triggerCheckCycle(agent);
    while (!agent.toComputer().isOnline()) {
      TimeUnit.SECONDS.sleep(10);
    }
    assertThat(agent.toComputer().isOnline(), is(true));
    assertThat(maintenanceHelper.hasMaintenanceWindows(target.toKey()), is(false));
  }

  @Test
  @Timeout(600)
  void agentStaysOffline() throws Exception {
    Slave agent = getAgent("agentStaysOffline");
    LocalDateTime start = LocalDateTime.now().minusMinutes(1);
    LocalDateTime end = start.plusMinutes(15);
    MaintenanceWindow mw =
        new MaintenanceWindow(
            start.format(DATE_FORMATTER),
            end.format(DATE_FORMATTER),
            "test",
            false,
            false,
            "0",
            "test",
            null);
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.AGENT, agent.getNodeName());
    maintenanceHelper.addMaintenanceWindow(target.toKey(), mw);
    waitForDisconnect(agent, mw);
    // Instead of waiting for the maintenance window to be over just delete it
    maintenanceHelper.deleteMaintenanceWindow(target.toKey(), mw.getId());
    triggerCheckCycle(agent);
    TimeUnit.MINUTES.sleep(1);
    assertThat(agent.getChannel(), is(nullValue()));
    OfflineCause oc = agent.toComputer().getOfflineCause();
    assertThat(oc, instanceOf(MaintenanceOfflineCause.class));
    assertThat(((MaintenanceOfflineCause) oc).isTakeOnline(), is(false));
    assertThat(maintenanceHelper.hasMaintenanceWindows(target.toKey()), is(false));
  }
}
