package com.sap.prd.jenkins.plugins.agent_maintenance;

import static com.sap.prd.jenkins.plugins.agent_maintenance.MaintenanceWindow.DATE_FORMATTER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Slave;
import hudson.slaves.RetentionStrategy.Demand;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import jenkins.model.InterruptedBuildAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.jvnet.hudson.test.SleepBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests retention strategy that involves running jobs.
 */
@WithJenkins
class IntegrationTest extends BaseIntegrationTest {

  @Test
  @Timeout(300)
  void waitForRunningProjectToFinishBeforeDisconnect() throws Exception {
    Slave agent = getAgent("waitForRunningProjectToFinishBeforeDisconnect");
    FreeStyleProject project = rule.createFreeStyleProject();
    project.setAssignedLabel(agent.getSelfLabel());
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = start.plusMinutes(10);
    MaintenanceWindow mw =
        new MaintenanceWindow(
            start.format(DATE_FORMATTER),
            end.format(DATE_FORMATTER),
            "test",
            true,
            true,
            "3",
            "test",
            null);
    String id = mw.getId();
    project.getBuildersList().add(new SleepBuilder(1000 * 60 * 2));
    FreeStyleBuild build = project.scheduleBuild2(0).waitForStart();
    maintenanceHelper.addMaintenanceWindow(agent.getNodeName(), mw);
    assertThat(agent.toComputer().isAcceptingTasks(), is(false));
    waitForDisconnect(agent, mw);
    assertThat(build.getResult(), is(Result.SUCCESS));
    maintenanceHelper.deleteMaintenanceWindow(agent.getNodeName(), id);
  }

  @Test
  @Timeout(300)
  void projectGetsAbortedWhenRunningTooLong() throws Exception {
    Slave agent = getAgent("projectGetsAbortedWhenRunningTooLong");
    FreeStyleProject project = rule.createFreeStyleProject();
    project.setAssignedLabel(agent.getSelfLabel());
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = start.plusMinutes(10);
    MaintenanceWindow mw =
        new MaintenanceWindow(
            start.format(DATE_FORMATTER),
            end.format(DATE_FORMATTER),
            "test",
            true,
            true,
            "1",
            "test",
            null);
    String id = mw.getId();
    project.getBuildersList().add(new SleepBuilder(1000 * 60 * 7));
    FreeStyleBuild build = project.scheduleBuild2(0).waitForStart();
    maintenanceHelper.addMaintenanceWindow(agent.getNodeName(), mw);
    assertThat(agent.toComputer().isAcceptingTasks(), is(false));
    waitForDisconnect(agent, mw);
    assertThat(build.getResult(), is(Result.ABORTED));
    InterruptedBuildAction interruptedCauseAction = build.getAction(InterruptedBuildAction.class);
    assertThat(interruptedCauseAction, is(notNullValue()));
    assertThat(interruptedCauseAction.getCauses().get(0), instanceOf(MaintenanceInterruption.class));
    maintenanceHelper.deleteMaintenanceWindow(agent.getNodeName(), id);
  }

  @Test
  @Timeout(300)
  void projectGetsAbortedWithoutKeepOnline() throws Exception {
    Slave agent = getAgent("projectGetsAbortedWhenRunningTooLong");
    FreeStyleProject project = rule.createFreeStyleProject();
    project.setAssignedLabel(agent.getSelfLabel());
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = start.plusMinutes(10);
    MaintenanceWindow mw =
        new MaintenanceWindow(
            start.format(DATE_FORMATTER),
            end.format(DATE_FORMATTER),
            "test",
            true,
            false,
            "5",
            "test",
            null);
    String id = mw.getId();
    project.getBuildersList().add(new SleepBuilder(1000 * 60 * 7));
    FreeStyleBuild build = project.scheduleBuild2(0).waitForStart();
    maintenanceHelper.addMaintenanceWindow(agent.getNodeName(), mw);
    assertThat(agent.toComputer().isAcceptingTasks(), is(false));
    waitForDisconnect(agent, mw);
    assertThat(build.getResult(), is(Result.ABORTED));
    assertThat(build.getDuration(), lessThan(1000L * 60 * 3));
    maintenanceHelper.deleteMaintenanceWindow(agent.getNodeName(), id);
  }

  @Test
  @Timeout(300)
  void onDemandStrategyIsAppliedProperly() throws Exception {
    Demand demandStrategy = new Demand(1, 1);
    Slave agent = getAgent("onDemandStrategyIsAppliedProperly", demandStrategy);
    FreeStyleProject project = rule.createFreeStyleProject();
    project.setAssignedLabel(agent.getSelfLabel());
    project.getBuildersList().add(new SleepBuilder(1000));
    assertThat(agent.toComputer().isAcceptingTasks(), is(true));
    waitForDisconnect(agent, null);
    project.scheduleBuild2(0);
    waitForConnect(agent);
    waitForDisconnect(agent, null);
    assertThat(agent.getChannel(), is(nullValue()));
  }

  protected void waitForConnect(Slave agent) throws Exception {
    while (agent.getChannel() == null) {
      TimeUnit.SECONDS.sleep(10);
      triggerCheckCycle(agent);
    }
  }
}
