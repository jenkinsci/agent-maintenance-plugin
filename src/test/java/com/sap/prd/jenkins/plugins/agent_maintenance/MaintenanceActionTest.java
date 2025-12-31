package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.slaves.RetentionStrategy.Demand;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/** Tests for the action. */
@WithJenkins
@ExtendWith(MockitoExtension.class)
class MaintenanceActionTest extends BasePermissionChecks {

  @Mock
  private StaplerRequest2 req;
  @Mock
  private StaplerResponse2 rsp;

  // ====== AGENT TESTS =====

  @Test
  void readPermissionHasNoAccess() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(USER);
    HtmlPage managePage = w.withThrowExceptionOnFailingStatusCode(false).goTo(agentMaintenanceUrl);
    assertThat(managePage.getWebResponse().getStatusCode(), is(404));
  }

  @Test
  void extendedReadPermissionDoesNotExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(READER);
    HtmlPage managePage = w.goTo(agentMaintenanceUrl);
    assertThat(managePage.querySelector("#" + maintenanceId + " .am__action-delete"), is(nullValue()));
  }

  @Test
  void configurePermissionDoesExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(CONFIGURE);
    HtmlPage managePage = w.goTo(agentMaintenanceUrl);
    assertThat(managePage.querySelector("#" + maintenanceId + " .am__action-delete"), is(notNullValue()));
  }

  @Test
  void disconnectPermissionDoesExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(DISCONNECT);
    HtmlPage managePage = w.goTo(agentMaintenanceUrl);
    assertThat(managePage.querySelector("#" + maintenanceId + " .am__action-delete"), is(notNullValue()));
  }

  @Test
  void extendedReadPermissionCantPost() {
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.AGENT, agent.getNodeName());
    MaintenanceAction action = new MaintenanceAction(target);
    try (ACLContext ignored = ACL.as(User.getById(READER, false))) {
      assertThrows(AccessDeniedException.class, () -> action.doConfigSubmit(req));
      assertThrows(AccessDeniedException.class, () -> action.doAdd(req));
      assertThat(action.deleteMaintenance(maintenanceId), is(false));
      assertThrows(AccessDeniedException.class, () -> action.deleteMultiple(new String[0]));
    }
  }

  @Test
  void disconnectUserCantEnableDisable() {
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.AGENT, agent.getNodeName());
    MaintenanceAction action = new MaintenanceAction(target);
    try (ACLContext ignored = ACL.as(User.getById(DISCONNECT, false))) {
      assertThrows(AccessDeniedException.class, () -> action.doDisable(rsp));
      assertThrows(AccessDeniedException.class, () -> action.doEnable(rsp));
    }
  }

  @Test
  void deleteEnableKeepsOriginalStrategy() throws Exception {
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.AGENT, agent.getNodeName());
    MaintenanceAction action = new MaintenanceAction(target);
    try (ACLContext ignored = ACL.as(User.getById(CONFIGURE, false))) {
      action.doDisable(rsp);
      assertThat(agent.getRetentionStrategy(), instanceOf(Demand.class));
      action.doEnable(rsp);
      assertThat(agent.getRetentionStrategy(), instanceOf(AgentMaintenanceRetentionStrategy.class));
      AgentMaintenanceRetentionStrategy strategy = (AgentMaintenanceRetentionStrategy) agent.getRetentionStrategy();
      assertThat(strategy.getRegularRetentionStrategy(), instanceOf(Demand.class));
      Demand demand = (Demand) strategy.getRegularRetentionStrategy();
      assertThat(demand.getIdleDelay(), is(2L));
      assertThat(demand.getInDemandDelay(), is(1L));
    }
  }

  // ===== CLOUD TESTS =====


}
