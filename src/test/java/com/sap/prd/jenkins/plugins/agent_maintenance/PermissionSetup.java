package com.sap.prd.jenkins.plugins.agent_maintenance;

import hudson.model.Computer;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.matrixauth.AuthorizationType;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.junit.Before;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

/** Base class for permission checks. */
public abstract class PermissionSetup {

  @Rule public JenkinsRule rule = new JenkinsRule();

  protected static final String READER = "reader";
  protected static final String CONFIGURE = "configure";
  protected static final String DISCONNECT = "disconnect";
  protected static final String USER = "user";
  protected static final String MANAGE = "manage";
  protected static final String ADMIN = "admin";

  protected static final PermissionEntry configure = new PermissionEntry(AuthorizationType.USER, CONFIGURE);
  protected static final PermissionEntry disconnect = new PermissionEntry(AuthorizationType.USER, DISCONNECT);

  /**
   * Setup tests.
   *
   * @throws Exception when something goes wrong
   */
  @Before
  public void setupPermissions() throws Exception {
    HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(false, false, null);
    rule.jenkins.setSecurityRealm(realm);
    realm.createAccount(READER, READER);
    realm.createAccount(MANAGE, MANAGE);
    realm.createAccount(USER, USER);
    realm.createAccount(CONFIGURE, CONFIGURE);
    realm.createAccount(DISCONNECT, DISCONNECT);
    realm.createAccount(ADMIN, ADMIN);
    ProjectMatrixAuthorizationStrategy matrixAuth = new ProjectMatrixAuthorizationStrategy();

    // System read and computer configure on agent, but not on agentRestricted
    matrixAuth.add(Jenkins.READ, configure);
    matrixAuth.add(Jenkins.SYSTEM_READ, configure);

    // System read and computer configure on agent, but not on agentRestricted
    matrixAuth.add(Jenkins.READ, disconnect);
    matrixAuth.add(Jenkins.SYSTEM_READ, disconnect);

    // system manage
    PermissionEntry manage = new PermissionEntry(AuthorizationType.USER, MANAGE);
    matrixAuth.add(Jenkins.READ, manage);
    matrixAuth.add(Jenkins.MANAGE, manage);
    
    // Administrator
    PermissionEntry admin = new PermissionEntry(AuthorizationType.USER, ADMIN);
    matrixAuth.add(Jenkins.ADMINISTER, admin);

    // system read
    PermissionEntry reader = new PermissionEntry(AuthorizationType.USER, READER);
    matrixAuth.add(Jenkins.READ, reader);
    matrixAuth.add(Jenkins.SYSTEM_READ, reader);
    matrixAuth.add(Computer.EXTENDED_READ, reader);

    // normal user
    PermissionEntry user = new PermissionEntry(AuthorizationType.USER, USER);
    matrixAuth.add(Jenkins.READ, user);

    rule.jenkins.setAuthorizationStrategy(matrixAuth);
    Jenkins.MANAGE.setEnabled(true);
    Jenkins.SYSTEM_READ.setEnabled(true);
    Computer.EXTENDED_READ.setEnabled(true);
  }
}
