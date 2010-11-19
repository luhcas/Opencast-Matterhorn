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
package org.opencastproject.capture.pipeline.bins;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GStreamerElementFactory {
  private static GStreamerElementFactory factory;
  protected static final Logger logger = LoggerFactory.getLogger(GStreamerElementFactory.class);
  
  /** Singleton factory **/
  public static synchronized GStreamerElementFactory getInstance(){
    if(factory == null){
      factory = new GStreamerElementFactory();
    }
    return factory;
  }
  
  private GStreamerElementFactory(){
    
  }
  
  public Element createElement(String captureDeviceName, String elementType, String elementFriendlyName)
          throws UnableToCreateElementException {
    try {
      ElementFactory codecFactory = ElementFactory.find(elementType);
      return codecFactory.create(elementFriendlyName);
    } catch (IllegalArgumentException e) {
      throw new UnableToCreateElementException(captureDeviceName, elementType);
    }
  }
}
