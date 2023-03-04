Agent Maintenance Plugin for Jenkins
=========================

[![Build Status](https://ci.jenkins.io/job/Plugins/job/agent-maintenance-plugin/job/main/badge/icon)](https://ci.jenkins.io/job/Plugins/job/agent-maintenance-plugin/job/main/)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/agent-maintenance)](https://plugins.jenkins.io/agent-maintenance)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/agent-maintenance-plugin.svg?label=release)](https://github.com/jenkinsci/agent-maintenance-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/agent-maintenance.svg?color=blue)](https://plugins.jenkins.io/agent-maintenance)
[![REUSE status](https://api.reuse.software/badge/github.com/jenkinsci/agent-maintenance-plugin)](https://api.reuse.software/info/github.com/jenkinsci/agent-maintenance-plugin)


This plugin allows to take agents offline for one or more dedicated time windows on specific dates (e.g. due to a hardware/network maintenance)
while allowing to configure any of the other availability strategies (e.g *Keep online as much as possible* or *Bring this agent online according to a schedule*) for the times when no maintenance window is active.

Maintenance activities are usually scheduled on weekends or outside normal business hours during the night. When you have many permanent agents this plugin helps to ensure that your builds are not unexpectedly killed because of a planned network interruption, an OS update or a reboot of the machine.


## Configuration

On the Jenkins main configuration page you can enable that for newly created (permanent) agents, the agent maintenance availability is automatically injected.
(this does not apply to cloud agents where it doesn't make sense as the agent is deleted right after it was used for a build usually. Not all Cloud implementations inherit from the corresponding classes so that they are detected).

Using the button *Inject* will inject it to all existing agents when not already configured. The currently configured availability will be set as the regular availability.
This is useful directly after installing the plugin.

The same way the button *Remove* will remove the agent maintenance availability and restore the configured default availability as the agents availability.

When creating a new agent select *Take agent offline during maintenance, otherwise use other availability*.

![Configuration](/docs/images/configure.PNG)

To enable for an existing agent open the agent overview page, select *Maintenance Windows* and then click on the *Enable* button. It is possible to also enable it via the configure page but then you need to reconfigure the existing Availability settings.

## Defining maintenance windows

##### Individually for a single agent
Maintenance windows can be defined by opening the corresponding link on the agents overview page.
Using the "Add" button you can directly define a new maintenance window.
Via the "Edit" button one can edit existing maintenance windows (and also add and delete).
To directly delete a maintenance windows click the "x" next to it or mark the checkboxes and use the "Delete selected" button to delete several maintenance windows.

##### For many agents simultaneously
Going to "Manage Jenkins->Agent Maintenance" will present you a list of all currently defined maintenance windows of all agents.
Using the button "Add" allows to use a label expression to select a list of agents for which to apply the maintenance window.
Use the "x" to directly delete a single maintenance window or use the checkboxes to mark multiple windows and delete with the "Delete selected" button.


## Best practices

When defining a maintenance window one has to consider the time it takes for any running build to finish. So if the actual maintenance starts at 8 AM and your builds usually run for 30 minutes you might set the start time to 7:15 AM and define a "Max waiting time in minutes for builds to finish" of 45 minutes.

At 7:15 the agent will stop accepting new tasks, running builds should have enough time to finish. If a build is still running when the max waiting time is reached an abort request is sent to the build. 

## Contributing

Refer to our [contribution guidelines](CONTRIBUTING.md).

## License

Copyright 2022 SAP SE or an SAP affiliate company and agent-maintenance-plugin contributors. Licensed under the [Apache License, Version 2.0](LICENSE). Detailed information including third-party components and their licensing/copyright information is available [via the REUSE tool](https://api.reuse.software/info/github.com/jenkinsci/agent-maintenance-plugin).
