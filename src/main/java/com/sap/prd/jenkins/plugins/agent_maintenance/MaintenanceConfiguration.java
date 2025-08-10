package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.Extension;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.slaves.AbstractCloudSlave;
import hudson.util.HttpResponses;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.verb.POST;

/** The global configuration of the plugin. */
@Extension
@Symbol("agent-maintenance")
public class MaintenanceConfiguration extends GlobalConfiguration {
  private boolean injectRetentionStrategy;

  @DataBoundConstructor
  public MaintenanceConfiguration() {
    load();
  }

  public void setInjectRetentionStrategy(boolean injectRetentionStrategy) {
    this.injectRetentionStrategy = injectRetentionStrategy;
    save();
  }

  public boolean isInjectRetentionStrategy() {
    return injectRetentionStrategy;
  }

  public static MaintenanceConfiguration getInstance() {
    return GlobalConfiguration.all().get(MaintenanceConfiguration.class);
  }

  /**
   * Called when UI button to inject strategy to all agents is pressed.
   *
   * @param rsp Stapler Response
   * @return A HttpResponse
   */
  @POST
  public HttpResponse doInject(StaplerResponse2 rsp) {
    Jenkins.get().checkPermission(Jenkins.ADMINISTER);
    int counter = 0;
    for (Node node : Jenkins.get().getNodes()) {
      if (node instanceof Slave && !(node instanceof AbstractCloudSlave)) {
        if (MaintenanceHelper.getInstance().injectRetentionStrategy(node.toComputer())) {
          counter++;
        }
      }
    }
    String message = "<div>Injected maintenance strategy to " + counter + " agents</div>";
    return HttpResponses.literalHtml(message);
  }

  /**
   * Called when UI button to remove strategy from all agents is pressed.
   *
   * @param rsp Stapler Response
   * @return A HttpResponse
   */
  @POST
  public HttpResponse doRemove(StaplerResponse2 rsp) {
    Jenkins.get().checkPermission(Jenkins.ADMINISTER);
    int counter = 0;
    for (Node node : Jenkins.get().getNodes()) {
      if (node instanceof Slave && !(node instanceof AbstractCloudSlave)) {
        if (MaintenanceHelper.getInstance().removeRetentionStrategy(node.toComputer())) {
          counter++;
        }
      }
    }
    String message = "<div>Removed maintenance strategy from " + counter + " agents</div>";
    return HttpResponses.literalHtml(message);
  }
}
