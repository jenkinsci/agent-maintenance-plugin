package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.SortedSet;
import net.sf.json.JSONObject;
import org.htmlunit.HttpMethod;
import org.htmlunit.Page;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.NameValuePair;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Covers the add form wire contract and the DOM elements the page javascript depends on.
 */
@WithJenkins
class AddMaintenanceWindowTest extends BasePermissionChecks {

  private static JSONObject payload(String reason) {
    JSONObject json = new JSONObject();
    json.put("startTime", "2099-01-01 10:00");
    json.put("endTime", "2099-01-01 12:00");
    json.put("reason", reason);
    json.put("takeOnline", true);
    json.put("keepUpWhenActive", true);
    json.put("maxWaitMinutes", "10");
    return json;
  }

  private static Page postForm(WebClient w, String url, JSONObject json) throws Exception {
    WebRequest request = new WebRequest(w.createCrumbedUrl(url), HttpMethod.POST);
    request.setRequestParameters(List.of(new NameValuePair("json", json.toString())));
    return w.getPage(request);
  }

  private static SortedSet<MaintenanceWindow> windowsOf(String nodeName) throws Exception {
    return MaintenanceHelper.getInstance().getMaintenanceWindows(nodeName);
  }

  @Test
  void addFromAgentPageCreatesWindow() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(ADMIN);
    int before = windowsOf(agent.getNodeName()).size();

    postForm(w, agentMaintenanceUrl + "/add", payload("added from agent page"));

    assertThat(windowsOf(agent.getNodeName()), hasSize(before + 1));
  }

  @Test
  void addFromManagementPageCreatesWindow() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(ADMIN);
    int before = windowsOf(agent.getNodeName()).size();

    JSONObject json = payload("added from management page");
    json.put("label", agent.getNodeName());
    postForm(w, "manage/agent-maintenances/add", json);

    assertThat(windowsOf(agent.getNodeName()), hasSize(before + 1));
  }

  @Test
  void addWithNonMatchingLabelReportsError() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(ADMIN);
    int before = windowsOf(agent.getNodeName()).size();

    JSONObject json = payload("no matching agent");
    json.put("label", "there-is-no-such-agent");
    // getError() clears the message, so it is only visible on the redirect target.
    HtmlPage result = (HtmlPage) postForm(w, "manage/agent-maintenances/add", json);

    assertThat(windowsOf(agent.getNodeName()), hasSize(before));
    assertThat(result.querySelector(".error"), is(notNullValue()));
    assertThat(result.asNormalizedText(), containsString("there-is-no-such-agent"));
  }

  @Test
  void managementPageRendersElementsUsedByJavascript() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(ADMIN);
    HtmlPage page = w.goTo("agent-maintenances/");

    assertThat(page.getElementById("maintenance-table"), is(notNullValue()));
    assertThat(page.getElementById("add-button"), is(notNullValue()));
    assertThat(page.getElementById("delete-selected-button-link"), is(notNullValue()));
    assertThat(page.querySelector(".jenkins-table__checkbox-container"), is(notNullValue()));
    assertThat(page.getElementById("am__div--select"), is(nullValue()));
  }

  @Test
  void agentPageRendersElementsUsedByJavascript() throws Exception {
    WebClient w = rule.createWebClient();
    w.login(ADMIN);
    HtmlPage page = w.goTo(agentMaintenanceUrl);

    assertThat(page.getElementById("maintenance-table"), is(notNullValue()));
    assertThat(page.getElementById("recurring-maintenance-table"), is(notNullValue()));
    assertThat(page.getElementById("add-button"), is(notNullValue()));
    assertThat(page.getElementById("add-recurring"), is(notNullValue()));
    assertThat(page.getElementById("delete-selected-button-action"), is(notNullValue()));
    assertThat(page.getElementById("delete-selected-recurring-action"), is(notNullValue()));
    assertThat(page.querySelector(".jenkins-table__checkbox-container"), is(notNullValue()));
    assertThat(page.getElementById("am__div--select"), is(nullValue()));
  }
}
