package com.sap.prd.jenkins.plugins.agent_maintenance;

import static com.sap.prd.jenkins.plugins.agent_maintenance.MaintenanceWindow.DATE_FORMATTER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import hudson.model.Slave;
import hudson.slaves.DumbSlave;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.RetentionStrategy.Always;
import hudson.slaves.RetentionStrategy.Demand;
import java.io.IOException;
import java.time.LocalDateTime;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

/** Tests the node listener. */
public class MaintenanceNodeListenerTest {

  @Rule public JenkinsRule rule = new JenkinsRule();

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private MaintenanceHelper maintenanceHelper = MaintenanceHelper.getInstance();
  private Slave agent;

  /**
   * Setup from some tests.
   *
   * @throws Exception in case of an error
   */
  public void setup() throws Exception {
    agent = rule.createOnlineSlave();
    AgentMaintenanceRetentionStrategy strategy =
        new AgentMaintenanceRetentionStrategy(new Always());
    agent.setRetentionStrategy(strategy);
  }

  @After
  public void tearDown() throws IOException {
    maintenanceHelper.getMaintenanceWindows(agent.getNodeName()).clear();
  }

  @Test
  public void agentDeleted() throws Exception {
    setup();
    LocalDateTime start = LocalDateTime.now().minusMinutes(1);
    LocalDateTime end = start.plusMinutes(5);
    MaintenanceWindow mw =
        new MaintenanceWindow(
            start.format(DATE_FORMATTER),
            end.format(DATE_FORMATTER),
            "test",
            true,
            true,
            5,
            "test",
            null);
    maintenanceHelper.addMaintenanceWindow(agent.getNodeName(), mw);
    assertThat(agent.toComputer().isAcceptingTasks(), is(false));
    rule.jenkins.removeNode(agent);
    assertThat(maintenanceHelper.hasMaintenanceWindows(agent.getNodeName()), is(false));
  }

  @Test
  public void agentRenamed() throws Exception {
    setup();
    LocalDateTime start = LocalDateTime.now().minusMinutes(1);
    LocalDateTime end = start.plusMinutes(10);
    MaintenanceWindow mw =
        new MaintenanceWindow(
            start.format(DATE_FORMATTER),
            end.format(DATE_FORMATTER),
            "test",
            true,
            true,
            5,
            "test",
            null);
    maintenanceHelper.addMaintenanceWindow(agent.getNodeName(), mw);
    assertThat(agent.toComputer().isAcceptingTasks(), is(false));
    Slave newAgent =
        new DumbSlave(
            "newAgent", folder.newFolder().getAbsolutePath(), rule.createComputerLauncher(null));
    rule.jenkins.getNodesObject().replaceNode(agent, newAgent);
    assertThat(maintenanceHelper.hasMaintenanceWindows(agent.getNodeName()), is(false));
    assertThat(maintenanceHelper.hasMaintenanceWindows(newAgent.getNodeName()), is(true));
  }

  @Test
  public void retentionStrategyIsInjected() throws Exception {
    MaintenanceConfiguration.getInstance().setInjectRetentionStrategy(true);
    agent = rule.createOnlineSlave();
    assertThat(agent.getRetentionStrategy(), instanceOf(AgentMaintenanceRetentionStrategy.class));
  }

  @Test
  public void retentionStrategyIsInjectedOnRename() throws Exception {
    MaintenanceConfiguration.getInstance().setInjectRetentionStrategy(false);
    agent = rule.createOnlineSlave();
    assertThat(
        agent.getRetentionStrategy(), not(instanceOf(AgentMaintenanceRetentionStrategy.class)));
    MaintenanceConfiguration.getInstance().setInjectRetentionStrategy(true);
    Slave newAgent =
        new DumbSlave(
            "newAgent", folder.newFolder().getAbsolutePath(), rule.createComputerLauncher(null));
    Demand demand = new Demand(1, 1);
    newAgent.setRetentionStrategy(demand);
    rule.jenkins.getNodesObject().replaceNode(agent, newAgent);
    RetentionStrategy<?> strategy = newAgent.getRetentionStrategy();
    assertThat(strategy, instanceOf(AgentMaintenanceRetentionStrategy.class));
    assertThat(
        ((AgentMaintenanceRetentionStrategy) strategy).getRegularRetentionStrategy(), is(demand));
  }

  @Test
  public void retentionStrategyIsNotInjected() throws Exception {
    MaintenanceConfiguration.getInstance().setInjectRetentionStrategy(false);
    agent = rule.createOnlineSlave();
    assertThat(
        agent.getRetentionStrategy(), not(instanceOf(AgentMaintenanceRetentionStrategy.class)));
  }
}
