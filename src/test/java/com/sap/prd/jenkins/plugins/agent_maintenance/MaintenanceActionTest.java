package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;

import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.slaves.RetentionStrategy.Demand;
import hudson.slaves.SlaveComputer;
import org.htmlunit.html.HtmlPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

/** Tests for the action. */
public class MaintenanceActionTest extends BasePermissionChecks {
  @Mock
  private StaplerRequest req;
  @Mock
  private StaplerResponse rsp;
  private AutoCloseable mocks;

  @Before
  public void setup() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
  }

  @After
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void readPermissionHasNoAccess() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(USER);
    HtmlPage managePage = w.withThrowExceptionOnFailingStatusCode(false).goTo(agentMaintenanceUrl);
    assertThat(managePage.getWebResponse().getStatusCode(), is(404));
  }

  @Test
  public void extendedReadPermissionDoesNotExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(READER);
    HtmlPage managePage = w.goTo(agentMaintenanceUrl);
    assertThat(managePage.querySelector("#" + maintenanceId + " .am__action-delete"), is(nullValue()));
  }

  @Test
  public void configurePermissionDoesExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(CONFIGURE);
    HtmlPage managePage = w.goTo(agentMaintenanceUrl);
    assertThat(managePage.querySelector("#" + maintenanceId + " .am__action-delete"), is(notNullValue()));
  }

  @Test
  public void disconnectPermissionDoesExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(DISCONNECT);
    HtmlPage managePage = w.goTo(agentMaintenanceUrl);
    assertThat(managePage.querySelector("#" + maintenanceId + " .am__action-delete"), is(notNullValue()));
  }

  @Test
  public void extendedReadPermissionCantPost() throws Exception {
    MaintenanceAction action = new MaintenanceAction((SlaveComputer) agent.toComputer());
    try (ACLContext ignored = ACL.as(User.getById(READER, false))) {
      assertThrows(AccessDeniedException.class, () -> action.doConfigSubmit(req));
      assertThrows(AccessDeniedException.class, () -> action.doAdd(req));
      assertThat(action.deleteMaintenance(maintenanceId), is(false));
      assertThrows(AccessDeniedException.class, () -> action.deleteMultiple(new String[0]));
    }
  }

  @Test
  public void dicsonnectUserCantEnableDisable() throws Exception {
    MaintenanceAction action = new MaintenanceAction((SlaveComputer) agent.toComputer());
    try (ACLContext ignored = ACL.as(User.getById(DISCONNECT, false))) {
      assertThrows(AccessDeniedException.class, () -> action.doDisable(rsp));
      assertThrows(AccessDeniedException.class, () -> action.doEnable(rsp));
    }
  }

  @Test
  public void deleteEnableKeepsOriginalStrategy() throws Exception {
    MaintenanceAction action = new MaintenanceAction((SlaveComputer) agent.toComputer());
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
}
