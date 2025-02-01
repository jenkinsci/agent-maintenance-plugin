package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.Extension;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.ManagementLink;
import hudson.model.Node;
import hudson.model.labels.LabelExpression;
import hudson.security.Permission;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.SlaveComputer;
import hudson.util.FormValidation;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.management.Badge;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.verb.POST;

/**
 * Link on manage Jenkins page to list all maintenance windows of all agents.
 */
@Extension
public class MaintenanceLink extends ManagementLink {
  private static final Logger LOGGER = Logger.getLogger(MaintenanceLink.class.getName());

  private transient Throwable error;

  @Override
  public String getDescription() {
    return Messages.MaintenanceLink_description();
  }

  @Override
  public String getDisplayName() {
    return Messages.MaintenanceLink_displayName();
  }

  @Override
  public String getIconFileName() {
    return "symbol-maintenance plugin-agent-maintenance";
  }

  @Override
  public String getUrlName() {
    return "agent-maintenances";
  }

  @Override
  public Permission getRequiredPermission() {
    return Jenkins.SYSTEM_READ;
  }

  /**
   * List of actions.
   *
   * @return List of actions
   */
  public List<MaintenanceAction> getAgents() {
    List<MaintenanceAction> agentList = new ArrayList<>();
    for (Node node : Jenkins.get().getNodes()) {
      Computer computer = node.toComputer();
      if (computer instanceof SlaveComputer) {
        MaintenanceAction action = new MaintenanceAction((SlaveComputer) computer);
        if (action.hasMaintenanceWindows()) {
          agentList.add(action);
        }
      }
    }

    return agentList;
  }

  private void setError(Throwable error) {
    this.error = error;
  }

  /**
   * The message of the last error that occured.
   *
   * @return error message
   */
  public String getError() {
    StringWriter message = new StringWriter();
    error.printStackTrace(new PrintWriter(message));
    error = null;
    return message.toString();
  }

  public boolean hasError() {
    return error != null;
  }

  @Override
  public Badge getBadge() {
    int active = 0;
    int total = 0;
    List<MaintenanceAction> mwList = getAgents();
    for (MaintenanceAction ma : mwList) {
      total++;
      if (ma.hasActiveMaintenanceWindows()) {
        active++;
      }
    }
    if (total == 0) {
      return null;
    }
    String text = active + "/" + total;
    String tooltip = active + getVerb(active) + " an active maintenance window.\n"
        + total + getVerb(total) + " defined maintenance windows.";
    Badge.Severity severity = Badge.Severity.INFO;
    if (active > 0) {
      severity = Badge.Severity.WARNING;
    }

    return new Badge(text, tooltip, severity);
  }

  private String getVerb(int count) {
    if (count == 1) {
      return " agent has";
    }
    return " agents have";
  }

  /**
   * Delete given maintenance window.
   *
   * @param id The id of the maintenance to delete
   * @param computerName The name of the computer to which the maintenance belongs
   */
  @JavaScriptMethod
  public boolean deleteMaintenance(String id, String computerName) throws IOException, ServletException {
    if (hasPermission(computerName)) {
      try {
        MaintenanceHelper.getInstance().deleteMaintenanceWindow(computerName, id);
        return true;
      } catch (Throwable e) {
        LOGGER.log(Level.WARNING, "Error while deleting maintenance window", e);
        return false;
      }
    }
    return false;
  }

  /**
   * Delete selected maintenance windows.
   *
   * @param json An json with maintenance ids to delete and corresponding computer names
   */
  @JavaScriptMethod
  public String[] deleteMultiple(JSONObject json) throws IOException, ServletException {
    Map<String, String> mwList = (Map<String, String>) JSONObject.toBean(json, Map.class);
    List<String> deletedList = new ArrayList<>();
    for (Entry<String, String> entry : mwList.entrySet()) {
      String computerName = entry.getValue();
      if (hasPermission(computerName)) {
        String id = entry.getKey();
        try {
          MaintenanceHelper.getInstance().deleteMaintenanceWindow(computerName, id);
          deletedList.add(id);
        } catch (Throwable e) {
          LOGGER.log(Level.WARNING, "Error while deleting maintenance window", e);
        }
      }
    }
    return deletedList.toArray(new String[0]);
  }

  /**
   * UI method to fetch status about maintenance windows.
   *
   * @return A Map containing for each maintenance window whether it is active or not.
   */
  @JavaScriptMethod
  public Map<String, Boolean> getMaintenanceStatus() {
    Map<String, Boolean> statusList = new HashMap<>();
    for (MaintenanceAction action : getAgents()) {
      Computer computer = action.getComputer();
      if (computer.hasAnyPermission(Computer.DISCONNECT, Computer.CONFIGURE, Computer.EXTENDED_READ)) {
        try {
          for (MaintenanceWindow mw : MaintenanceHelper.getInstance().getMaintenanceWindows(computer.getName())) {
            if (!mw.isMaintenanceOver()) {
              statusList.put(mw.getId(), mw.isMaintenanceScheduled());
            }
          }
        } catch (IOException ioe) {
          LOGGER.log(Level.WARNING, "Failed to read maintenance windows", ioe);
        }
      }
    }
    return statusList;
  }

  private boolean hasPermission(String computerName) {
    Computer c = Jenkins.get().getComputer(computerName);
    if (c != null) {
      return c.hasAnyPermission(MaintenanceAction.CONFIGURE_AND_DISCONNECT);
    }
    return false;
  }

  @Restricted(NoExternalUse.class)
  public FormValidation doCheckLabel(@QueryParameter String value) {
    return LabelExpression.validate(value);
  }

  @Restricted(NoExternalUse.class)
  public AutoCompletionCandidates doAutoCompleteLabel(@QueryParameter String value) {
    return LabelExpression.autoComplete(value);
  }

  /**
   * Add a maintenance window to a list of machines.
   *
   * @param req StaplerRequest
   * @param rsp StaplerResponse
   * @throws IOException      when saving xml failed
   * @throws ServletException when reading the form failed
   */
  @POST
  public void doAdd(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException, ServletException {
    Jenkins j = Jenkins.get();

    JSONObject src = req.getSubmittedForm();
    String labelString = src.optString("label");
    Label label = j.getLabel(labelString);
    if (label != null) {
      Set<Node> nodes = label.getNodes();
      MaintenanceWindow mw = req.bindJSON(MaintenanceWindow.class, src);
      LOGGER.log(Level.FINER, "Adding maintenance windows {0}", mw);
      LOGGER.log(Level.FINER, "Adding maintenance windows for agents: {0}", nodes);
  
      nodes.stream()
          .filter(n -> n.toComputer() instanceof SlaveComputer && !(n.toComputer() instanceof AbstractCloudComputer)
              && n.toComputer().getRetentionStrategy() instanceof AgentMaintenanceRetentionStrategy
              && n.hasAnyPermission(MaintenanceAction.CONFIGURE_AND_DISCONNECT))
          .forEach(n -> {
            try {
              SlaveComputer computer = (SlaveComputer) n.toComputer();
              MaintenanceWindow maintenanceWindow = req.bindJSON(MaintenanceWindow.class, src);
              MaintenanceHelper.getInstance().addMaintenanceWindow(computer.getName(), maintenanceWindow);
            } catch (Exception e) {
              LOGGER.log(Level.WARNING, "Error while adding maintenance window", e);
              setError(e);
            }
          });
    }
    rsp.sendRedirect(".");
  }

  public Class<MaintenanceWindow> getMaintenanceWindowClass() {
    return MaintenanceWindow.class;
  }
}
