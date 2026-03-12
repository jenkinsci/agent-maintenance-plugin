package com.sap.prd.jenkins.plugins.agent_maintenance;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.ManagementLink;
import hudson.model.Node;
import hudson.model.labels.LabelExpression;
import hudson.security.Permission;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.Cloud;
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
import java.util.Objects;
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
 * Link on manage Jenkins page to list all maintenance windows of all targets.
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
    List<MaintenanceAction> all;
    try {
      all = getTargets();
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Error while reading maintenance windows", e);
      return Messages.MaintenanceLink_displayName();
    }
    boolean hasAgents = all.stream().anyMatch(MaintenanceAction::isAgent);
    boolean hasClouds = all.stream().anyMatch(MaintenanceAction::isCloud);

    if (hasAgents && !hasClouds) {
      return Messages.MaintenanceLink_displayName_agent();
    }
    if (hasClouds && !hasAgents) {
      return Messages.MaintenanceLink_displayName_cloud();
    }
    return Messages.MaintenanceLink_displayName();
  }

  @Override
  public String getIconFileName() {
    return "symbol-maintenance plugin-agent-maintenance";
  }

  @Override
  public String getUrlName() {
    return "target-maintenances";
  }

  @Override
  @NonNull
  public Permission getRequiredPermission() {
    return Jenkins.SYSTEM_READ;
  }

  /**
   * List of actions.
   *
   * @return List of actions
   */
  public List<MaintenanceAction> getTargets() throws IOException {
    List<MaintenanceAction> targetList = new ArrayList<>();
    Jenkins j = Jenkins.get();

    // Existing agent specific logic
    for (Node node : j.getNodes()) {
      Computer computer = node.toComputer();
      if (computer instanceof SlaveComputer) {
        MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.AGENT, node.getNodeName());
        MaintenanceAction action = new MaintenanceAction(target);
        if (action.hasMaintenanceWindows() || action.hasRecurringMaintenanceWindows()) {
          targetList.add(action);
        }
      }
    }

    // New: Adding clouds to the list
    for (Cloud cloud : j.clouds) {
      try {
        MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, cloud.name);
        MaintenanceAction action = new MaintenanceAction(target);

        if (action.hasMaintenanceWindows()) {
          targetList.add(action);
        }
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Error while processing metadata for cloud: " + cloud.name, e);
      }
    }

    return targetList.stream()
        .filter(action -> PermissionManager.canView(action.getTarget()))
        .toList();
  }

  /**
   * Gets all Agents' maintenance actions (called by jelly).
   *
   * @return List of all Agent actions.
   */
  public List<MaintenanceAction> getAgentTargets() throws IOException {
    List<MaintenanceAction> allTargets = getTargets();
    return allTargets.stream()
            .filter(MaintenanceAction::isAgent)
            .toList();
  }

  /**
   * Gets all Clouds' maintenance actions (called by jelly).
   *
   * @return List of all Cloud actions.
   */
  public List<MaintenanceAction> getCloudTargets() throws IOException {
    List<MaintenanceAction> allTargets = getTargets();
    return allTargets.stream()
            .filter(MaintenanceAction::isCloud)
            .toList();
  }

  private void setError(Throwable error) {
    this.error = error;
  }

  /**
   * The message of the last error that occurred.
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
    int activeAgents = 0;
    int totalAgents = 0;
    int activeClouds = 0;
    int totalClouds = 0;
    List<MaintenanceAction> mwList;
    try {
      mwList = getTargets();
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Error while reading maintenance windows", e);
      return null;
    }
    for (MaintenanceAction ma : mwList) {
      if (ma.isAgent()) {
        totalAgents++;
        if (ma.hasActiveMaintenanceWindows()) {
          activeAgents++;
        }
      } else if (ma.isCloud()) {
        totalClouds++;
        if (ma.hasActiveMaintenanceWindows()) {
          activeClouds++;
        }
      }
    }
    if (totalAgents + totalClouds == 0) {
      return null;
    }
    String text = (activeAgents + activeClouds) + "/" + (totalAgents + totalClouds);
    StringBuilder tooltip = new StringBuilder();

    if (totalAgents > 0) {
      tooltip.append(activeAgents)
             .append("/")
             .append(totalAgents)
             .append(getVerb(activeAgents, "agent"))
             .append(" an active maintenance window.\n");
    }

    if (totalClouds > 0) {
      tooltip.append(activeClouds)
             .append("/")
             .append(totalClouds)
             .append(getVerb(activeClouds, "cloud"))
             .append(" an active maintenance window.\n");
    }

    Badge.Severity severity = Badge.Severity.INFO;
    if ((activeAgents + activeClouds) > 0) {
      severity = Badge.Severity.WARNING;
    }

    return new Badge(text, tooltip.toString().trim(), severity);
  }

  private String getVerb(int count, String target) {
    if (count == 1) {
      return " %s has".formatted(target);
    }
    return " %ss have".formatted(target);
  }

  /**
   * Delete given maintenance window.
   *
   * @param id The id of the maintenance to delete
   * @param targetKey The key of the target to which the maintenance belongs
   */
  @JavaScriptMethod
  public boolean deleteMaintenance(String id, String targetKey) {
    MaintenanceTarget target = MaintenanceTarget.fromKey(targetKey);
    if (PermissionManager.canDelete(target)) {
      try {
        MaintenanceHelper.getInstance().deleteMaintenanceWindow(targetKey, id);
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
   * @param json An json with maintenance ids to delete and corresponding target keys
   */
  @JavaScriptMethod
  public String[] deleteMultiple(JSONObject json) {
    Map<String, String> mwList = (Map<String, String>) JSONObject.toBean(json, Map.class);
    List<String> deletedList = new ArrayList<>();
    for (Entry<String, String> entry : mwList.entrySet()) {
      String targetKey = entry.getValue();
      MaintenanceTarget target = MaintenanceTarget.fromKey(targetKey);
      if (PermissionManager.canDelete(target)) {
        String id = entry.getKey();
        try {
          MaintenanceHelper.getInstance().deleteMaintenanceWindow(targetKey, id);
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
  public Map<String, Boolean> getMaintenanceStatus() throws IOException {
    Map<String, Boolean> statusList = new HashMap<>();
    for (MaintenanceAction action : getTargets()) {
      try {
        if (!PermissionManager.canView(action.getTarget())) {
          continue;
        }

        MaintenanceTarget target = action.getTarget();
        for (MaintenanceWindow mw : MaintenanceHelper.getInstance().getMaintenanceWindows(target.toKey())) {
          if (!mw.isMaintenanceOver()) {
            statusList.put(mw.getId(), mw.isMaintenanceScheduled());
          }
        }
      } catch (IOException ioe) {
        LOGGER.log(Level.WARNING, "Failed to read maintenance windows", ioe);
      }
    }
    return statusList;
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
   * Adds a planned or recurring maintenance window to a list of targets.
   *
   * @param req StaplerRequest2
   * @param rsp StaplerResponse2
   * @throws IOException      when saving xml failed
   * @throws ServletException when reading the form failed
   */
  @POST
  public void doAdd(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException, ServletException {
    Jenkins j = Jenkins.get();

    // ========== CLOUDS WINDOW SUBMISSION ==========
    // Check if cloud form (URL parameters)
    String[] cloudParams = req.getParameterValues("clouds");
    if (cloudParams != null && cloudParams.length > 0) {
      MaintenanceTarget mt = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, cloudParams[0]);
      if (!PermissionManager.canModify(mt)) {
        rsp.sendError(403, "You do not have permission to add cloud maintenance windows");
        return;
      }

      MaintenanceWindow maintenanceWindow = null;
      RecurringMaintenanceWindow recurringMaintenanceWindow = null;

      if (req.hasParameter("startTime") && req.hasParameter("endTime")) {
        // Cloud form - for simple maintenance windows
        String startTime = req.getParameter("startTime");
        String endTime = req.getParameter("endTime");
        String reason = req.getParameter("reason");

        maintenanceWindow = new MaintenanceWindow(startTime, endTime, reason);
      } else if (req.hasParameter("startTimeSpec") && req.hasParameter("duration")) {
        // Parsing cloud form for recurring maintenance windows
        String startTimeSpec = req.getParameter("startTimeSpec");
        String reason = req.getParameter("reason");
        String duration = req.getParameter("duration");

        recurringMaintenanceWindow = new RecurringMaintenanceWindow(startTimeSpec, reason, duration);
      } else {
        IllegalArgumentException iae = new IllegalArgumentException("Error parsing maintenance window configurations");
        LOGGER.warning(iae.getMessage());
        setError(iae);
        rsp.sendRedirect(".");
        return;
      }

      for (String cloudName : cloudParams) {
        Cloud cloud = j.clouds.getByName(cloudName);
        if (cloud == null) {
          LOGGER.warning("Could not find cloud: " + cloudName);
          continue;
        }

        try {
          MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, cloud.name);
          if (maintenanceWindow != null) {
            MaintenanceHelper.getInstance().addMaintenanceWindow(target.toKey(), maintenanceWindow);
          } else {
            MaintenanceHelper.getInstance().addRecurringMaintenanceWindow(target.toKey(), recurringMaintenanceWindow);
          }
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, "Error adding cloud maintenance window", e);
          setError(e);
        }
      }
      rsp.sendRedirect(".");
      return;
    }

    // ========= AGENTS WINDOW SUBMISSION =========

    JSONObject src = req.getSubmittedForm();
    String labelString = src.optString("label");
    Label label = j.getLabel(labelString);
    if (label == null) {
      rsp.sendRedirect(".");
      return;
    }
    boolean isRecurring = src.has("startTimeSpec");
    MaintenanceWindow maintenanceWindow = isRecurring ? null : req.bindJSON(MaintenanceWindow.class, src);
    RecurringMaintenanceWindow recurringMaintenanceWindow = isRecurring ? req.bindJSON(RecurringMaintenanceWindow.class, src) : null;

    Set<Node> nodes = label.getNodes();
    LOGGER.log(Level.FINER, "Adding {0} maintenance windows: {1}",
        new Object[]{ isRecurring ? "recurring" : "planned", maintenanceWindow });
    LOGGER.log(Level.FINER, "Adding {0} maintenance windows for agents: {1}",
        new Object[]{ isRecurring ? "recurring" : "planned", nodes });

    nodes.stream()
        .filter(n -> n.toComputer() instanceof SlaveComputer && !(n.toComputer() instanceof AbstractCloudComputer)
            && Objects.requireNonNull(n.toComputer()).getRetentionStrategy() instanceof AgentMaintenanceRetentionStrategy
            && n.hasAnyPermission(MaintenanceAction.CONFIGURE_AND_DISCONNECT))
        .forEach(n -> {
          try {
            SlaveComputer computer = (SlaveComputer) n.toComputer();
            MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.AGENT, computer.getName());
            if (isRecurring) {
              MaintenanceHelper.getInstance().addRecurringMaintenanceWindow(target.toKey(), recurringMaintenanceWindow);
            } else {
              MaintenanceHelper.getInstance().addMaintenanceWindow(target.toKey(), maintenanceWindow);
            }
          } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while adding maintenance window", e);
            setError(e);
          }
        });
    rsp.sendRedirect(".");
  }

  @JavaScriptMethod
  public boolean deleteRecurringMaintenance(String id, String targetKey) {
    MaintenanceTarget target = MaintenanceTarget.fromKey(targetKey);
    if (PermissionManager.canDelete(target)) {
      try {
        MaintenanceHelper.getInstance().deleteRecurringMaintenanceWindow(targetKey, id);
        return true;
      } catch (Throwable e) {
        LOGGER.log(Level.WARNING, "Error deleting recurring maintenance", e);
      }
    }
    return false;
  }

  @JavaScriptMethod
  public String[] deleteMultipleRecurring(JSONObject json) {
    Map<String, String> mwList = (Map<String, String>) JSONObject.toBean(json, Map.class);
    List<String> deletedList = new ArrayList<>();
    for (Entry<String, String> entry : mwList.entrySet()) {
      String targetKey = entry.getValue();
      MaintenanceTarget target = MaintenanceTarget.fromKey(targetKey);
      if (PermissionManager.canDelete(target)) {
        try {
          MaintenanceHelper.getInstance().deleteRecurringMaintenanceWindow(targetKey, entry.getKey());
          deletedList.add(entry.getKey());
        } catch (Throwable e) {
          LOGGER.log(Level.WARNING, "Error deleting recurring maintenance window", e);
        }
      }
    }
    return deletedList.toArray(new String[0]);
  }

  public Class<MaintenanceWindow> getMaintenanceWindowClass() {
    return MaintenanceWindow.class;
  }
  
  public Class<RecurringMaintenanceWindow> getRecurringMaintenanceWindowClass() {
    return RecurringMaintenanceWindow.class;
  }
}
