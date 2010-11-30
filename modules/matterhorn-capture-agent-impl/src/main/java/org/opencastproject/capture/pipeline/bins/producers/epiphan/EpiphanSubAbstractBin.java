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

import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.elements.AppSink;
import org.opencastproject.capture.impl.CaptureAgentImpl;

/**
 * EpiphanAbstractBin implements ApiphanSubBin interface.
 * Create a empty Pipeline and implement state change methods.
 */
public abstract class EpiphanSubAbstractBin implements EpiphanSubBin {

  /** Pipeline */
  Pipeline pipeline;

  /**
   * Constructor. Creates a ampty pipeline.
   * @param pipelineName pipeline name.
   */
  public EpiphanSubAbstractBin(String pipelineName) {
    pipeline = new Pipeline(pipelineName);
  }

  /**
   * @inheritDocs
   * @see EpiphanSubBin#getSink() 
   */
  @Override
  public abstract AppSink getSink();

  /**
   * @inheritDocs
   * @see EpiphanSubBin#start(long)
   */
  @Override
  public boolean start(long time) {
    return setState(State.PLAYING, time);
  }

  /**
   * @inheritDocs
   * @see EpiphanSubBin#stop()
   */
  @Override
  public void stop() {
    setState(State.NULL, -1);
  }

  /**
   * @inheritDocs
   * @see EpiphanSubBin#setState(State, long)
   */
  @Override
  public boolean setState(State state, long time) {
    pipeline.setState(state);
    if (time < 0) {
      return true;
    } else {
      return pipeline.getState(time * CaptureAgentImpl.GST_SECOND) == state;
    }
  }
}
