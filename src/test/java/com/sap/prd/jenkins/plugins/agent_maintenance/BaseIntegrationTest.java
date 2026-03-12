package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.model.Slave;
import hudson.slaves.Cloud;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.RetentionStrategy.Always;
import hudson.slaves.SlaveComputer;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Base class for tests of the retention strategy.
 */
@WithJenkins
abstract class BaseIntegrationTest {

  protected JenkinsRule rule;

  protected MaintenanceHelper maintenanceHelper = MaintenanceHelper.getInstance();

  @BeforeEach
  void setup(JenkinsRule rule) throws Exception {
    this.rule = rule;
    maintenanceHelper.clearCache(); // Cache clear before each test
    Jenkins.get().clouds.clear();
  }

  protected Slave getAgent(String name) throws Exception {
    return getAgent(name, null);
  }

  protected Slave getAgent(String name, RetentionStrategy<SlaveComputer> innerStrategy) throws Exception {
    Slave agent = rule.createSlave(name, null, null);
    rule.waitOnline(agent);
    if (innerStrategy == null) {
      innerStrategy = new Always();
    }
    AgentMaintenanceRetentionStrategy ams = new AgentMaintenanceRetentionStrategy(innerStrategy);
    agent.setRetentionStrategy(ams);

    return agent;
  }

  protected void waitForDisconnect(Slave agent, MaintenanceWindow mw) throws Exception {
    LocalDateTime timeout = LocalDateTime.now().plusMinutes(4);
    while (agent.getChannel() != null) {
      TimeUnit.SECONDS.sleep(10);
      triggerCheckCycle(agent);
      LocalDateTime now = LocalDateTime.now();
      if (now.isAfter(timeout)) {
        String active = "unknown";
        if (mw != null) {
          active = "" + mw.isMaintenanceScheduled();
        }
        throw new Exception("Agent did not disconnect within 4 minutes. Active: " + active);
      }
    }
  }

  protected void triggerCheckCycle(Slave agent) {
    SlaveComputer computer = (SlaveComputer) agent.toComputer();
    if (computer != null) {
      computer.getRetentionStrategy().check(computer);
    }
  }

  protected MaintenanceTarget getTarget(MaintenanceTarget.TargetType targetType, String name) {
    MaintenanceTarget target;
    if (targetType == MaintenanceTarget.TargetType.CLOUD) {
      Cloud testCloud = new TestCloud(name);
      target = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, testCloud.name);
    } else {
      target = new MaintenanceTarget(targetType, name);
    }
    return target;
  }
}
