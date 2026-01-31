package com.sap.prd.jenkins.plugins.agent_maintenance;

import static com.sap.prd.jenkins.plugins.agent_maintenance.MaintenanceWindow.DATE_FORMATTER;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Util;
import hudson.model.Action;
import hudson.model.Computer;
import hudson.security.Permission;
import hudson.slaves.Cloud;
import hudson.util.FormApply;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.verb.POST;

/**
 * Action to display link to maintenance window configuration.
 */
public class MaintenanceAction implements Action {

  protected final MaintenanceTarget target;

  private static final Logger LOGGER = Logger.getLogger(MaintenanceAction.class.getName());

  @SuppressFBWarnings(value = "MS_PKGPROTECT", justification = "called by Jelly")
  @Restricted(NoExternalUse.class)
  public static final Permission[] CONFIGURE_AND_DISCONNECT = new Permission[]{Computer.DISCONNECT, Computer.CONFIGURE};

  /**
   * Creates MaintenanceAction. Including UUID for clouds.
   */
  public MaintenanceAction(MaintenanceTarget target) {
    this.target = target;
  }

  /**
   * Checks if the action is visible based on permissions.
   *
   * @return true if visible.
   */
  @Restricted(NoExternalUse.class)
  public boolean isVisible() {
    try {
      if (!MaintenanceHelper.getInstance().isValidTarget(target.toKey())) {
        return false;
      }

      if (isAgent()) {
        Computer computer = Jenkins.get().getComputer(target.getName());
        return computer != null
                && (computer.hasPermission(Computer.DISCONNECT)
                        || computer.hasPermission(Computer.CONFIGURE)
                        || computer.hasPermission(Computer.EXTENDED_READ))
                && computer.getNode() != null;
      }
      return Jenkins.get().hasPermission(Jenkins.ADMINISTER);
    } catch (IOException e) {
      return false;
    }
  }

  protected void checkPermission(Permission... permissions) {
    if (isAgent()) {
      Computer c = Jenkins.get().getComputer(target.getName());
      if (c == null) {
        throw new IllegalStateException("Agent '" + target.getName() + "' no longer exists");
      }
      c.checkAnyPermission(permissions);
    } else { // For cloud
      Jenkins.get().checkPermission(Jenkins.ADMINISTER);
    }
  }

  @Override
  public String getIconFileName() {
    if (isVisible()) {
      return "symbol-build-outline plugin-ionicons-api";
    } else {
      return null;
    }
  }

  @Override
  public String getDisplayName() {
    if (isVisible()) {
      if (hasPermissions()) {
        return Messages.MaintenanceAction_maintenanceWindows();
      } else {
        return Messages.MaintenanceAction_view();
      }
    } else {
      return null;
    }
  }

  @Override
  public String getUrlName() {
    return "maintenanceWindows";
  }

  /**
   * Gets the target of this maintenance action.
   *
   * @return the maintenance target
   */
  public MaintenanceTarget getTarget() {
    return target;
  }

  public boolean isAgent() {
    return target.getType() == MaintenanceTarget.TargetType.AGENT;
  }

  public boolean isCloud() {
    return target.getType() == MaintenanceTarget.TargetType.CLOUD;
  }

  public Class<MaintenanceWindow> getMaintenanceWindowClass() {
    return MaintenanceWindow.class;
  }

  public Class<RecurringMaintenanceWindow> getRecurringMaintenanceWindowClass() {
    return RecurringMaintenanceWindow.class;
  }

  /**
   * Checks if Agent's retention strategy is enabled.
   *
   * @return true if it is.
   */
  public boolean isEnabled() {
    if (!isAgent()) {
      return false;
    }
    Computer computer = Jenkins.get().getComputer(target.getName());
    return computer != null && computer.getRetentionStrategy() instanceof AgentMaintenanceRetentionStrategy;
  }

  /**
   * Gets the Cloud associated with the maintenance action.
   *
   * @return <code>Cloud</code> instance of the maintenance action.
   */
  public Cloud getCloud() {
    if (!isCloud()) {
      return null;
    }
    return CloudUuidStore.getInstance().getCloudByTarget(target);
  }

  /**
   * Gets the Agent associated with the maintenance action.
   *
   * @return <code>Computer</code> instance of the maintenance action.
   */
  public Computer getAgentComputer() {
    Computer c = null;
    if (isAgent()) {
      c = Jenkins.get().getComputer(target.getName());
    }
    return c;
  }

  /**
   * Checks if the user has permissions to access MaintenanceWindows.
   *
   * @return true if they do.
   */
  public boolean hasPermissions() {
    if (isAgent()) {
      Computer c = Jenkins.get().getComputer(target.getName());
      return c != null
              && (c.hasPermission(Computer.DISCONNECT)
              || c.hasPermission(Computer.CONFIGURE)
              || c.hasPermission(Computer.EXTENDED_READ));
    } else {
      return Jenkins.get().hasPermission(Jenkins.ADMINISTER);
    }
  }

