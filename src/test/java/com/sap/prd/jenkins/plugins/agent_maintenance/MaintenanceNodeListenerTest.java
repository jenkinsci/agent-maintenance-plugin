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
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** Tests the node listener. */
@WithJenkins
class MaintenanceNodeListenerTest extends BaseIntegrationTest {

  @TempDir
  private File folder;

  private Slave agent;

  /**
   * Setup from some tests.
   *
   * @throws Exception in case of an error
   */
  @Override
  @BeforeEach
  void setup(JenkinsRule rule) throws Exception {
    super.setup(rule);
    agent = rule.createOnlineSlave();
    AgentMaintenanceRetentionStrategy strategy =
        new AgentMaintenanceRetentionStrategy(new Always());
    agent.setRetentionStrategy(strategy);
  }

  @AfterEach
  void tearDown() throws IOException {
    maintenanceHelper.getMaintenanceWindows(agent.getNodeName()).clear();
  }

  @Test
  void agentDeleted() throws Exception {
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
    assertThat(agent.toComputer().isAcceptingTasks(), is(false));
    rule.jenkins.removeNode(agent);
    assertThat(maintenanceHelper.hasMaintenanceWindows(agent.getNodeName()), is(false));
  }

  @Test
  void agentRenamed() throws Exception {
    LocalDateTime start = LocalDateTime.now().minusMinutes(1);
    LocalDateTime end = start.plusMinutes(10);
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
    assertThat(agent.toComputer().isAcceptingTasks(), is(false));
    Slave newAgent =
        new DumbSlave(
            "newAgent", newFolder(folder, "junit").getAbsolutePath(), rule.createComputerLauncher(null));
    rule.jenkins.getNodesObject().replaceNode(agent, newAgent);
    assertThat(maintenanceHelper.hasMaintenanceWindows(agent.getNodeName()), is(false));
    assertThat(maintenanceHelper.hasMaintenanceWindows(newAgent.getNodeName()), is(true));
  }

  @Test
  void retentionStrategyIsInjected() throws Exception {
    MaintenanceConfiguration.getInstance().setInjectRetentionStrategy(true);
    agent = rule.createOnlineSlave();
    assertThat(agent.getRetentionStrategy(), instanceOf(AgentMaintenanceRetentionStrategy.class));
  }

  @Test
  void retentionStrategyIsInjectedOnRename() throws Exception {
    MaintenanceConfiguration.getInstance().setInjectRetentionStrategy(false);
    agent = rule.createOnlineSlave();
    assertThat(
        agent.getRetentionStrategy(), not(instanceOf(AgentMaintenanceRetentionStrategy.class)));
    MaintenanceConfiguration.getInstance().setInjectRetentionStrategy(true);
    Slave newAgent =
        new DumbSlave(
            "newAgent", newFolder(folder, "junit").getAbsolutePath(), rule.createComputerLauncher(null));
    Demand demand = new Demand(1, 1);
    newAgent.setRetentionStrategy(demand);
    rule.jenkins.getNodesObject().replaceNode(agent, newAgent);
    RetentionStrategy<?> strategy = newAgent.getRetentionStrategy();
    assertThat(strategy, instanceOf(AgentMaintenanceRetentionStrategy.class));
    assertThat(
        ((AgentMaintenanceRetentionStrategy) strategy).getRegularRetentionStrategy(), is(demand));
  }

  @Test
  void retentionStrategyIsNotInjected() throws Exception {
    MaintenanceConfiguration.getInstance().setInjectRetentionStrategy(false);
    agent = rule.createOnlineSlave();
    assertThat(
        agent.getRetentionStrategy(), not(instanceOf(AgentMaintenanceRetentionStrategy.class)));
  }

  private static File newFolder(File root, String... subDirs) throws IOException {
    String subFolder = String.join("/", subDirs);
    File result = new File(root, subFolder);
    if (!result.mkdirs()) {
      throw new IOException("Couldn't create folders " + root);
    }
    return result;
  }
}
