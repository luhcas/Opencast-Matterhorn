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
package org.opencastproject.captionsHandler.endpoint;

import org.opencastproject.captionsHandler.api.CaptionshandlerEntity;
import org.opencastproject.captionsHandler.api.CaptionshandlerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

/**
 * @see CaptionshandlerWebService
 */
@WebService()
public class CaptionshandlerWebServiceImpl implements CaptionshandlerWebService {
  private static final Logger logger = LoggerFactory.getLogger(CaptionshandlerWebServiceImpl.class);
  
  private CaptionshandlerService service;
  public void setService(CaptionshandlerService service) {
    this.service = service;
  }

  public void unsetService(CaptionshandlerService service) {
    this.service = null;
  }
  
  @WebMethod()
  @WebResult(name="captionsHandler-entity")
  public CaptionshandlerEntityJaxbImpl getCaptionshandlerEntity(@WebParam(name="id") String id) {
    CaptionshandlerEntity entity = service.getCaptionshandlerEntity(id);
    return new CaptionshandlerEntityJaxbImpl(entity);
  }

  @WebMethod()
  public void storeCaptionshandlerEntity(@WebParam(name="captionsHandler-entity") CaptionshandlerEntityJaxbImpl jaxbEntity) {
    logger.info("Storing " + jaxbEntity);
    service.saveCaptionshandlerEntity(jaxbEntity.getEntity());
  }
}
