package com.sap.prd.jenkins.plugins.agent_maintenance;

import static com.sap.prd.jenkins.plugins.agent_maintenance.MaintenanceWindow.DATE_FORMATTER;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Util;
import hudson.model.Action;
import hudson.model.Computer;
import hudson.security.Permission;
import hudson.slaves.SlaveComputer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.jenkins.ui.icon.IconSpec;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
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
   * The default start time shown in the UI when adding a new maintenance windows.
   * Current time.
   *
   * @return end time
   */
  public boolean hasMaintenanceWindows() {
    try {
      return MaintenanceHelper.getInstance().hasMaintenanceWindows(computer.getName());
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
   * @param rsp Stapler Response
   * @throws IOException      when writing fails
   * @throws ServletException if an error occurs reading the form
   */
  @POST
  public void doAdd(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
    computer.checkAnyPermission(CONFIGURE_AND_DISCONNECT);

    JSONObject src = req.getSubmittedForm();
    MaintenanceWindow mw = req.bindJSON(MaintenanceWindow.class, src);
    MaintenanceHelper.getInstance().addMaintenanceWindow(computer.getName(), mw);
    rsp.sendRedirect(".");
  }

  /**
   * UI method to delete multiple maintenance windows.
   *
   * @param req Stapler Request
   * @param rsp Stapler Response
   * @throws IOException      when writing fails
   * @throws ServletException if an error occurs reading the form
   */
  @POST
  public void doDeleteMultiple(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
    computer.checkAnyPermission(CONFIGURE_AND_DISCONNECT);
    JSONObject src = req.getSubmittedForm();
    src.forEach((key, value) -> {
      if (!key.equals("Jenkins-Crumb")) {
        try {
          boolean delete = (Boolean) value;
          if (delete) {
            MaintenanceHelper.getInstance().deleteMaintenanceWindow(computer.getName(), key);
          }
        } catch (Throwable e) {
          LOGGER.log(Level.WARNING, "Error while deleting maintenance window", e);
        }
      }
    });
    rsp.sendRedirect(".");
  }

  /**
   * UI method to delete a maintenance window.
   *
   * @param req Stapler Request
   * @param rsp Stapler Response
   * @throws IOException when writing fails
   */
  @POST
  public void doDeleteMaintenance(StaplerRequest req, StaplerResponse rsp, @QueryParameter String id) throws IOException {
    computer.checkAnyPermission(CONFIGURE_AND_DISCONNECT);

    if (Util.fixEmptyAndTrim(id) == null) {
      return;
    }
    MaintenanceHelper.getInstance().deleteMaintenanceWindow(computer.getName(), id);
    rsp.sendRedirect(".");
  }

  /**
   * UI method to submit the configuration.
   *
   * @param req Stapler Request
   * @param rsp Stapler Response
   * @throws IOException      when writing fails
   * @throws ServletException if an error occurs reading the form
   */
  @POST
  public synchronized void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
    computer.checkPermission(Computer.CONFIGURE);

    JSONObject src = req.getSubmittedForm();

    List<MaintenanceWindow> newTargets = req.bindJSONToList(MaintenanceWindow.class, src.get("maintenanceWindows"));

    SortedSet<MaintenanceWindow> mwlist = MaintenanceHelper.getInstance().getMaintenanceWindows(computer.getName());
    synchronized (mwlist) {
      mwlist.clear();
      mwlist.addAll(newTargets);
      MaintenanceHelper.getInstance().saveMaintenanceWindows(computer.getName(), mwlist);
    }
    rsp.sendRedirect(".");
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
