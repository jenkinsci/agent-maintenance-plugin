package com.sap.prd.jenkins.plugins.agent_maintenance;

import static com.sap.prd.jenkins.plugins.agent_maintenance.MaintenanceWindow.DATE_FORMATTER;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Util;
import hudson.model.Action;
import hudson.model.Computer;
import hudson.security.Permission;
import hudson.slaves.SlaveComputer;
import hudson.util.FormApply;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.verb.POST;

/** Action to display link to maintenance window configuration. */
public class MaintenanceAction implements Action {

  private static final Logger LOGGER = Logger.getLogger(MaintenanceAction.class.getName());

  @SuppressFBWarnings(value = "MS_PKGPROTECT", justification = "called by Jelly")
  @Restricted(NoExternalUse.class)
  public static final Permission[] CONFIGURE_AND_DISCONNECT = new Permission[] { Computer.DISCONNECT, Computer.CONFIGURE }; 

  private final SlaveComputer computer;

  public MaintenanceAction(SlaveComputer computer) {
    this.computer = computer;
  }

  public Computer getComputer() {
    return computer;
  }
  
  private boolean isVisible() {
    return (computer.hasPermission(Computer.DISCONNECT) || computer.hasPermission(Computer.CONFIGURE)
        || computer.hasPermission(Computer.EXTENDED_READ)) && computer.getNode() != null;
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
      if (computer.hasPermission(Computer.DISCONNECT) || computer.hasPermission(Computer.CONFIGURE)) {
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
    if (isVisible()) {
      return "maintenanceWindows";
    } else {
      return null;
    }
  }

  public Class<MaintenanceWindow> getMaintenanceWindowClass() {
    return MaintenanceWindow.class;
  }

  public boolean isEnabled() {
    return computer.getRetentionStrategy() instanceof AgentMaintenanceRetentionStrategy;
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
      return MaintenanceHelper.getInstance().hasMaintenanceWindows(computer.getName());
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
      return MaintenanceHelper.getInstance().hasActiveMaintenanceWindows(computer.getName());
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
      return Collections.unmodifiableSortedSet(MaintenanceHelper.getInstance().getMaintenanceWindows(computer.getName()));
    } catch (IOException e) {
      return Collections.emptySortedSet();
    }
  }

  /**
   * UI method to add a smaintenance window.
   *
   * @param req Stapler Request
   * @return Response containing the result of the add
   * @throws IOException      when writing fails
   * @throws ServletException if an error occurs reading the form
   */
  @POST
  public HttpResponse doAdd(StaplerRequest req) throws IOException, ServletException {
    computer.checkAnyPermission(CONFIGURE_AND_DISCONNECT);

    JSONObject src = req.getSubmittedForm();
    MaintenanceWindow mw = req.bindJSON(MaintenanceWindow.class, src);
    MaintenanceHelper.getInstance().addMaintenanceWindow(computer.getName(), mw);
    return FormApply.success(".");
  }

  /**
   * UI method to delete multiple maintenance windows.
   *
   * @param ids A list of maintenance window ids to delete
   */
  @JavaScriptMethod
  public String[] deleteMultiple(String[] ids) {
    computer.checkAnyPermission(CONFIGURE_AND_DISCONNECT);
    List<String> deletedList = new ArrayList<>();
    for (String id : ids) {
      try {
        MaintenanceHelper.getInstance().deleteMaintenanceWindow(computer.getName(), id);
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
    return statusList;
  }

  /**
   * UI method to delete a maintenance window.
   *
   * @param id The id of the maintenance to delete
   */
  @JavaScriptMethod
  public boolean deleteMaintenance(String id) {
    if (computer.hasAnyPermission(CONFIGURE_AND_DISCONNECT)) {
      if (Util.fixEmptyAndTrim(id) == null) {
        return false;
      }
      try {
        MaintenanceHelper.getInstance().deleteMaintenanceWindow(computer.getName(), id);
        return true;
      } catch (IOException ioe) {
        LOGGER.log(Level.WARNING, "Failed to delete maintenance window.", ioe);
        return false;
      }
    }
    return false;
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
  public synchronized HttpResponse doConfigSubmit(StaplerRequest req) throws IOException, ServletException {
    computer.checkPermission(Computer.CONFIGURE);

    JSONObject src = req.getSubmittedForm();

    List<MaintenanceWindow> newTargets = req.bindJSONToList(MaintenanceWindow.class, src.get("maintenanceWindows"));

    SortedSet<MaintenanceWindow> mwlist = MaintenanceHelper.getInstance().getMaintenanceWindows(computer.getName());
    synchronized (mwlist) {
      mwlist.clear();
      mwlist.addAll(newTargets);
      MaintenanceHelper.getInstance().saveMaintenanceWindows(computer.getName(), mwlist);
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
  public void doEnable(StaplerResponse rsp) throws IOException {
    computer.checkPermission(Computer.CONFIGURE);

    MaintenanceHelper.getInstance().injectRetentionStrategy(computer);
    rsp.sendRedirect(".");
  }

  /**
   * UI method to remove the retention strategy.
   *
   * @param rsp Stapler Response
   * @throws IOException when something goes wrong
   */
  @POST
  public void doDisable(StaplerResponse rsp) throws IOException {
    computer.checkPermission(Computer.CONFIGURE);

    MaintenanceHelper.getInstance().removeRetentionStrategy(computer);
    rsp.sendRedirect(".");
  }
}
