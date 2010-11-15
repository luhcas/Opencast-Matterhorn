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
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;


public class V4L2SrcBin extends VideoSrcBin{

  Element v4l2src;

  public V4L2SrcBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }
  
  @Override
  protected void createElements(){
    super.createElements();
    v4l2src = ElementFactory.make("v4l2src", null);
  }
  
  @Override
  protected void addElementsToBin() {
    bin.addMany(v4l2src, queue, videorate, fpsfilter);
  }

  @Override
  protected void linkElements() throws Exception {
    /** TODO - akm220 - Check to see if this queue negatively effects performance. **/
    /*
     * if (!v4l2src.link(queue)) throw new Exception(CaptureDeviceBin.formatBinError(captureDevice, v4l2src, queue));
     * else if (!queue.link(videorate)) throw new Exception(CaptureDeviceBin.formatBinError(captureDevice, queue,
     * videorate)); else if (!videorate.link(fpsfilter)) throw new
     * Exception(CaptureDeviceBin.formatBinError(captureDevice, videorate, fpsfilter));
     */
    if (!v4l2src.link(videorate))
      throw new UnableToLinkGStreamerElementsException(captureDevice, v4l2src, videorate);
    else if (!videorate.link(fpsfilter))
      throw new UnableToLinkGStreamerElementsException(captureDevice, videorate, fpsfilter);
  }

  @Override
  public Pad getSrcPad() {
    return fpsfilter.getStaticPad("src");
  } 
}
