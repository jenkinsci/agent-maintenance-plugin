package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.model.Slave;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.AccessDeniedException3;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.RetentionStrategy.Always;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.Mock;

/**
 * Test the Configuration.
 */
@WithJenkins
class MaintenanceConfigurationTest extends PermissionSetup {

  @Mock
  private StaplerResponse2 rsp;

  private Slave agent;
  private Slave agent2;

  @BeforeEach
  void setup() throws Exception {
    agent = rule.createOnlineSlave();
    agent2 = rule.createOnlineSlave();
  }

  @Test
  void injectAddsToAgents() {

    MaintenanceConfiguration config = MaintenanceConfiguration.getInstance();
    agent2.setRetentionStrategy(new AgentMaintenanceRetentionStrategy(new Always()));
    try (ACLContext ignored = ACL.as(User.getById(ADMIN, false))) {
      assertThat(agent.getRetentionStrategy(), is(RetentionStrategy.NOOP));
      assertThat(agent2.getRetentionStrategy(), instanceOf(AgentMaintenanceRetentionStrategy.class));
      config.doInject(rsp);
      assertThat(agent.getRetentionStrategy(), instanceOf(AgentMaintenanceRetentionStrategy.class));
      assertThat(agent2.getRetentionStrategy(), instanceOf(AgentMaintenanceRetentionStrategy.class));
    }
  }

  @Test
  void injectIsDeniedForReader() {
    MaintenanceConfiguration config = MaintenanceConfiguration.getInstance();
    try (ACLContext ignored = ACL.as(User.getById(READER, false))) {
      assertThrows(AccessDeniedException3.class, () -> config.doInject(rsp));
    }
  }

  @Test
  void removeFromAgents() {
    agent.setRetentionStrategy(new AgentMaintenanceRetentionStrategy(new Always()));
    MaintenanceConfiguration config = MaintenanceConfiguration.getInstance();
    try (ACLContext ignored = ACL.as(User.getById(ADMIN, false))) {
      assertThat(agent.getRetentionStrategy(), instanceOf(AgentMaintenanceRetentionStrategy.class));
      assertThat(agent2.getRetentionStrategy(), is(RetentionStrategy.NOOP));
      config.doRemove(rsp);
      assertThat(agent.getRetentionStrategy(), instanceOf(Always.class));
      assertThat(agent2.getRetentionStrategy(), is(RetentionStrategy.NOOP));
    }
  }

  @Test
  void removeIsDeniedForReader() {
    MaintenanceConfiguration config = MaintenanceConfiguration.getInstance();
    try (ACLContext ignored = ACL.as(User.getById(READER, false))) {
      assertThrows(AccessDeniedException3.class, () -> config.doRemove(rsp));
    }
  }
}
