package com.sap.prd.jenkins.plugins.agent_maintenance;

import static com.sap.prd.jenkins.plugins.agent_maintenance.MaintenanceWindow.DATE_FORMATTER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Slave;
import hudson.slaves.RetentionStrategy.Demand;
import java.time.LocalDateTime;
import org.junit.Test;
import org.jvnet.hudson.test.SleepBuilder;
import org.jvnet.hudson.test.recipes.WithTimeout;

/**
 * Tests retention strategy that involves running jobs.
 */
public class IntegrationTest extends BaseIntegationTest {

  @Test
  @WithTimeout(600)
  public void waitForRunningProjectToFinishBeforeDisconnect() throws Exception {
    Slave agent = getAgent("waitForRunningProjectToFinishBeforeDisconnect");
    FreeStyleProject project = rule.createFreeStyleProject();
    project.setAssignedLabel(agent.getSelfLabel());
    LocalDateTime start = LocalDateTime.now().plusMinutes(2);
    LocalDateTime end = start.plusMinutes(10);
    MaintenanceWindow mw =
        new MaintenanceWindow(
            start.format(DATE_FORMATTER),
            end.format(DATE_FORMATTER),
            "test",
            true,
            true,
            "6",
            "test",
            null);
    String id = mw.getId();
    project.getBuildersList().add(new SleepBuilder(1000 * 60 * 5));
    maintenanceHelper.addMaintenanceWindow(agent.getNodeName(), mw);
    FreeStyleBuild build = project.scheduleBuild2(0).get();
    assertThat(agent.toComputer().isAcceptingTasks(), is(false));
    assertThat(build.getResult(), is(Result.SUCCESS));
    waitForDisconnect(agent, mw);
    maintenanceHelper.deleteMaintenanceWindow(agent.getNodeName(), id);
  }

  @Test
  @WithTimeout(600)
  public void projectGetsAbortedWhenRunningTooLong() throws Exception {
    Slave agent = getAgent("projectGetsAbortedWhenRunningTooLong");
    FreeStyleProject project = rule.createFreeStyleProject();
    project.setAssignedLabel(agent.getSelfLabel());
    LocalDateTime start = LocalDateTime.now().plusMinutes(2);
    LocalDateTime end = start.plusMinutes(10);
    MaintenanceWindow mw =
        new MaintenanceWindow(
            start.format(DATE_FORMATTER),
            end.format(DATE_FORMATTER),
            "test",
            true,
            true,
            "2",
            "test",
            null);
    String id = mw.getId();
    project.getBuildersList().add(new SleepBuilder(1000 * 60 * 7));
    maintenanceHelper.addMaintenanceWindow(agent.getNodeName(), mw);
    assertThat(agent.toComputer().isAcceptingTasks(), is(true));
    assertThat(agent.toComputer().isOnline(), is(true));
    FreeStyleBuild build = project.scheduleBuild2(0).get();
    assertThat(agent.toComputer().isAcceptingTasks(), is(false));
    assertThat(build.getResult(), is(Result.ABORTED));
    waitForDisconnect(agent, mw);
    maintenanceHelper.deleteMaintenanceWindow(agent.getNodeName(), id);
  }

  @Test
  @WithTimeout(600)
  public void projectGetsAbortedWithoutKeepOnline() throws Exception {
    Slave agent = getAgent("projectGetsAbortedWhenRunningTooLong");
    FreeStyleProject project = rule.createFreeStyleProject();
    project.setAssignedLabel(agent.getSelfLabel());
    LocalDateTime start = LocalDateTime.now().plusMinutes(2);
    LocalDateTime end = start.plusMinutes(10);
    MaintenanceWindow mw =
        new MaintenanceWindow(
            start.format(DATE_FORMATTER),
            end.format(DATE_FORMATTER),
            "test",
            true,
            false,
            "2",
            "test",
            null);
    String id = mw.getId();
    project.getBuildersList().add(new SleepBuilder(1000 * 60 * 7));
    maintenanceHelper.addMaintenanceWindow(agent.getNodeName(), mw);
    assertThat(agent.toComputer().isAcceptingTasks(), is(true));
    assertThat(agent.toComputer().isOnline(), is(true));
    FreeStyleBuild build = project.scheduleBuild2(0).get();
    assertThat(agent.toComputer().isAcceptingTasks(), is(false));
    assertThat(build.getResult(), is(Result.ABORTED));
    waitForDisconnect(agent, mw);
    maintenanceHelper.deleteMaintenanceWindow(agent.getNodeName(), id);
  }

  @Test
  @WithTimeout(600)
  public void onDemandStrategyIsAppliedProperly() throws Exception {
    Demand demandStrategy = new Demand(1, 1);
    Slave agent = getAgent("onDemandStrategyIsAppliedProperly", demandStrategy);
    FreeStyleProject project = rule.createFreeStyleProject();
    project.setAssignedLabel(agent.getSelfLabel());
    project.getBuildersList().add(new SleepBuilder(1000));
    assertThat(agent.toComputer().isAcceptingTasks(), is(true));
    waitForDisconnect(agent, null);
    FreeStyleBuild build = project.scheduleBuild2(0).get();
    assertThat(build.getResult(), is(Result.SUCCESS));
    waitForDisconnect(agent, null);
    assertThat(agent.getChannel(), is(nullValue()));
  }
}
