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
package org.opencastproject.inspection.impl;

import org.opencastproject.inspection.api.MediaInspectionService;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Dictionary;

public class MediaInspectionServiceImpl implements MediaInspectionService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceImpl.class);
  
  public void inspect(URL url) {
    logger.info("inspect(" + url + ") called");
    // TODO Implement me
  }

  public void updated(Dictionary properties) throws ConfigurationException {
    // TODO Update the local path to the mediainfo binary
  }

}
