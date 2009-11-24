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
package org.opencastproject.capture.admin.endpoint;

import javax.jws.WebService;

import org.opencastproject.capture.admin.api.CaptureAgentStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO:  Write me!  Comment me!
 * @see Capture-adminWebService
 */
@WebService()
public class CaptureAgentStatusWebServiceImpl implements CaptureAgentStatusWebService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentStatusWebServiceImpl.class);
  
  private CaptureAgentStatusService service;

  public void setService(CaptureAgentStatusService service) {
    this.service = service;
  }

  public void unsetService(CaptureAgentStatusService service) {
    this.service = null;
  }
}
