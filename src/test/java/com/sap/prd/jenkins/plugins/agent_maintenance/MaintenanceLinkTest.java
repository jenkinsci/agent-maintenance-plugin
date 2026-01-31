package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import hudson.model.ManagementLink;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import java.util.List;
import java.util.stream.Stream;
import jenkins.model.Jenkins;
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

/** Access tests for the management link. */
@WithJenkins
@ExtendWith(MockitoExtension.class)
class MaintenanceLinkTest extends BasePermissionChecks {

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

  @Test
  void readPermissionHasNoAccess() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(USER);
    HtmlPage managePage = w.withThrowExceptionOnFailingStatusCode(false).goTo("target-maintenances/");
    assertThat(managePage.getWebResponse().getStatusCode(), is(403));
  }

  @Test
  void systemReadPermissionDoesNotExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(READER);
    HtmlPage managePage = w.goTo("target-maintenances/");
    assertThat(managePage.querySelector("#" + maintenanceId + " .am__link-delete"), is(nullValue()));
    assertThat(managePage.querySelector("#" + maintenanceIdRestricted + " .am__link-delete"), is(nullValue()));
    assertThat(managePage.getElementById(cloudMaintenanceId), is(nullValue()));
  }

  @Test
  void managePermissionDoesNotExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(MANAGE);
    HtmlPage managePage = w.goTo("target-maintenances/");
    assertThat(managePage.querySelector("#" + maintenanceId + " .am__link-delete"), is(nullValue()));
    assertThat(managePage.querySelector("#" + maintenanceIdRestricted + " .am__link-delete"), is(nullValue()));
    assertThat(managePage.getElementById(cloudMaintenanceId), is(nullValue()));
  }

  @ParameterizedTest(name = "deleteMaintenanceWindow[{0}]")
  @MethodSource("allTargets")
  void deleteMaintenanceWindow(MaintenanceTarget.TargetType target) throws Exception {
    MaintenanceLink instance = null;
    List<ManagementLink> list = Jenkins.get().getManagementLinks();
    for (ManagementLink link : list) {
      if (link instanceof MaintenanceLink) {
        instance = (MaintenanceLink) link;
        break;
      }
    }

    assertThat(instance, is(notNullValue()));

    WebClient w = rule.createWebClient();
    w.login(ADMIN);
    HtmlPage managePage = w.goTo("target-maintenances/");
    String idToDelete = getMaintenanceIdToDelete(target);
    String id = getMaintenanceId(target);
    MaintenanceTarget mt = createTarget(target);

    assertThat(managePage.getElementById(idToDelete), is(notNullValue()));

    try (ACLContext ignored = ACL.as(User.getById(ADMIN, false))) {
      instance.deleteMaintenance(idToDelete, mt.toKey());
    }

    managePage = w.goTo("target-maintenances/");
    assertThat(managePage.getElementById(idToDelete), is(nullValue()));
    assertThat(managePage.getElementById(id), is(notNullValue()));
  }

  @Test
  void configurePermissionDoesExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(CONFIGURE);
    HtmlPage managePage = w.goTo("target-maintenances/");

    assertThat(managePage.querySelector("#" + maintenanceId + " .am__link-delete"), is(notNullValue()));
    assertThat(managePage.querySelector("#" + maintenanceIdRestricted + " .am__link-delete"), is(nullValue()));
    assertThat(managePage.getElementById(cloudMaintenanceId), is(nullValue()));
  }

  @Test
  void disconnectPermissionDoesExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(DISCONNECT);
    HtmlPage managePage = w.goTo("target-maintenances/");

    assertThat(managePage.querySelector("#" + maintenanceId + " .am__link-delete"), is(notNullValue()));
    assertThat(managePage.querySelector("#" + maintenanceIdRestricted + " .am__link-delete"), is(nullValue()));
    assertThat(managePage.getElementById(cloudMaintenanceId), is(nullValue()));
  }

  @Test
  void adminPermissionDoesExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(ADMIN);
    HtmlPage managePage = w.goTo("target-maintenances/");

    assertThat(managePage.querySelector("#" + maintenanceId + " .am__link-delete"), is(notNullValue()));
    assertThat(managePage.querySelector("#" + maintenanceIdRestricted + " .am__link-delete"), is(notNullValue()));
    assertThat(managePage.querySelector("#" + cloudMaintenanceId + " .am__link-delete"), is(notNullValue()));
  }
}
