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
package org.opencastproject.capture.endpoint;

import org.opencastproject.capture.api.State;
import org.opencastproject.capture.api.StatusService;
import org.opencastproject.capture.impl.StatusServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebMethod;
import javax.jws.WebService;

/**
 * @see StatusWebService
 */
@WebService()
public class StatusWebServiceImpl implements StatusWebService {
  private static final Logger logger = LoggerFactory.getLogger(StatusWebServiceImpl.class);

  private StatusService service;

  public StatusWebServiceImpl() {
    service = new StatusServiceImpl();
  }

  public void setService(StatusService service) {
    this.service = service;
  }

  public void unsetService(StatusService service) {
    this.service = null;
  }

  @WebMethod()
  public State getStatus() {
    return service.getState();
  }
}
