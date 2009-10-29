/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.capture.impl;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * The class responsible for starting a capture.
 */
public class CaptureJob implements Job {

  /**
   * Starts the capture itself.
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  public void execute(JobExecutionContext arg0) throws JobExecutionException {
    //TODO:  Waiting on capture impl code to be able to actually start the capture
    /*
     * General idea:
     *  Get all required properties via
     *   ctx.getMergedJobDataMap().get($PROPERTY)
     *  Turn these properties into hashmap as defined by MH-1265
     *  Send this to capture's start method
     *   via HTTP post?  Native call?  Unclear just yet...
     */
  }

}
