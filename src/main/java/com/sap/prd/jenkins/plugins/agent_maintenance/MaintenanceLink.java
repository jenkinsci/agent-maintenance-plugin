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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.jenkins.ui.icon.IconSpec;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.verb.POST;

/**
 * Link on manage Jenkins page to list all maintenance windows of all agents.
 */
@Extension
public class MaintenanceLink extends ManagementLink implements IconSpec {
  private static final Logger LOGGER = Logger.getLogger(MaintenanceLink.class.getName());

  private transient Throwable error;

  static {
    IconSet.icons
        .addIcon(new Icon("icon-agent-maintenances icon-sm", "plugin/agent-maintenance/images/maintenance.svg", Icon.ICON_SMALL_STYLE));
    IconSet.icons
        .addIcon(new Icon("icon-agent-maintenances icon-md", "plugin/agent-maintenance/images/maintenance.svg", Icon.ICON_MEDIUM_STYLE));
    IconSet.icons
        .addIcon(new Icon("icon-agent-maintenances icon-ld", "plugin/agent-maintenance/images/maintenance.svg", Icon.ICON_LARGE_STYLE));
    IconSet.icons
        .addIcon(new Icon("icon-agent-maintenances icon-xlg", "plugin/agent-maintenance/images/maintenance.svg", Icon.ICON_XLARGE_STYLE));
  }

  @Override
  public String getDescription() {
    return Messages.MaintenanceLink_description();
  }

  @Override
  public String getDisplayName() {
    return Messages.MaintenanceLink_displayName();
  }

  @Override
  public String getIconClassName() {
    return "icon-agent-maintenances";
  }

  @Override
  public String getIconFileName() {
    return "/plugin/agent-maintenance/images/maintenance.svg";
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

  /**
   * Delete a maintenance window.
   *
   * @param req StaplerRequest
   * @param rsp StaplerResponse
   * @throws IOException      when saving xml failed
   * @throws ServletException when reading the form failed
   */
  @POST
  public void doDelete(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
    JSONObject src = req.getSubmittedForm();
    src.forEach((key, value) -> {
      if (value instanceof JSONArray) {
        try {
          JSONArray data = (JSONArray) value;
          String computerName = data.getString(0);
          boolean delete = data.getBoolean(1);
          if (delete && hasPermission(computerName)) {
            MaintenanceHelper.getInstance().deleteMaintenanceWindow(computerName, key);
          }
        } catch (Throwable e) {
          LOGGER.log(Level.WARNING, "Error while deleting maintenance window", e);
          setError(e);
        }
      }
    });
    rsp.sendRedirect(".");
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
  public void doAdd(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
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
