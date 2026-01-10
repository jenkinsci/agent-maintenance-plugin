package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import hudson.model.Slave;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.stream.Stream;

/** Test recurring maintenance windows. */
@WithJenkins
class RecurringTest extends BaseIntegrationTest {

  static Stream<MaintenanceTarget.TargetType> allTargets() {
    return Stream.of(
            MaintenanceTarget.TargetType.AGENT,
            MaintenanceTarget.TargetType.CLOUD
    );
  }

  @ParameterizedTest(name = "recurringMaintenanceInjectsMaintenance")
  @MethodSource("allTargets")
  void recurringMaintenanceInjectsMaintenance(MaintenanceTarget.TargetType targetType) throws Exception {
    String targetName = "recurring-" + targetType.name().toLowerCase();
    MaintenanceTarget target = getTarget(targetType, targetName);
    RecurringMaintenanceWindow rw = new RecurringMaintenanceWindow("0 2 * * *",
        "test", true, true, "10m", "60m",  "test", null, 0);
    maintenanceHelper.addRecurringMaintenanceWindow(target.toKey(), rw);
    maintenanceHelper.checkRecurring(target.toKey());
    if (targetType == MaintenanceTarget.TargetType.AGENT) {
      Slave agent = getAgent(targetName);
      triggerCheckCycle(agent);
    }
    assertThat(maintenanceHelper.hasMaintenanceWindows(target.toKey()), is(true));
  }
}