  /**
   * Checks the given permissions.
   *
   * @param permissions A group of permissions to be checked.
   * @return true if all permissions are granted.
   */
  public boolean hasPermissions(Permission... permissions) {
    if (isAgent()) {
      Computer c = Jenkins.get().getComputer(target.getName());
      return c != null && Arrays.stream(permissions).allMatch(c::hasPermission);
    } else {
      return Jenkins.get().hasPermission(Jenkins.ADMINISTER);
    }
  }

  /**
   * Checks if the user has permissions to delete MaintenanceWindows.
   *
   * @return true if they do.
   */
  public boolean hasDeletePermission() {
    if (isAgent()) {
      Computer c = Jenkins.get().getComputer(target.getName());
      return c != null && (c.hasPermission(Computer.DISCONNECT) || c.hasPermission(Computer.CONFIGURE));
    } else {
      return Jenkins.get().hasPermission(Jenkins.ADMINISTER);
    }
  }

  /**
   * Checks if the user has permissions to delete Cloud windows.
   *
   * @return true if they do.
   */
  public boolean hasCloudDeletePermission() {
    return Jenkins.get().hasPermission(Jenkins.ADMINISTER);
  }

  /**
   * The default start time shown in the UI when adding a new maintenance windows.
   * Current time.
   *
   * @return end time
   */
  public static String getDefaultStartTime() {
    LocalDateTime now = LocalDateTime.now();
    return DATE_FORMATTER.format(now);
  }

  /**
   * The default end time shown in the UI when adding a new maintenance windows.
   * Current time + 1 day.
   *
   * @return end time
   */
  public static String getDefaultEndTime() {
    LocalDateTime now = LocalDateTime.now();
    now = now.plusDays(1);
    return DATE_FORMATTER.format(now);
  }

