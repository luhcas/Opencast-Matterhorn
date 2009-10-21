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

import org.opencastproject.capture.api.State;
import org.opencastproject.capture.api.StatusService;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

/**
 * FIXME -- Add javadocs
 */
public class StatusServiceImpl implements StatusService, ManagedService {

  private static final Logger logger = LoggerFactory.getLogger(StatusServiceImpl.class);
  private static State cur_status = null;

  public StatusServiceImpl() {
    cur_status = State.IDLE;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.status.api.StatusService#getState()
   */
  public State getState() {
    return cur_status;
  }

  public void start() {
    cur_status = State.CAPTURING;
  }

  public void stop() {
    if (cur_status == State.CAPTURING) {
      cur_status = State.UPLOADING;
    } else {
      cur_status = State.IDLE;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.status.api.StatusService#setState()
   */
  public void setState(State state) {
    cur_status = state;
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }
}
