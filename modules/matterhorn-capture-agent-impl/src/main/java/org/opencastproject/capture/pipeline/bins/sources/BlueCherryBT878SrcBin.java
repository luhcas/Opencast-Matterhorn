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
package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

public class BlueCherryBT878SrcBin extends V4L2SrcBin{
  Element v4l2src;
  /**
   * Adds a pipeline specifically designed to captured from the Bluecherry Provideo cards to the main pipeline
   * 
   * @param captureDevice
   *          The Bluecherry {@code CaptureDevice} to create pipeline around
   * @param properties
   *          The {@code Properties} of the confidence monitoring.
   * @return True, if successful
   * @throws UnableToSetElementPropertyBecauseElementWasNullException 
   * @throws UnableToCreateGhostPadsForBinException 
   * @throws UnableToLinkGStreamerElementsException 
   * @throws CaptureDeviceNullPointerException 
   * @throws UnableToCreateElementException 
   * @throws Exception - When something in the bin doesn't link together correctly.
   */
  public BlueCherryBT878SrcBin(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  /** Create the v4l2src Element 
   * @throws UnableToCreateElementException **/
  @Override
  protected void createElements() throws UnableToCreateElementException{
    super.createElements();
    v4l2src = ElementFactory.make(GStreamerElements.V4L2SRC, null);
  }
  
  /** Add the v4l2src, queue, videorate corrector and fpsfilter to the source bin. **/
  @Override
  protected void addElementsToBin() {
    bin.addMany(v4l2src, queue, videorate, fpsfilter);
  }

  /** Link together all of the {@code Element}s in this bin so that we can use 
   * them as a source.
   * @throws Exception - When something in the bin doesn't link together correctly we throw an exception. **/ 
  @Override
  protected void linkElements() throws UnableToLinkGStreamerElementsException {
    if (!v4l2src.link(queue)){
      throw new UnableToLinkGStreamerElementsException(captureDevice, v4l2src, queue);
    }
    else if (!queue.link(videorate)){
      throw new UnableToLinkGStreamerElementsException(captureDevice, queue, videorate);
    }
    else if (!videorate.link(fpsfilter)){
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
    }
  }

  /** Returns the fpsfilter element as the sink for this source bin since it is the last part of this source. **/ 
  @Override
  public Pad getSrcPad() {
    return fpsfilter.getStaticPad(GStreamerProperties.SRC);
  }
}
