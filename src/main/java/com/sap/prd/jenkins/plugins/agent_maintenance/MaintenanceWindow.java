package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.slaves.OfflineCause;
import hudson.util.FormValidation;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.springframework.security.core.Authentication;

/**
 * Describes a maintenance window.
 */
public class MaintenanceWindow extends AbstractDescribableImpl<MaintenanceWindow> implements Comparable<MaintenanceWindow> {

  public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private final String startTime;
  private final String endTime;
  private final String reason;
  private final boolean takeOnline;
  private final boolean keepUpWhenActive;
  private final int maxWaitMinutes;
  private final String userid;
  private String id;
  private transient LocalDateTime startDateTime;
  private transient LocalDateTime endDateTime;
  private transient boolean aborted;

  /**
   * Create a new maintenance window.
   *
   * @param startTime        Start time
   * @param endTime          End time
   * @param reason           Reason
   * @param takeOnline       Take online at end of maintenance
   * @param keepUpWhenActive Keep up while builds are running
   * @param maxWaitMinutes   Max waitung time before cancelling running builds
   * @param userid           Userid that created the maintenance window
   * @param id               ID of the maintenance, use <code>null</code> to
   *                         generate a new id
   */
  @DataBoundConstructor
  public MaintenanceWindow(String startTime, String endTime, String reason, boolean takeOnline, boolean keepUpWhenActive,
      int maxWaitMinutes, String userid, String id) {
    startDateTime = LocalDateTime.parse(startTime, DATE_FORMATTER);
    endDateTime = LocalDateTime.parse(endTime, DATE_FORMATTER);
    this.startTime = startTime;
    this.endTime = endTime;
    this.reason = reason;
    this.takeOnline = takeOnline;
    this.maxWaitMinutes = maxWaitMinutes;
    this.keepUpWhenActive = keepUpWhenActive;
    if (Util.fixEmptyAndTrim(userid) == null) {
      Authentication auth = Jenkins.getAuthentication2();
      userid = "System";
      if (auth != ACL.SYSTEM2) {
        userid = auth.getName();
      }
    }
    this.userid = userid;
    if (Util.fixEmptyAndTrim(id) == null) {
      id = UUID.randomUUID().toString();
    }
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public String getUserid() {
    return userid;
  }

  public boolean isKeepUpWhenActive() {
    return keepUpWhenActive;
  }

  public boolean isAborted() {
    return aborted;
  }

  public void setAborted(boolean aborted) {
    this.aborted = aborted;
  }

  public int getMaxWaitMinutes() {
    return maxWaitMinutes;
  }

  public boolean isTakeOnline() {
    return takeOnline;
  }

  protected Object readResolve() {
    startDateTime = LocalDateTime.parse(startTime, DATE_FORMATTER);
    endDateTime = LocalDateTime.parse(endTime, DATE_FORMATTER);

    if (id == null) {
      id = UUID.randomUUID().toString();
    }
    return this;
  }

  public String getStartTime() {
    return startTime;
  }

  public String getEndTime() {
    return endTime;
  }

  public String getReason() {
    return reason;
  }

  /**
   * Checks if max waiting time is finished.
   *
   * @return true when waiting is finished
   */
  public boolean isMaxWaitTimeFinished() {
    if (maxWaitMinutes < 0) {
      return false;
    }
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime maxWaitTime = startDateTime.plusMinutes(maxWaitMinutes);
    return now.isAfter(maxWaitTime);
  }

  public boolean isMaintenanceScheduled() {
    LocalDateTime now = LocalDateTime.now();
    return now.isAfter(startDateTime) && now.isBefore(endDateTime);
  }

  public boolean isMaintenanceOver() {
    LocalDateTime now = LocalDateTime.now();
    return now.isAfter(endDateTime);
  }

  public OfflineCause getOfflineCause() {
    return new MaintenanceOfflineCause(this);
  }

  /** Descriptor for UI only. */
  @Extension
  public static class DescriptorImpl extends Descriptor<MaintenanceWindow> {

    @Override
    public String getDisplayName() {
      return "";
    }

    /**
     * UI only, validate the start time.
     *
     * @param startTime start time
     * @param endTime   end time
     * @return Formvalidation with the check result
     */
    public FormValidation doCheckStartTime(@QueryParameter String startTime, @QueryParameter("endTime") String endTime) {
      if (!isValidDate(startTime)) {
        return FormValidation.error("\"%s\" is not a valid date for start time", startTime);
      }
      if (!isValidDate(endTime)) {
        return FormValidation.error("\"%s\" is not a valid date for end time", endTime);
      }
      if (compareTimes(startTime, endTime) > 0) {
        return FormValidation.warning("Start time is after end time.");
      }

      return FormValidation.ok();
    }

    private boolean isValidDate(String date) {
      try {
        LocalDateTime.parse(date, DATE_FORMATTER);
      } catch (DateTimeParseException e) {
        return false;
      }
      return true;
    }
  }

  private static int compareTimes(String time1, String time2) {
    LocalDateTime startTimeCalendar = LocalDateTime.parse(time1, DATE_FORMATTER);
    LocalDateTime endTimeCalendar = LocalDateTime.parse(time2, DATE_FORMATTER);
    return startTimeCalendar.compareTo(endTimeCalendar);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((endTime == null) ? 0 : endDateTime.hashCode());
    result = prime * result + (keepUpWhenActive ? 1231 : 1237);
    result = prime * result + maxWaitMinutes;
    result = prime * result + ((reason == null) ? 0 : reason.hashCode());
    result = prime * result + ((startTime == null) ? 0 : startDateTime.hashCode());
    result = prime * result + (takeOnline ? 1231 : 1237);
    return result;
  }

  @Override
  @SuppressWarnings("checkstyle:NeedBraces")
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MaintenanceWindow other = (MaintenanceWindow) obj;
    if (endTime == null) {
      if (other.endTime != null)
        return false;
    } else if (!endDateTime.equals(other.endDateTime))
      return false;
    if (keepUpWhenActive != other.keepUpWhenActive)
      return false;
    if (maxWaitMinutes != other.maxWaitMinutes)
      return false;
    if (reason == null) {
      if (other.reason != null)
        return false;
    } else if (!reason.equals(other.reason))
      return false;
    if (startTime == null) {
      if (other.startTime != null)
        return false;
    } else if (!startDateTime.equals(other.startDateTime))
      return false;
    if (takeOnline != other.takeOnline)
      return false;
    return true;
  }
  // CHECKSTYLE:ON: EmptyBlock

  @Override
  public int compareTo(MaintenanceWindow other) {
    return startDateTime.compareTo(endDateTime);
  }
}
