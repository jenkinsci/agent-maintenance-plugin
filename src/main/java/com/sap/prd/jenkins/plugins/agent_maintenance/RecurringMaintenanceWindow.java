package com.sap.prd.jenkins.plugins.agent_maintenance;

import static hudson.Util.fixNull;

import antlr.ANTLRException;
import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.scheduler.CronTabList;
import hudson.security.ACL;
import hudson.util.FormValidation;
import java.io.ObjectStreamException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import org.springframework.security.core.Authentication;

/**
 * Defines a recurring maintenance window based on a cron like schedule.
 */
public class RecurringMaintenanceWindow extends AbstractDescribableImpl<RecurringMaintenanceWindow> {

  private static final CronDefinition cronDefinition = CronDefinitionBuilder.defineCron()
          .withMinutes().withValidRange(0, 59).withStrictRange().and()
          .withHours().withValidRange(0, 23).withStrictRange().and()
          .withDayOfMonth().withValidRange(1, 31).supportsL().supportsW().supportsLW().supportsQuestionMark().and()
          .withMonth().withValidRange(1, 12).withStrictRange().and()
          .withDayOfWeek().withValidRange(0, 7).withMondayDoWValue(1).withIntMapping(7, 0).supportsHash()
          .supportsL().supportsQuestionMark().withStrictRange().and().instance();

  private static final CronParser parser = new CronParser(cronDefinition);
  /*
   * The interval between 2 check runs in minutes.
   */
  @SuppressFBWarnings("MS_SHOULD_BE_FINAL") // Used to set the check interval
  @Restricted(NoExternalUse.class)
  public static int CHECK_INTERVAL_MINUTES = Integer.getInteger(RecurringMaintenanceWindow.class.getName() + ".CHECK_INTERVAL_MINUTES", 15);

  /*
   * The amount of time a maintenance window is created in advance in days
   */
  @SuppressFBWarnings("MS_SHOULD_BE_FINAL") // Used to control usage of binary or shell wrapper
  @Restricted(NoExternalUse.class)
  public static int LEAD_TIME_DAYS = Integer.getInteger(RecurringMaintenanceWindow.class.getName() + ".LEAD_TIME_DAYS", 7);

  private static final Logger LOGGER = Logger.getLogger(RecurringMaintenanceWindow.class.getName());
  private final String reason;
  private final boolean takeOnline;
  private final boolean keepUpWhenActive;
  private final String maxWaitMinutes;
  private final String userid;
  private String id;
  private final String startTimeSpec;
  private final int duration;
  private long nextCheck = 0;
  private transient Cron cron;

