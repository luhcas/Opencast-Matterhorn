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
package org.opencastproject.ingest.endpoint;

//import org.opencastproject.ingest.api.IngestService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;

/**
 * @see IngestWebService
 */
@WebService()
public class IngestWebServiceImpl implements IngestWebService {
  private static final Logger logger = LoggerFactory.getLogger(IngestWebServiceImpl.class);

  // private IngestService service;

  public IngestWebServiceImpl() {
    logger.info("Ingest Web Service started");
  }

  // public void setService(IngestService service) {
  // this.service = service;
  // }

  // @WebMethod()
  // @WebResult(name="ingest-MediaPackageID")
  // public String getIngestEntity() {
  // return service.createMediaPackage();
  // }

  // @WebMethod()
  // @WebResult(name="ingest-entity")
  // public IngestEntityJaxbImpl getIngestEntity(@WebParam(name="id") String id) {
  // IngestEntity entity = service.getIngestEntity(id);
  // return new IngestEntityJaxbImpl(entity);
  // }
  //
  // @WebMethod()
  // public void storeIngestEntity(@WebParam(name="ingest-entity") IngestEntityJaxbImpl jaxbEntity) {
  // logger.info("Storing " + jaxbEntity);
  // service.saveIngestEntity(jaxbEntity.getEntity());
  // }
}
