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

  private static final CloudUuidStore CLOUD_UUID_STORE = CloudUuidStore.getInstance();

  private transient Throwable error;

  @Override
  public String getDescription() {
    return Messages.MaintenanceLink_description();
  }

  @Override
  public String getDisplayName() {
    boolean hasClouds, hasAgents;
    try {
      hasClouds = !getCloudTargets().isEmpty();
      hasAgents = !getAgentTargets().isEmpty();
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Error while reading cloud metadata", e);
      throw new RuntimeException(e);
    }
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
        if (action.hasMaintenanceWindows()) {
          targetList.add(action);
        }
      }
    }

    // New: Adding clouds to the list
    for (Cloud cloud : j.clouds) {
      try {
        String uuid = CloudUuidStore.getInstance().getUuidIfPresent(cloud);
        MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, cloud.name, uuid);
        MaintenanceAction action = new MaintenanceAction(target);

        if (action.hasMaintenanceWindows()) {
          targetList.add(action);
        }
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Error while processing metadata for cloud: " + cloud.name, e);
      }
    }

    return targetList;
  }

  /**
   * Gets all Agents' maintenance actions.
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
   * Gets all Clouds' maintenance actions.
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
    if (hasPermission(targetKey)) {
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
      if (hasPermission(targetKey)) {
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
        if (!action.hasPermissions()) {
          continue;
        }

        MaintenanceTarget target = action.getTarget();
        if (target != null) {
          for (MaintenanceWindow mw : MaintenanceHelper.getInstance().getMaintenanceWindows(target.toKey())) {
            if (!mw.isMaintenanceOver()) {
              statusList.put(mw.getId(), mw.isMaintenanceScheduled());
            }
          }
        }
      } catch (IOException ioe) {
        LOGGER.log(Level.WARNING, "Failed to read maintenance windows", ioe);
      }
    }
    return statusList;
  }

  /**
   * Returns list of available clouds for multi select.
   *
   * @return List of available clouds
   */
  public List<CloudOption> getAvailableClouds() {
    List<CloudOption> options = new ArrayList<>();
    Map<String, Boolean> duplicateMap = new HashMap<>();
    Jenkins j = Jenkins.get();

    for (Cloud cloud : j.clouds) {
      duplicateMap.put(cloud.name, CLOUD_UUID_STORE.hasDuplicates(cloud.name));
    }

    for (Cloud cloud : j.clouds) {
      String uuid = CLOUD_UUID_STORE.getUuidIfPresent(cloud);
      boolean hasDuplicate = duplicateMap.getOrDefault(cloud.name, false);
      String shortUuid = (uuid == null) ? null : uuid.substring(0, 8);
      options.add(new CloudOption(cloud.name, uuid, shortUuid, hasDuplicate));
    }
    return options;
  }

  private boolean hasPermission(String targetKey) {
    MaintenanceAction action = new MaintenanceAction(MaintenanceTarget.fromKey(targetKey));
    return action.hasPermissions();
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
   * Add a maintenance window to a list of targets.
   *
   * @param req StaplerRequest2
   * @param rsp StaplerResponse2
   * @throws IOException      when saving xml failed
   * @throws ServletException when reading the form failed
   */
  @POST
  public void doAdd(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException, ServletException {
    Jenkins j = Jenkins.get();

    // Check if cloud form (URL parameters)
    String[] cloudParams = req.getParameterValues("clouds");
    if (cloudParams != null && cloudParams.length > 0) {
      if (!j.hasPermission(Jenkins.ADMINISTER)) {
        rsp.sendError(403, "You do not have permission to add cloud maintenance windows");
        return;
      }

      // Cloud form - simple constructor (no agent fields)
      String startTime = req.getParameter("startTime");
      String endTime = req.getParameter("endTime");
      String reason = req.getParameter("reason");

      MaintenanceWindow maintenanceWindow = new MaintenanceWindow(startTime, endTime, reason);

      for (String cloudParam : cloudParams) {
        String[] parts = cloudParam.split("::", 2);
        if (parts.length != 2) {
          LOGGER.log(Level.WARNING, "Invalid cloud parameter format: {0}", cloudParam);
          continue;
        }
        String cloudName = parts[0];
        String uuidStr = parts[1];
        String uuid = "null".equals(uuidStr) ? null : uuidStr;
        Cloud cloud = null;
        if (uuid == null) {
          cloud = j.getCloud(cloudName);
        } else {
          for (Cloud c : j.clouds) {
            if (c.name.equals(cloudName) && uuid.equals(CLOUD_UUID_STORE.getUuidIfPresent(c))) {
              cloud = c;
              break;
            }
          }
        }

        if (cloud == null) {
          LOGGER.warning("Could not find cloud with name " + cloudName + " and UUID " + uuid);
          continue;
        }

        try {
          MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, cloud.name, uuid);
          MaintenanceHelper.getInstance().addMaintenanceWindow(target.toKey(), maintenanceWindow);
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, "Error adding cloud maintenance window", e);
          setError(e);
        }
      }
      rsp.sendRedirect(".");
      return;
    }

    JSONObject src = req.getSubmittedForm();
    MaintenanceWindow maintenanceWindow = req.bindJSON(MaintenanceWindow.class, src);

    String labelString = src.optString("label");
    Label label = j.getLabel(labelString);
    if (label != null) {
      Set<Node> nodes = label.getNodes();
      LOGGER.log(Level.FINER, "Adding maintenance windows {0}", maintenanceWindow);
      LOGGER.log(Level.FINER, "Adding maintenance windows for agents: {0}", nodes);
  
      nodes.stream()
          .filter(n -> n.toComputer() instanceof SlaveComputer && !(n.toComputer() instanceof AbstractCloudComputer)
              && Objects.requireNonNull(n.toComputer()).getRetentionStrategy() instanceof AgentMaintenanceRetentionStrategy
              && n.hasAnyPermission(MaintenanceAction.CONFIGURE_AND_DISCONNECT))
          .forEach(n -> {
            try {
              SlaveComputer computer = (SlaveComputer) n.toComputer();
              MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.AGENT, computer.getName());
              MaintenanceHelper.getInstance().addMaintenanceWindow(target.toKey(), maintenanceWindow);
            } catch (Exception e) {
              LOGGER.log(Level.WARNING, "Error while adding maintenance window", e);
              setError(e);
            }
          });
    }
    rsp.sendRedirect(".");
  }

    /**
     * Helper class for cloud selection in the UI.
     */
    public record CloudOption(String name, String uuid, String shortUuid, boolean hasDuplicate) {

    public String getValue() {
        return name + "::" + (uuid == null ? "null" : uuid);
      }

      public String getDisplayName() {
        if (hasDuplicate) {
          return name + " (" + shortUuid + ")";
        }
        return name;
      }
    }

  public Class<MaintenanceWindow> getMaintenanceWindowClass() {
    return MaintenanceWindow.class;
  }
}
