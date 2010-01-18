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
package org.opencastproject.captions.endpoint;

import org.opencastproject.captions.api.CaptionsMediaItem;
import org.opencastproject.captions.api.CaptionsService;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

/**
 * @see CaptionsWebService
 */
@WebService()
public class CaptionsWebServiceImpl implements CaptionsWebService {
  
  private CaptionsService service;
  public void setService(CaptionsService service) {
    this.service = service;
  }

  public void unsetService(CaptionsService service) {
    this.service = null;
  }
  
  @WebMethod()
  @WebResult(name="captionsHandler-entity")
  public CaptionsEntityJaxb getCaptionshandlerEntity(@WebParam(name="id") String id) {
    CaptionsMediaItem entity = service.getCaptionsMediaItem(id);
    return new CaptionsEntityJaxb(entity);
  }
}
