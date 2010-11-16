/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.workflow.handler;

import org.opencastproject.workflow.api.AbstractResumableWorkflowOperationHandler;

import org.osgi.service.component.ComponentContext;

/**
 * Workflow operation handler that signifies a workflow that is currently scheduled and is waiting for the capture
 * process to happen.
 * <p>
 * The operation registers a ui that displays information on the capture status, the recording device as well as other
 * related information.
 */
public class ScheduleWorkflowOperationHandler extends AbstractResumableWorkflowOperationHandler {

  /** Configuration key for the start date and time */
  public static final String OPT_SCHEDULE_START = "schedule.start";

  /** Configuration key for the end date and time */
  public static final String OPT_SCHEDULE_END = "schedule.end";

  /** Configuration key for the schedule location */
  public static final String OPT_SCHEDULE_LOCATION = "schedule.location";


  /** Path to the hold state ui  */
  public static final String UI_RESOURCE_PATH = "/ui/operation/schedule/index.html";

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.AbstractResumableWorkflowOperationHandler#activate(org.osgi.service.component.ComponentContext)
   */
  @Override
  public void activate(ComponentContext componentContext) {
    super.activate(componentContext);

    // Set the operation's action link title
    setHoldActionTitle("View schedule");
    
    // Register the supported configuration options
    addConfigurationOption(OPT_SCHEDULE_START, "Schedule start date");
    addConfigurationOption(OPT_SCHEDULE_END, "Schedule end date");
    addConfigurationOption(OPT_SCHEDULE_LOCATION, "Recording location");

    // Add the ui piece that displays the schedule information
    registerHoldStateUserInterface(UI_RESOURCE_PATH);  
  }

}
