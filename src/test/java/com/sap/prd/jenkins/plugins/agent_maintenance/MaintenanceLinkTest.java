package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.ManagementLink;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import java.util.List;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Access tests for the management link. */
public class MaintenanceLinkTest extends BasePermissionChecks {

  @Mock
  private StaplerRequest req;
  @Mock
  private StaplerResponse rsp;
  
  private AutoCloseable closeable;


  @Before
  public void openMocks() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  public void readPermissionHasNoAccess() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(USER);
    HtmlPage managePage = w.withThrowExceptionOnFailingStatusCode(false).goTo("agent-maintenances/");
    assertThat(managePage.getWebResponse().getStatusCode(), is(403));
  }

  @Test
  public void systemReadPermissionDoesNotExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(READER);
    HtmlPage managePage = w.goTo("agent-maintenances/");
    assertThat(managePage.querySelector("#" + maintenanceId + " .am__link-delete"), is(nullValue()));
    assertThat(managePage.querySelector("#" + maintenanceIdRestricted + " .am__link-delete"), is(nullValue()));
  }

  @Test
  public void managePermissionDoesNotExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(MANAGE);
    HtmlPage managePage = w.goTo("agent-maintenances/");
    assertThat(managePage.querySelector("#" + maintenanceId + " .am__link-delete"), is(nullValue()));
    assertThat(managePage.querySelector("#" + maintenanceIdRestricted + " .am__link-delete"), is(nullValue()));
  }

  @Test
  public void deleteMaintenanceWindow() throws Exception {
    MaintenanceLink instance = null;
    List<ManagementLink> list = Jenkins.get().getManagementLinks();
    for (ManagementLink link : list) {
      if (link instanceof MaintenanceLink) {
        instance = (MaintenanceLink) link;
        break;
      }
    }

    assert instance != null;

    WebClient w = rule.createWebClient();
    w.login(ADMIN);
    HtmlPage managePage = w.goTo("agent-maintenances/");
    assertThat(managePage.getElementById(maintenanceIdToDelete), is(notNullValue()));

    try (ACLContext ignored = ACL.as(User.getById(CONFIGURE, false))) {
      instance.deleteMaintenance(maintenanceIdToDelete, agent.getNodeName());
    }

    managePage = w.goTo("agent-maintenances/");
    assertThat(managePage.getElementById(maintenanceIdToDelete), is(nullValue()));
    assertThat(managePage.getElementById(maintenanceIdRestricted), is(notNullValue()));
  }

  @Test
  public void configurePermissionDoesExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(CONFIGURE);
    HtmlPage managePage = w.goTo("agent-maintenances/");
    assertThat(managePage.querySelector("#" + maintenanceId + " .am__link-delete"), is(notNullValue()));
    assertThat(managePage.querySelector("#" + maintenanceIdRestricted + " .am__link-delete"), is(nullValue()));
  }

  @Test
  public void disconnectPermissionDoesExposeDeleteLink() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(DISCONNECT);
    HtmlPage managePage = w.goTo("agent-maintenances/");
    assertThat(managePage.querySelector("#" + maintenanceId + " .am__link-delete"), is(notNullValue()));
    assertThat(managePage.querySelector("#" + maintenanceIdRestricted + " .am__link-delete"), is(nullValue()));
  }
}
