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
package org.opencastproject.capture.pipeline.bins.producers.epiphan;

import org.gstreamer.State;
import org.gstreamer.elements.AppSink;

/**
 * EpiphanSubBin Interface.
 */
interface EpiphanSubBin {

  /**
   * Returns AppSink Element to get buffer from.
   * @return
   */
  public AppSink getSink();

  /**
   * Start pipeline.
   * @param time time to check, if pipeline is playing, -1 skip checks.
   * @return true, if pipeline is playing.
   */
  public boolean start(long time);

  /**
   * Stop pipeline.
   */
  public void stop();

  /**
   * Set pipeline to specified State.
   * @param state state to set.
   * @param time time to check state, -1 skip checks.
   * @return true, if pipeline is in specified state.
   */
  public boolean setState(State state, long time);
}
