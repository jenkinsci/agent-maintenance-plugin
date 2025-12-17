package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.Extension;
import hudson.slaves.CloudProvisioningListener;

/**
 * Cloud provisioning listener placeholder for maintenance window support.
 * Initial implementation only registers the extension.
 * Behavior will be added incrementally.
 */
@Extension
public class MaintenanceCloudProvisioningListener extends CloudProvisioningListener {
    // Intentionally empty (first-step contribution)
}
