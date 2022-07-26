package com.sap.prd.jenkins.plugins.agent_maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.sap.prd.jenkins.plugins.agent_maintenance.MaintenanceWindow.DescriptorImpl;
import hudson.util.FormValidation;
import org.junit.Test;

/** Tests for the maintenance window. */
public class MaintenanceWindowTest {

  @Test
  public void testDoCheckStartTime() {
    DescriptorImpl d = new DescriptorImpl();
    assertThat(d.doCheckStartTime("2022-01-01 12:00", "").kind, is(FormValidation.Kind.ERROR));
    assertThat(d.doCheckStartTime("", "2022-01-01 12:00").kind, is(FormValidation.Kind.ERROR));
    assertThat(d.doCheckStartTime("2022-01-01 12:00", "2022-01-01 10:00").kind, is(FormValidation.Kind.WARNING));
    assertThat(d.doCheckStartTime("2022-01-01 12:00", "2022-01-02 12:00").kind, is(FormValidation.Kind.OK));
  }

  @Test
  public void parseWaitingTime() {
    MaintenanceWindow m1 = new MaintenanceWindow("2022-01-01 12:00", "2022-01-02 12:00", "test", false, false, "10", "user", "id");
    assertThat(m1.getMaxWaitMinutes(), is(10));
    m1 = new MaintenanceWindow("2022-01-01 12:00", "2022-01-02 12:00", "test", false, false, "10m", "user", "id");
    assertThat(m1.getMaxWaitMinutes(), is(10));
    m1 = new MaintenanceWindow("2022-01-01 12:00", "2022-01-02 12:00", "test", false, false, "1h", "user", "id");
    assertThat(m1.getMaxWaitMinutes(), is(60));
    m1 = new MaintenanceWindow("2022-01-01 12:00", "2022-01-02 12:00", "test", false, false, "2h 10m", "user", "id");
    assertThat(m1.getMaxWaitMinutes(), is(130));
    m1 = new MaintenanceWindow("2022-01-01 12:00", "2022-01-02 12:00", "test", false, false, "1d 1h 30m", "user", "id");
    assertThat(m1.getMaxWaitMinutes(), is(1530));
  }

  @Test
  public void testEquals() {
    MaintenanceWindow m1 = new MaintenanceWindow("2022-01-01 12:00", "2022-01-02 12:00", "test", false, false, "0", "user", "id");
    MaintenanceWindow m2 = new MaintenanceWindow("2022-01-01 12:00", "2022-01-02 12:00", "test", false, false, "0", "user", "id");
    MaintenanceWindow m3 = new MaintenanceWindow("2022-01-01 12:00", "2022-01-02 12:00", "test", false, false, "0", "user2", "id");
    MaintenanceWindow m4 = new MaintenanceWindow("2022-01-01 12:00", "2022-01-02 12:00", "test", false, false, "0", "user", "id2");
    MaintenanceWindow m5 = new MaintenanceWindow("2022-01-01 12:00", "2022-01-02 11:00", "test", false, false, "350", "user", "id");
    MaintenanceWindow m6 = new MaintenanceWindow("2022-01-01 11:00", "2022-01-02 12:00", "test", false, false, "0", "user", "id");
    MaintenanceWindow m7 = new MaintenanceWindow("2022-01-01 12:00", "2022-01-02 12:00", "test", true, false, "0", "user", "id");
    MaintenanceWindow m8 = new MaintenanceWindow("2022-01-01 12:00", "2022-01-02 12:00", "test", false, true, "0", "user", "id");
    MaintenanceWindow m9 = new MaintenanceWindow("2022-01-01 12:00", "2022-01-02 12:00", "test2", false, false, "0", "user", "id");

    assertThat(m1.equals(m2), is(true));
    assertThat(m1.equals(m3), is(true));
    assertThat(m1.equals(m4), is(true));
    assertThat(m1.equals(m5), is(false));
    assertThat(m1.equals(m6), is(false));
    assertThat(m1.equals(m7), is(false));
    assertThat(m1.equals(m8), is(false));
    assertThat(m1.equals(m9), is(false));
  }
}
