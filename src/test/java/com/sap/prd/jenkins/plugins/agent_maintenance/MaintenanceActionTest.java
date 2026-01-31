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
import java.util.stream.Stream;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

  static Stream<MaintenanceTarget.TargetType> allTargets() {
    return Stream.of(
            MaintenanceTarget.TargetType.AGENT,
            MaintenanceTarget.TargetType.CLOUD
    );
  }

  @ParameterizedTest(name = "readPermissionHasNoAccess[{0}]")
  @MethodSource("allTargets")
  void readPermissionHasNoAccess(MaintenanceTarget.TargetType target) throws Exception {
    WebClient w = rule.createWebClient();
    w.login(USER);
    String url = getMaintenanceUrl(target);
    HtmlPage managePage = w.withThrowExceptionOnFailingStatusCode(false).goTo(url);
    assertThat(managePage.getWebResponse().getStatusCode(), is(403));
  }

  @Test
  void extendedReadPermissionDoesNotExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(READER);
    String url = getMaintenanceUrl(MaintenanceTarget.TargetType.AGENT);
    String id = getMaintenanceId(MaintenanceTarget.TargetType.AGENT);
    HtmlPage managePage = w.goTo(url);
    assertThat(managePage.querySelector("#" + id + " .am__action-delete"), is(nullValue()));
  }

  @ParameterizedTest(name = "extendedReadPermissionCantPost[{0}]")
  @MethodSource("allTargets")
  void extendedReadPermissionCantPost(MaintenanceTarget.TargetType target) {
    MaintenanceTarget mt = createTarget(target);
    MaintenanceAction action = new MaintenanceAction(mt);
    String id = getMaintenanceId(target);
    try (ACLContext ignored = ACL.as(User.getById(READER, false))) {
      assertThrows(AccessDeniedException.class, () -> action.doConfigSubmit(req));
      assertThrows(AccessDeniedException.class, () -> action.doAdd(req));
      assertThat(action.deleteMaintenance(id), is(false));
      assertThrows(AccessDeniedException.class, () -> action.deleteMultiple(new String[0]));
    }
  }

  @Test
  void configurePermissionDoesExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(CONFIGURE);
    String url = getMaintenanceUrl(MaintenanceTarget.TargetType.AGENT);
    String id = getMaintenanceId(MaintenanceTarget.TargetType.AGENT);
    HtmlPage managePage = w.goTo(url);
    assertThat(managePage.querySelector("#" + id + " .am__action-delete"), is(notNullValue()));
  }

  @Test
  void disconnectPermissionDoesExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(DISCONNECT);
    String url = getMaintenanceUrl(MaintenanceTarget.TargetType.AGENT);
    String id = getMaintenanceId(MaintenanceTarget.TargetType.AGENT);
    HtmlPage managePage = w.goTo(url);
    assertThat(managePage.querySelector("#" + id + " .am__action-delete"), is(notNullValue()));
  }

  @Test
  void disconnectUserCantEnableDisable() {
    MaintenanceTarget target = createTarget(MaintenanceTarget.TargetType.AGENT);
    MaintenanceAction action = new MaintenanceAction(target);
    try (ACLContext ignored = ACL.as(User.getById(DISCONNECT, false))) {
      assertThrows(AccessDeniedException.class, () -> action.doDisable(rsp));
      assertThrows(AccessDeniedException.class, () -> action.doEnable(rsp));
    }
  }

  @Test
  void deleteEnableKeepsOriginalStrategy() throws Exception {
    MaintenanceTarget target = createTarget(MaintenanceTarget.TargetType.AGENT);
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

  @Test
  void configurePermissionDoesNotExposeDeleteLinkForCloud() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(CONFIGURE);
    String url = getMaintenanceUrl(MaintenanceTarget.TargetType.CLOUD);
    HtmlPage managePage = w.withThrowExceptionOnFailingStatusCode(false).goTo(url);
    assertThat(managePage.getWebResponse().getStatusCode(), is(403));
  }

  @Test
  void disconnectPermissionDoesNotExposeDeleteLinkForCloud() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(DISCONNECT);
    String url = getMaintenanceUrl(MaintenanceTarget.TargetType.CLOUD);
    HtmlPage managePage = w.withThrowExceptionOnFailingStatusCode(false).goTo(url);
    assertThat(managePage.getWebResponse().getStatusCode(), is(403));
  }

  @Test
  void configureUserCantDeleteCloudMaintenance() {
    MaintenanceTarget target = createTarget(MaintenanceTarget.TargetType.CLOUD);
    MaintenanceAction action = new MaintenanceAction(target);
    String id = getMaintenanceId(MaintenanceTarget.TargetType.CLOUD);

    try (ACLContext ignored = ACL.as(User.getById(CONFIGURE, false))) {
      boolean result = action.deleteMaintenance(id);
      assertThat(result, is(false));
    }
  }

  @Test
  void adminPermissionDoesExposeDeleteLinkForCloud() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(ADMIN);
    String url = getMaintenanceUrl(MaintenanceTarget.TargetType.CLOUD);
    String id = getMaintenanceId(MaintenanceTarget.TargetType.CLOUD);
    HtmlPage managePage = w.goTo(url);
    assertThat(managePage.querySelector("#" + id + " .am__action-delete"), is(notNullValue()));
  }

  @Test
  void cloudAndAgentMaintenanceAreIndependent() throws Exception {
    MaintenanceTarget cloudTarget = createTarget(MaintenanceTarget.TargetType.CLOUD);
    MaintenanceTarget agentTarget = createTarget(MaintenanceTarget.TargetType.AGENT);
    MaintenanceHelper helper = MaintenanceHelper.getInstance();

    MaintenanceWindow cloudWindow = getMaintenanceWindow(MaintenanceTarget.TargetType.CLOUD);
    MaintenanceWindow agentWindow = getMaintenanceWindow(MaintenanceTarget.TargetType.AGENT);

    MaintenanceAction cloudAction = new MaintenanceAction(cloudTarget);
    try (ACLContext ignored = ACL.as(User.getById(ADMIN, false))) {
      // Deleting both cloud mw
      cloudAction.deleteMaintenance(cloudWindow.getId());
      cloudAction.deleteMaintenance(getMaintenanceIdToDelete(MaintenanceTarget.TargetType.CLOUD));
    }

    assertThat(helper.hasMaintenanceWindows(agentTarget.toKey()), is(true));
    assertThat(helper.hasMaintenanceWindows(cloudTarget.toKey()), is(false));
  }
}
