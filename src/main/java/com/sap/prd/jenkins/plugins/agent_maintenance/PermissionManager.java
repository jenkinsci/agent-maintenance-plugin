package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.model.Computer;
import hudson.security.AccessDeniedException3;
import hudson.security.Permission;
import java.util.Arrays;
import jenkins.model.Jenkins;

/**
 * Centralized permission manager for maintenance targets.
 *
 * <p>
 * Permission rules:
 * <ul>
 *   <li>AGENT view: Computer.EXTENDED_READ | Computer.CONFIGURE | Computer.DISCONNECT</li>
 *   <li>AGENT modify/delete: Computer.CONFIGURE | Computer.DISCONNECT</li>
 *   <li>CLOUD view: Jenkins.SYSTEM_READ | Jenkins.ADMINISTER</li>
 *   <li>CLOUD modify/delete: Jenkins.ADMINISTER only</li>
 * </ul>
 */
public final class PermissionManager {

  private PermissionManager() {}

  /**
   * Returns true if the current user can VIEW maintenance windows for this target.
   */
  public static boolean canView(MaintenanceTarget target) {
    return switch (target.getType()) {
      case AGENT -> {
        Computer c = getComputer(target);
        yield c != null
            && (c.hasPermission(Computer.EXTENDED_READ)
                || c.hasPermission(Computer.CONFIGURE)
                || c.hasPermission(Computer.DISCONNECT));
      }
      case CLOUD -> Jenkins.get().hasPermission(Jenkins.SYSTEM_READ);
    };
  }

  /**
   * Returns true if the current user can ADD or EDIT maintenance windows for this target.
   */
  public static boolean canModify(MaintenanceTarget target) {
    return switch (target.getType()) {
      case AGENT -> {
        Computer c = getComputer(target);
        yield c != null
            && (c.hasPermission(Computer.CONFIGURE)
                || c.hasPermission(Computer.DISCONNECT));
      }
      case CLOUD -> Jenkins.get().hasPermission(Jenkins.ADMINISTER);
    };
  }

  /**
   * Returns true if the current user can DELETE maintenance windows for this target.
   */
  public static boolean canDelete(MaintenanceTarget target) {
    return canModify(target); // same threshold for now
  }

  /**
   * Returns true if the current user has the given permissions for this target.
   *
   * @param target the target to check permissions for
   * @param checkAll if true, all permissions must be granted; otherwise any permission is sufficient
   * @param permissions the permissions to check for
   *
   * @return true if the user has the given permissions
   */
  public static boolean hasPermissions(MaintenanceTarget target, boolean checkAll, Permission... permissions) {
    return switch (target.getType()) {
      case AGENT -> {
        Computer c = getComputer(target);
        yield c != null
            && (checkAll ? Arrays.stream(permissions).allMatch(c::hasPermission)
            : Arrays.stream(permissions).anyMatch(c::hasPermission));
      }
      case CLOUD -> checkAll ? Arrays.stream(permissions).allMatch(Jenkins.get()::hasPermission)
          : Arrays.stream(permissions).anyMatch(Jenkins.get()::hasPermission);
    };
  }

  /**
   * Throws AccessDeniedException if the user cannot VIEW.
   */
  public static void checkCanView(MaintenanceTarget target) {
    if (!canView(target)) {
      throwDenied(target.getType() == MaintenanceTarget.TargetType.CLOUD
          ? Jenkins.SYSTEM_READ
          : Computer.EXTENDED_READ);
    }
  }

  /**
   * Throws AccessDeniedException if the user cannot MODIFY.
   */
  public static void checkCanModify(MaintenanceTarget target) {
    if (!canModify(target)) {
      throwDenied(target.getType() == MaintenanceTarget.TargetType.CLOUD
          ? Jenkins.ADMINISTER
          : Computer.CONFIGURE);
    }
  }

  /**
   * Throws AccessDeniedException if the user cannot DELETE.
   */
  public static void checkCanDelete(MaintenanceTarget target) {
    if (!canDelete(target)) {
      throwDenied(target.getType() == MaintenanceTarget.TargetType.CLOUD
          ? Jenkins.ADMINISTER
          : Computer.CONFIGURE);
    }
  }

  /**
   * Throws AccessDeniedException if the user does not have the given permissions.
   *
   * @param target the target to check permissions for
   * @param checkAll if true, all permissions must be granted; otherwise any permission is sufficient
   * @param permissions the permissions to check for
   *
   * @throws AccessDeniedException3 if the user does not have the required permissions
   */
  public static void checkHasPermissions(MaintenanceTarget target, boolean checkAll, Permission... permissions) {
    if (hasPermissions(target, checkAll, permissions)) {
      return;
    }

    if (checkAll) {
      Permission missing = Arrays.stream(permissions)
          .filter(p -> !hasPermissions(target, true, p))
          .findFirst()
          .orElse(permissions[0]);
      throwDenied(missing);
    } else {
      throwDenied(permissions[0]);
    }
  }

  private static Computer getComputer(MaintenanceTarget target) {
    return Jenkins.get().getComputer(target.getName());
  }

  private static void throwDenied(Permission required) {
    throw new AccessDeniedException3(Jenkins.getAuthentication2(), required);
  }
}