  /**
   * Creates a new recurring maintenance window.
   *
   * @param startTimeSpec Start time
   * @param reason Reason
   * @param takeOnline Take online at end of maintenance
   * @param keepUpWhenActive Keep up while builds are running
   * @param maxWaitMinutes Max waiting time before canceling running builds.
   * @param duration Duration of the maintenance
   * @param userid Userid that created the maintenance window
   * @param id ID of the maintenance, use <code>null</code> to generate a new id
   * @param nextCheck timestamp when the next check should be performed
   * @throws ANTLRException When parsing the crontab list fails
   */
  @DataBoundConstructor
  public RecurringMaintenanceWindow(String startTimeSpec, String reason, boolean takeOnline, boolean keepUpWhenActive,
                                    String maxWaitMinutes, String duration, String userid, String id, long nextCheck) {
    this.startTimeSpec = startTimeSpec;
    this.cron = parser.parse(startTimeSpec);
    this.reason = reason;
    this.takeOnline = takeOnline;
    this.maxWaitMinutes = maxWaitMinutes;
    this.keepUpWhenActive = keepUpWhenActive;
    this.duration = MaintenanceHelper.parseDurationString(duration);
    this.nextCheck = nextCheck;
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

  protected synchronized Object readResolve() throws ObjectStreamException {
    cron = parser.parse(startTimeSpec);
    return this;
  }

  public String getStartTimeSpec() {
    return startTimeSpec;
  }

  public int getDuration() {
    return duration;
  }

  public String getReason() {
    return reason;
  }

  public boolean isTakeOnline() {
    return takeOnline;
  }

  public boolean isKeepUpWhenActive() {
    return keepUpWhenActive;
  }

  public String getMaxWaitMinutes() {
    return maxWaitMinutes;
  }

  public String getUserid() {
    return userid;
  }

  @Restricted(NoExternalUse.class)
  public long getNextCheck() {
    return nextCheck;
  }

  @Restricted(NoExternalUse.class)
  public String getId() {
    return id;
  }

  /**
   * Returns a list of maintenance windows that should be put into the scheduled maintenance windows
   * of an agent.
   * Updates the nextCheck interval for the recurring window.
   *
   * @return The list of maintenance windows.
   */
  @NonNull
  @Restricted(NoExternalUse.class)
  public synchronized Set<MaintenanceWindow> getFutureMaintenanceWindows() {
    LOGGER.log(Level.FINER, "Checking for future maintenance Windows.");
    ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    ZoneId zoneId = now.getZone();

    Set<MaintenanceWindow> futureMaintenanceWindows = new TreeSet<>();
    if (now.toEpochSecond() > nextCheck) {

      Instant instant = Instant.ofEpochSecond(nextCheck);
      ZonedDateTime time = ZonedDateTime.ofInstant(instant, zoneId).truncatedTo(ChronoUnit.MINUTES);
      ZonedDateTime endCheckTime = time.plus(CHECK_INTERVAL_MINUTES, ChronoUnit.MINUTES);

      if (endCheckTime.isBefore(now)) {
        endCheckTime = now.plus(CHECK_INTERVAL_MINUTES, ChronoUnit.MINUTES);
      }
      ZonedDateTime nextCheckTime = endCheckTime;

      time = time.plus(LEAD_TIME_DAYS * 24L, ChronoUnit.HOURS);
      if (time.isBefore(now)) {
        time = now;
      }
      endCheckTime = endCheckTime.plus(LEAD_TIME_DAYS * 24L, ChronoUnit.HOURS);
      endCheckTime = endCheckTime.minus(1, ChronoUnit.MINUTES);

      LOGGER.log(Level.FINE, "Check for maintenance window starts between: {0} and {1}", new Object[] { time.toString(),
          endCheckTime.toString()});
      while (endCheckTime.isAfter(time)) {
        if (ExecutionTime.forCron(cron).isMatch(time)) {
          LOGGER.log(Level.FINER, "Time matched: {0}", time.toString());
          futureMaintenanceWindows.add(getMaintenanceWindow(time));
        }
        time = time.plus(1, ChronoUnit.MINUTES);
      }
      nextCheck = nextCheckTime.toEpochSecond();
      LOGGER.log(Level.FINER, "Setting next Check time to: {0}", nextCheckTime.toString());
    }
    return futureMaintenanceWindows;
  }

  private MaintenanceWindow getMaintenanceWindow(ZonedDateTime time) {
    LocalDateTime startTime = LocalDateTime.ofInstant(time.toInstant(), time.getZone());
    LocalDateTime endTime = startTime.plus(duration, ChronoUnit.MINUTES);
    return new MaintenanceWindow(startTime, endTime, reason, takeOnline, keepUpWhenActive,
        maxWaitMinutes, userid, "");
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + duration;
    result = prime * result + (keepUpWhenActive ? 1231 : 1237);
    result = prime * result + ((maxWaitMinutes == null) ? 0 : maxWaitMinutes.hashCode());
    result = prime * result + ((reason == null) ? 0 : reason.hashCode());
    result = prime * result + ((startTimeSpec == null) ? 0 : startTimeSpec.hashCode());
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
    RecurringMaintenanceWindow other = (RecurringMaintenanceWindow) obj;
    if (startTimeSpec == null) {
      if (other.startTimeSpec != null)
        return false;
    } else if (!startTimeSpec.equals(other.startTimeSpec))
      return false;
    if (keepUpWhenActive != other.keepUpWhenActive)
      return false;
    if (!maxWaitMinutes.equals(other.maxWaitMinutes))
      return false;
    if (reason == null) {
      if (other.reason != null) {
        return false;
      }
    } else if (!reason.equals(other.reason))
      return false;
    if (takeOnline != other.takeOnline)
      return false;
    return true;
  }

  /** Descriptor for UI only. */
  @Extension
  public static class DescriptorImpl extends Descriptor<RecurringMaintenanceWindow> {

    @Override
    public String getDisplayName() {
      return "";
    }

    /**
     * Performs syntax check.
     */
    @POST
    public FormValidation doCheckStartTimeSpec(@QueryParameter String value) {
      try {
        Cron cron = parser.parse(value);
        ExecutionTime et = ExecutionTime.forCron(cron);
        ZonedDateTime last = et.lastExecution(ZonedDateTime.now()).orElse(null);
        ZonedDateTime next = et.nextExecution(ZonedDateTime.now()).orElse(null);
        if (next != null && last != null) {
          String msg = "Would have last run at " + last + "; would next run at " + next;
          return FormValidation.warning(msg);
        }
        return FormValidation.ok();
      } catch (IllegalArgumentException  e) {
        return FormValidation.error(e.getMessage());
      }
    }
  }
}
