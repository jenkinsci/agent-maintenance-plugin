package com.sap.prd.jenkins.plugins.agent_maintenance;

import java.util.Objects;

/**
 * Model class for a Maintenance Target i.e. Cloud or Agent.
 */
public class MaintenanceTarget {

  private final TargetType type;
  private final String name;
  private final String uuid; // Cloud names can be duplicated so they also include uuid

  public MaintenanceTarget(TargetType type, String name) {
    this(type, name, null);
  }

  /**
   * Create a Maintenance Target with UUID.
   */
  public MaintenanceTarget(TargetType type, String name, String uuid) {
    this.type = type;
    this.name = name;
    this.uuid = uuid;
  }

  public TargetType getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public String getUuid() {
    return uuid;
  }

  /**
   * This returns a target key for the <code>MaintenanceTarget</code> prefixed with the target type.
   * Example: AGENT:docker-node, CLOUD:aws-1xxx09:xxx-uuid-xxxxx
   *
   * @return Target key.
   */
  public String toKey() {
    if (uuid != null) {
      return type.name() + ":" + name + ":" + uuid;
    }
    return type.name() + ":" + name;
  }

  /**
   * Static factory method for safely parsing target keys to <code>MaintenanceTarget</code> instance.
   *
   * @param key Key of a target.
   * @return <code>MaintenanceTarget</code> of corresponding target key.
   */
  public static MaintenanceTarget fromKey(String key) {
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("Invalid maintenance target key: " + key);
    }

    // Backwards compatible - Since target keys are written in XML now instead of plain names,
    // when reading the XML file, the plain target names are assumed to be AGENT
    if (!key.contains(":")) {
      return new MaintenanceTarget(TargetType.AGENT, key);
    }

    int indexFirst = key.indexOf(':');
    String typePart = key.substring(0, indexFirst);
    TargetType type;
    try {
      type = TargetType.valueOf(typePart.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown maintenance target type: " + typePart, e);
    }

    String remainder = key.substring(indexFirst + 1);
    int indexLast = key.lastIndexOf(':');
    boolean isDuplicate = indexLast > indexFirst;

    if (typePart.equals(TargetType.CLOUD.name()) && isDuplicate) {
      String namePart = key.substring(indexFirst + 1, indexLast);
      String uuidPart = key.substring(indexLast + 1);
      if (uuidPart.isEmpty()) {
        throw new IllegalArgumentException("Invalid CLOUD maintenance target key (for duplicate): " + key + " (missing UUID after ':'");
      }

      return new MaintenanceTarget(type, namePart, uuidPart);
    }

    return new MaintenanceTarget(type, remainder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MaintenanceTarget that = (MaintenanceTarget) o;
    return Objects.equals(type, that.type)
        && Objects.equals(name, that.name)
        && Objects.equals(uuid, that.uuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, name, uuid);
  }

  /**
   * Enum containing types of target for maintenance.
   */
  public enum TargetType {
    AGENT, CLOUD
  }
}
