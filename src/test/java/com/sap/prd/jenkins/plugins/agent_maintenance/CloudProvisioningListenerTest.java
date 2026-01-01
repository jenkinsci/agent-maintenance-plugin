package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.model.queue.CauseOfBlockage;
import hudson.slaves.Cloud;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class CloudProvisioningListenerTest extends BaseIntegrationTest {
  @Test
  void testCanProvision_blocksCloudInMaintenance() throws IOException {
    Cloud testCloud = new Cloud("test-cloud") {};

    LocalDateTime now = LocalDateTime.now();
    MaintenanceWindow window = new MaintenanceWindow(
            now.format(MaintenanceWindow.DATE_INPUT_FORMATTER),
            now.plusHours(2).format(MaintenanceWindow.DATE_INPUT_FORMATTER),
            "Test maintenance"
    );
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, testCloud.name);
    maintenanceHelper.addMaintenanceWindow(target.toKey(), window);

    CloudMaintenanceProvisioningListener listener = new CloudMaintenanceProvisioningListener();
    CauseOfBlockage blockage = listener.canProvision(testCloud, (Cloud.CloudState) null, 1);

    assertNotNull(blockage, "Cloud in maintenance should be blocked");
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
    MaintenanceWindow pastWindow = new MaintenanceWindow(
            now.minusHours(2).format(MaintenanceWindow.DATE_INPUT_FORMATTER),
            now.minusHours(1).format(MaintenanceWindow.DATE_INPUT_FORMATTER),
            "Past maintenance"
    );
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, mockCloud.name);
    maintenanceHelper.addMaintenanceWindow(target.toKey(), pastWindow);

    assertFalse(pastWindow.isMaintenanceScheduled(), "Past window should not be scheduled");
    assertFalse(maintenanceHelper.hasActiveMaintenanceWindows(target.toKey()), "Should have no active maintenance windows");

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

  @Test
  void testCanProvision_allowsAfterMaintenanceDeleted() throws Exception {
    Cloud testCloud = new Cloud("test-cloud") {};

    LocalDateTime now = LocalDateTime.now();
    MaintenanceWindow window = new MaintenanceWindow(
            now.minusMinutes(10).format(MaintenanceWindow.DATE_INPUT_FORMATTER),
            now.plusHours(2).format(MaintenanceWindow.DATE_INPUT_FORMATTER),
            "Active maintenance"
    );
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, testCloud.name);
    maintenanceHelper.addMaintenanceWindow(target.toKey(), window);

    CloudMaintenanceProvisioningListener listener = new CloudMaintenanceProvisioningListener();
    CauseOfBlockage blockageBefore = listener.canProvision(testCloud, (Cloud.CloudState) null, 1);

    assertNotNull(blockageBefore, "Cloud should be blocked before deletion");
    assertTrue(maintenanceHelper.hasActiveMaintenanceWindows(target.toKey()),
            "Should have active maintenance window before deletion");

    maintenanceHelper.deleteMaintenanceWindow(target.toKey(), window.getId());
    CauseOfBlockage blockageAfter = listener.canProvision(testCloud, (Cloud.CloudState) null, 1);

    assertNull(blockageAfter, "Cloud should be allowed after maintenance deleted");
    assertFalse(maintenanceHelper.hasActiveMaintenanceWindows(target.toKey()),
            "Should have no active maintenance windows after deletion");
    assertFalse(maintenanceHelper.hasMaintenanceWindows(target.toKey()),
            "Should have no maintenance windows at all after deletion");
  }
}