  /**
   * Return whether there are maintenance windows defined.
   *
   * @return true when there are maintenance windows defined.
   */
  public boolean hasMaintenanceWindows() {
    try {
      return MaintenanceHelper.getInstance().hasMaintenanceWindows(target.toKey());
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Return whether there are active maintenance windows.
   *
   * @return true when there are active maintenance windows defined.
   */
  public boolean hasActiveMaintenanceWindows() {
    try {
      return MaintenanceHelper.getInstance().hasActiveMaintenanceWindows(target.toKey());
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Returns a list of maintenance windows.
   *
   * @return A list of maintenance windows
   */
  public SortedSet<MaintenanceWindow> getMaintenanceWindows() {
    try {
      return Collections.unmodifiableSortedSet(MaintenanceHelper.getInstance().getMaintenanceWindows(target.toKey()));
    } catch (IOException e) {
      return Collections.emptySortedSet();
    }
  }

  /**
   * Returns a list of recurring maintenance windows.
   *
   * @return A list of recurring maintenance windows
   */
  public Set<RecurringMaintenanceWindow> getRecurringMaintenanceWindows() {
    try {
      return Collections.unmodifiableSet(MaintenanceHelper.getInstance().getRecurringMaintenanceWindows(target.toKey()));
    } catch (IOException e) {
      return Collections.emptySortedSet();
    }
  }

  /**
   * UI method to add a maintenance window.
   *
   * @param req Stapler Request
   * @return Response containing the result of the add
   * @throws IOException      when writing fails
   * @throws ServletException if an error occurs reading the form
   */
  @POST
  public HttpResponse doAdd(StaplerRequest2 req) throws IOException, ServletException {
    checkPermission(CONFIGURE_AND_DISCONNECT);

    JSONObject src = req.getSubmittedForm();
    MaintenanceWindow mw = req.bindJSON(MaintenanceWindow.class, src);
    MaintenanceHelper.getInstance().addMaintenanceWindow(target.toKey(), mw);
    return FormApply.success(".");
  }

  /**
   * UI method to add a recurring maintenance window.
   *
   * @param req Stapler Request
   * @param rsp Stapler Response
   * @throws IOException      when writing fails
   * @throws ServletException if an error occurs reading the form
   */
  @POST
  public void doAddRecurring(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException, ServletException {
    checkPermission(CONFIGURE_AND_DISCONNECT);

    JSONObject src = req.getSubmittedForm();
    RecurringMaintenanceWindow rmw = req.bindJSON(RecurringMaintenanceWindow.class, src);
    MaintenanceHelper.getInstance().addRecurringMaintenanceWindow(target.toKey(), rmw);
    rsp.sendRedirect(".");
  }

  /**
   * UI method to delete multiple maintenance windows.
   *
   * @param ids A list of maintenance window ids to delete
   */
  @JavaScriptMethod
  public String[] deleteMultiple(String[] ids) {
    checkPermission(CONFIGURE_AND_DISCONNECT);
    List<String> deletedList = new ArrayList<>();
    for (String id : ids) {
      try {
        MaintenanceHelper.getInstance().deleteMaintenanceWindow(target.toKey(), id);
        deletedList.add(id);
      } catch (Throwable e) {
        LOGGER.log(Level.WARNING, "Error while deleting maintenance window", e);
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
    if (hasPermissions()) {
      try {
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

  /**
   * UI method to delete multiple recurring maintenance windows.
   *
   * @param ids A list of recurring maintenance window ids to delete
   */
  @JavaScriptMethod
  public String[] deleteMultipleRecurring(String[] ids) {
    checkPermission(CONFIGURE_AND_DISCONNECT);
    List<String> deletedList = new ArrayList<>();
    for (String id : ids) {
      try {
        MaintenanceHelper.getInstance().deleteRecurringMaintenanceWindow(target.toKey(), id);
        deletedList.add(id);
      } catch (Throwable e) {
        LOGGER.log(Level.WARNING, "Error while deleting maintenance window", e);
      }
    }
    return deletedList.toArray(new String[0]);
  }

  /**
   * UI method to delete a maintenance window.
   *
   * @param id The id of the maintenance to delete
   */
  @JavaScriptMethod
  public boolean deleteMaintenance(String id) {
    try {
      checkPermission(CONFIGURE_AND_DISCONNECT);
      if (Util.fixEmptyAndTrim(id) == null) {
        return false;
      }
      try {
        MaintenanceHelper.getInstance().deleteMaintenanceWindow(target.toKey(), id);
        return true;
      } catch (IOException ioe) {
        LOGGER.log(Level.WARNING, "Failed to delete maintenance window.", ioe);
        return false;
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Access denied.", e);
      return false;
    }
  }

  /**
   * UI method to delete a recurring maintenance window.
   *
   * @param id The id of the maintenance to delete
   */
  @JavaScriptMethod
  public boolean deleteRecurringMaintenance(String id) {
    try {
      checkPermission(CONFIGURE_AND_DISCONNECT);
      if (Util.fixEmptyAndTrim(id) == null) {
        return false;
      }
      try {
        MaintenanceHelper.getInstance().deleteRecurringMaintenanceWindow(target.toKey(), id);
        return true;
      } catch (IOException ioe) {
        LOGGER.log(Level.WARNING, "Failed to delete recurring maintenance window.", ioe);
        return false;
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Access denied.", e);
      return false;
    }
  }

  /**
   * UI method to submit the configuration.
   *
   * @param req Stapler Request
   * @return Response containing the result of the add
   * @throws IOException      when writing fails
   * @throws ServletException if an error occurs reading the form
   */
  @POST
  public synchronized HttpResponse doConfigSubmit(StaplerRequest2 req) throws IOException, ServletException {
    checkPermission(Computer.CONFIGURE);

    JSONObject src = req.getSubmittedForm();

    List<MaintenanceWindow> newTargets = req.bindJSONToList(MaintenanceWindow.class, src.get("maintenanceWindows"));
    List<RecurringMaintenanceWindow> newRecurringTargets = req.bindJSONToList(RecurringMaintenanceWindow.class,
            src.get("recurringMaintenanceWindows"));

    MaintenanceDefinitions md = MaintenanceHelper.getInstance().getMaintenanceDefinitions(target.toKey());
    synchronized (md) {
      SortedSet<MaintenanceWindow> scheduled = md.getScheduled();
      Set<RecurringMaintenanceWindow> recurring = md.getRecurring();
      scheduled.clear();
      scheduled.addAll(newTargets);
      recurring.clear();
      recurring.addAll(newRecurringTargets);
      MaintenanceHelper.getInstance().saveMaintenanceWindows(target.toKey(), md);
    }
    return FormApply.success(".");
  }

  /**
   * UI method to enable the retention strategy.
   *
   * @param rsp Stapler Response
   * @throws IOException when something goes wrong
   */
  @POST
  public void doEnable(StaplerResponse2 rsp) throws IOException {
    Computer c = getAgentComputer();
    if (c != null) {
      c.checkPermission(Computer.CONFIGURE);
      MaintenanceHelper.getInstance().injectRetentionStrategy(c);
    }
    rsp.sendRedirect(".");
  }

  /**
   * UI method to remove the retention strategy.
   *
   * @param rsp Stapler Response
   * @throws IOException when something goes wrong
   */
  @POST
  public void doDisable(StaplerResponse2 rsp) throws IOException {
    Computer c = getAgentComputer();
    if (c != null) {
      c.checkPermission(Computer.CONFIGURE);
      MaintenanceHelper.getInstance().removeRetentionStrategy(c);
    }

    rsp.sendRedirect(".");
  }

  /**
   * Entry point for the maintenance windows page.
   * Checks permission before showing content.
   */
  public void doIndex(StaplerRequest2 req, StaplerResponse2 rsp)
          throws IOException, ServletException {

    if (isAgent()) {
      Computer c = Jenkins.get().getComputer(target.getName());
      if (c == null) {
        rsp.sendError(HttpServletResponse.SC_NOT_FOUND);  // 404
        return;
      }
      c.checkAnyPermission(Computer.EXTENDED_READ, Computer.CONFIGURE, Computer.DISCONNECT);
    } else {
      Cloud cloud = getCloud();
      if (cloud == null) {
        rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      Jenkins.get().checkPermission(Jenkins.ADMINISTER);
    }

    req.getView(this, "index.jelly").forward(req, rsp);
  }
}
