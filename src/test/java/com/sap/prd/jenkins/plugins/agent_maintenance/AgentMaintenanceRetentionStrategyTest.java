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
import org.junit.Test;
import org.jvnet.hudson.test.recipes.WithTimeout;

/** Integration tests for the maintenance strategy. */
public class AgentMaintenanceRetentionStrategyTest extends BaseIntegationTest {

  private void waitForMaintenanceEnd(MaintenanceWindow mw, Slave agent) throws InterruptedException {
    while (mw.isMaintenanceScheduled()) {
      TimeUnit.SECONDS.sleep(10);
    }
    triggerCheckCycle(agent);
  }

  @Test
  @WithTimeout(600)
  public void activeMaintenanceWindow() throws Exception {
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
    assertThat(agent.toComputer().isAcceptingTasks(), is(true));
    assertThat(agent.toComputer().isManualLaunchAllowed(), is(true));
    maintenanceHelper.addMaintenanceWindow(agent.getNodeName(), mw);
    assertThat(agent.toComputer().isAcceptingTasks(), is(false));
    assertThat(agent.toComputer().isManualLaunchAllowed(), is(false));
    waitForMaintenanceEnd(mw, agent);
    assertThat(maintenanceHelper.getMaintenanceWindows(agent.getNodeName()).size(), is(0));
  }

  @Test
  @WithTimeout(600)
  public void agentGetsDisconnected() throws Exception {
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
    String id = mw.getId();
    assertThat(agent.getChannel(), is(notNullValue()));
    maintenanceHelper.addMaintenanceWindow(agent.getNodeName(), mw);
    assertThat(mw.isMaintenanceScheduled(), is(true));
    waitForDisconnect(agent, mw);
    assertThat(agent.getChannel(), is(nullValue()));
    maintenanceHelper.deleteMaintenanceWindow(agent.getNodeName(), id);
  }

  @Test
  @WithTimeout(600)
  public void agentComesBackOnline() throws Exception {
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
    assertThat(agent.toComputer().isOnline(), is(true));
    maintenanceHelper.addMaintenanceWindow(agentName, mw);
    waitForDisconnect(agent, mw);
    // Instead of waiting for the maintenance window to be over just delete it
    maintenanceHelper.deleteMaintenanceWindow(agentName, mw.getId());
    triggerCheckCycle(agent);
    while (!agent.toComputer().isOnline()) {
      TimeUnit.SECONDS.sleep(10);
    }
    assertThat(agent.toComputer().isOnline(), is(true));
    assertThat(maintenanceHelper.hasMaintenanceWindows(agentName), is(false));
  }

  @Test
  @WithTimeout(600)
  public void agentStaysOffline() throws Exception {
    String agentName = "agentStaysOffline";
    Slave agent = getAgent(agentName);
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
    maintenanceHelper.addMaintenanceWindow(agentName, mw);
    waitForDisconnect(agent, mw);
    // Instead of waiting for the maintenance window to be over just delete it
    maintenanceHelper.deleteMaintenanceWindow(agentName, mw.getId());
    triggerCheckCycle(agent);
    TimeUnit.MINUTES.sleep(1);
    assertThat(agent.getChannel(), is(nullValue()));
    OfflineCause oc = agent.toComputer().getOfflineCause();
    assertThat(oc, instanceOf(MaintenanceOfflineCause.class));
    assertThat(((MaintenanceOfflineCause) oc).isTakeOnline(), is(false));
    assertThat(maintenanceHelper.hasMaintenanceWindows(agentName), is(false));
  }
}
