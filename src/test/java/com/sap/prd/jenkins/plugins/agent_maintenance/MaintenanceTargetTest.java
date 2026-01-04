package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class MaintenanceTargetTest {
  @Test
  void testAgentTargetKeyGeneration() {
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.AGENT, "my-agent");

    assertEquals("AGENT:my-agent", target.toKey());
    assertEquals(MaintenanceTarget.TargetType.AGENT, target.getType());
    assertEquals("my-agent", target.getName());
  }

  @Test
  void testCloudTargetKeyGeneration() {
    MaintenanceTarget target = new MaintenanceTarget(MaintenanceTarget.TargetType.CLOUD, "my-cloud");

    assertEquals("CLOUD:my-cloud", target.toKey());
    assertEquals(MaintenanceTarget.TargetType.CLOUD, target.getType());
    assertEquals("my-cloud", target.getName());
  }

  @Test
  void testFromKey_agent() {
    MaintenanceTarget target = MaintenanceTarget.fromKey("AGENT:test-agent");

    assertNotNull(target);
    assertEquals(MaintenanceTarget.TargetType.AGENT, target.getType());
    assertEquals("test-agent", target.getName());
  }

  @Test
  void testFromKey_cloud() {
    MaintenanceTarget target = MaintenanceTarget.fromKey("CLOUD:test-cloud");

    assertNotNull(target);
    assertEquals(MaintenanceTarget.TargetType.CLOUD, target.getType());
    assertEquals("test-cloud", target.getName());
  }

  @Test
  void testFromKey_withColonInName() {
    MaintenanceTarget target = MaintenanceTarget.fromKey("CLOUD:docker:latest");

    assertNotNull(target);
    assertEquals(MaintenanceTarget.TargetType.CLOUD, target.getType());
    assertEquals("docker:latest", target.getName());
  }

  @Test
  void testFromKey_withMultipleColons() {
    MaintenanceTarget target = MaintenanceTarget.fromKey("AGENT:region:india:node:67");

    assertNotNull(target);
    assertEquals(MaintenanceTarget.TargetType.AGENT, target.getType());
    assertEquals("region:india:node:67", target.getName());
  }

  @Test
  void testFromKey_backwardsCompatible_plainName() {
    // Plain names without the TargetType prefix and colon should default to AGENT type
    MaintenanceTarget target = MaintenanceTarget.fromKey("old-agent-name");

    assertNotNull(target);
    assertEquals(MaintenanceTarget.TargetType.AGENT, target.getType());
    assertEquals("old-agent-name", target.getName());
  }
}
