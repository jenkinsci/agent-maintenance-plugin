package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import hudson.model.queue.CauseOfBlockage;
import hudson.slaves.Cloud;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.time.LocalDateTime;

public class CloudProvisioningListenerTest extends BaseIntegrationTest {
  @Test
  void testCanProvision_blocksCloudInMaintenance() throws IOException {
    Cloud mockCloud = mock(Cloud.class, CALLS_REAL_METHODS);
    mockCloud.name = "test-cloud";

    LocalDateTime now = LocalDateTime.now();
    MaintenanceWindow window = new MaintenanceWindow(
            now.format(MaintenanceWindow.DATE_INPUT_FORMATTER),
            now.plusHours(2).format(MaintenanceWindow.DATE_INPUT_FORMATTER),
            "Test maintenance"
    );
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, mockCloud.name);
    maintenanceHelper.addMaintenanceWindow(target.toKey(), window);

    CloudMaintenanceProvisioningListener listener = new CloudMaintenanceProvisioningListener();
    CauseOfBlockage blockage = listener.canProvision(mockCloud, (Cloud.CloudState) null, 1);

    assertNotNull(blockage, "Cloud in maintenance should be blocked");
    String description = blockage.getShortDescription();
    assertTrue(description.contains("test-cloud"), "Blockage should mention cloud name");
    assertTrue(description.contains("maintenance"), "Blockage should mention maintenance");
  }

  @Test
  void testCanProvision_allowsCloudNotInMaintenance() {
    Cloud mockCloud = new Cloud("test-cloud") {};

    CloudMaintenanceProvisioningListener listener = new CloudMaintenanceProvisioningListener();
    CauseOfBlockage blockage = listener.canProvision(mockCloud, (Cloud.CloudState) null, 1);

    assertNull(blockage, "Cloud without maintenance should not be blocked");
  }

  @Test
  void testCanProvision_allowsAfterMaintenanceEnds() throws IOException {
    Cloud mockCloud = new Cloud("test-cloud") {};

    LocalDateTime now = LocalDateTime.now();
    MaintenanceWindow window = new MaintenanceWindow(
            now.minusHours(2).format(MaintenanceWindow.DATE_INPUT_FORMATTER),
            now.minusHours(1).format(MaintenanceWindow.DATE_INPUT_FORMATTER),
            "Past maintenance"
    );
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, mockCloud.name);
    maintenanceHelper.addMaintenanceWindow(target.toKey(), window);

    CloudMaintenanceProvisioningListener listener = new CloudMaintenanceProvisioningListener();
    CauseOfBlockage blockage = listener.canProvision(mockCloud, (Cloud.CloudState) null, 1);

    assertNull(blockage, "Cloud after maintenance should not be blocked");
  }

  @Test
  void testCanProvision_onlyBlocksSpecificCloud() throws IOException {
    // Two clouds - only one in maintenance
    Cloud cloudA = new Cloud("cloud-a") {};
    Cloud cloudB = new Cloud("cloud-b") {};

    LocalDateTime now = LocalDateTime.now();
    MaintenanceWindow window = new MaintenanceWindow(
            now.format(MaintenanceWindow.DATE_INPUT_FORMATTER),
            now.plusHours(2).format(MaintenanceWindow.DATE_INPUT_FORMATTER),
            "Cloud A maintenance"
    );
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, cloudA.name);
    maintenanceHelper.addMaintenanceWindow(target.toKey(), window);

    CloudMaintenanceProvisioningListener listener = new CloudMaintenanceProvisioningListener();

    CauseOfBlockage blockageA = listener.canProvision(cloudA, (Cloud.CloudState) null, 1);
    CauseOfBlockage blockageB = listener.canProvision(cloudB, (Cloud.CloudState) null, 1);

    assertNotNull(blockageA, "cloud-a should be blocked");
    assertNull(blockageB, "cloud-b should NOT be blocked");
  }
}
