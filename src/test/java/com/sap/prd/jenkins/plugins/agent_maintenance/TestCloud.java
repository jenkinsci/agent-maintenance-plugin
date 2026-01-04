package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 * Test cloud implementation for integration tests.
 */
public class TestCloud extends Cloud implements Serializable {

  @DataBoundConstructor
  public TestCloud(String name) {
    super(name);
    Jenkins.get().clouds.add(this);
  }

  @Override
  public Collection<PlannedNode> provision(Label label, int excessWorkload) {
    return Collections.emptyList();
  }

  @Override
  public boolean canProvision(Label label) {
    return true;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<Cloud> {
    @Override
    public String getDisplayName() {
      return "Test Cloud";
    }
  }
}
