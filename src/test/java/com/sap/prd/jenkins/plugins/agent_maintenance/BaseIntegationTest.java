package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.model.Slave;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.RetentionStrategy.Always;
import hudson.slaves.SlaveComputer;
import java.util.concurrent.TimeUnit;
import org.junit.ClassRule;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Base class for tests of the retention strategy.
 */
public abstract class BaseIntegationTest {

  @ClassRule
  public static JenkinsRule rule = new JenkinsRule();

  static {
    rule.timeout = 2400;
  }

  protected MaintenanceHelper maintenanceHelper = MaintenanceHelper.getInstance();

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

  protected void waitForDisconnect(Slave agent) throws InterruptedException {
    while (agent.getChannel() != null) {
      TimeUnit.SECONDS.sleep(10);
    }
  }
}
