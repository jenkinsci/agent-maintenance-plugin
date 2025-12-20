package com.sap.prd.jenkins.plugins.agent_maintenance;

public class MaintenanceTarget {

    private final TargetType type;
    private String name;

    public MaintenanceTarget(TargetType type, String name) {
        this.type = type;
        this.name = name;
    }

    public TargetType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String toKey() {
        return type.name() + ":" + name;
    }

    /**
     * Static factory method for safely parsing target keys to <code>MaintenanceTarget</code> instance
     *
     * @param key
     * @return <code>MaintenanceTarget</code> of corresponding target key
     */
    public static MaintenanceTarget fromKey(String key) {
        int index = key.indexOf(':');
        if (index <= 0 || index == key.length() - 1) {
            throw new IllegalArgumentException("Invalid maintenance target key: " + key);
        }

        String typePart = key.substring(0, index);
        String namePart = key.substring(index + 1);

        try {
            TargetType type = TargetType.valueOf(typePart.toUpperCase());
            return new MaintenanceTarget(type, namePart);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown maintenance target type: " + typePart, e);
        }
    }

    public enum TargetType {
        AGENT, CLOUD
    }
}
