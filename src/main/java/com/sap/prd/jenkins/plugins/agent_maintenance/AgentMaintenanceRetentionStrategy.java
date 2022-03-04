package com.sap.prd.jenkins.plugins.agent_maintenance;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.DescriptorVisibilityFilter;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Slave;
import hudson.slaves.OfflineCause;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SlaveComputer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.GuardedBy;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * {@link RetentionStrategy} that allows to take an agent offline for a defined time window for maintenance.
 */
public class AgentMaintenanceRetentionStrategy extends RetentionStrategy<SlaveComputer> {

  private static final Logger LOGGER = Logger.getLogger(AgentMaintenanceRetentionStrategy.class.getName());

  private RetentionStrategy<SlaveComputer> regularRetentionStrategy;

  @DataBoundConstructor
  public AgentMaintenanceRetentionStrategy(RetentionStrategy<SlaveComputer> regularRetentionStrategy) {
    this.regularRetentionStrategy = regularRetentionStrategy;
  }

  public RetentionStrategy<?> getRegularRetentionStrategy() {
    return regularRetentionStrategy;
  }

  public void setRegularRetentionStrategy(RetentionStrategy<SlaveComputer> regularRetentionStrategy) {
    this.regularRetentionStrategy = regularRetentionStrategy;
  }

  @Override
  public boolean isAcceptingTasks(SlaveComputer c) {
    MaintenanceWindow maintenance = MaintenanceHelper.getInstance().getMaintenance(c.getName());
    if (maintenance != null) {
      return false;
    }
    return regularRetentionStrategy.isAcceptingTasks(c);
  }

  @Override
  public boolean isManualLaunchAllowed(final SlaveComputer c) {
    MaintenanceWindow maintenance = MaintenanceHelper.getInstance().getMaintenance(c.getName());
    if (maintenance != null) {
      return false;
    }
    return regularRetentionStrategy.isManualLaunchAllowed(c);
  }

  @Override
  @GuardedBy("hudson.model.Queue.lock")
  public synchronized long check(final SlaveComputer c) {
    MaintenanceWindow maintenance = MaintenanceHelper.getInstance().getMaintenance(c.getName());
    LOGGER.log(Level.FINER, "Checking for Maintenance Window for agent {0}. online = {1}, idle = {2}",
        new Object[] { c.getName(), c.isOnline(), c.isIdle() });
    if (maintenance != null) {
      LOGGER.log(Level.FINE, "Active Maintenance Window found for agent {0}: startTime = {1}, endTime = {2}",
          new Object[] { c.getName(), maintenance.getStartTime(), maintenance.getEndTime() });

      if (c.isOnline()) {
        if (maintenance.isKeepUpWhenActive()) {
          if (!maintenance.isMaxWaitTimeFinished()) {
            if (c.isIdle()) {
              Queue.withLock(new Runnable() {
                @Override
                public void run() {
                  LOGGER.log(Level.INFO, "Disconnecting agent {0} as it was idle when " + "maintenance window started.",
                      new Object[] { c.getName() });
                  c.disconnect(maintenance.getOfflineCause());
                }
              });
            }
          } else {
            if (maintenance.isAborted()) {
              LOGGER.log(Level.INFO,
                  "Disconnecting agent {0} as it has finished its scheduled uptime and max " + "waiting time for builds to finish is over",
                  new Object[] { c.getName() });
              c.disconnect(maintenance.getOfflineCause());
            } else {
              LOGGER.log(Level.INFO, "Aborting running builds on agent {0} as it has finished its scheduled uptime "
                  + "and max waiting time for builds to finish is over", new Object[] { c.getName() });
              for (Executor e : c.getExecutors()) {
                if (e.isBusy()) {
                  e.interrupt(Result.ABORTED, new MaintenanceInterruption());
                }
              }
              maintenance.setAborted(true);
            }
          }
        } else {
          if (maintenance.isAborted()) {
            // no need to get the queue lock as the user
            // has selected the break builds
            // option!
            LOGGER.log(Level.INFO, "Disconnecting agent {0} as it has finished its scheduled uptime", new Object[] { c.getName() });
            c.disconnect(maintenance.getOfflineCause());
          } else {
            LOGGER.log(Level.INFO, "Aborting running builds on agent {0} as it has finished its scheduled uptime",
                new Object[] { c.getName() });
            for (Executor e : c.getExecutors()) {
              if (e.isBusy()) {
                e.interrupt(Result.ABORTED, new MaintenanceInterruption());
              }
            }
            maintenance.setAborted(true);
          }
        }
      }
    } else {
      if (c.isOffline()) {
        OfflineCause oc = c.getOfflineCause();
        if (oc instanceof MaintenanceOfflineCause) {
          MaintenanceOfflineCause moc = (MaintenanceOfflineCause) oc;
          if (!moc.isTakeOnline()) {
            LOGGER.log(Level.INFO, "Computer should not be taken online automatically: {0}", c.getName());
            return 5;
          }
        }
      }
      return regularRetentionStrategy.check(c);
    }
    return 1;
  }

  /** Descriptor for UI only. */
  @Extension
  @Symbol("agent-maintenance")
  public static class DescriptorImpl extends Descriptor<RetentionStrategy<?>> {
    @Override
    public String getDisplayName() {
      return Messages.AgentMaintenanceRetentionStrategy_displayName();
    }

    /**
     * For UI only.
     *
     * @param it the agent instance
     * @return List of descriptors
     */
    public final List<Descriptor<RetentionStrategy<?>>> retentionStrategyDescriptors(@CheckForNull Slave it) {
      List<Descriptor<RetentionStrategy<?>>> descriptors = it == null
          ? DescriptorVisibilityFilter.applyType(Slave.class, RetentionStrategy.all())
          : DescriptorVisibilityFilter.apply(it, RetentionStrategy.all());
      descriptors.remove(this);
      return descriptors;
    }
  }
}
