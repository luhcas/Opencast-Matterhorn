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
package org.opencastproject.example.endpoint;

import org.opencastproject.example.api.ExampleEntity;
import org.opencastproject.example.api.ExampleService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

/**
 * @see ExampleWebService
 */
@WebService()
public class ExampleWebServiceImpl implements ExampleWebService {
  private static final Logger logger = LoggerFactory.getLogger(ExampleWebServiceImpl.class);
  
  private ExampleService service;
  public void setService(ExampleService service) {
    this.service = service;
  }
  
  @WebMethod()
  @WebResult(name="example-entity")
  public ExampleEntityJaxbImpl getExampleEntity(@WebParam(name="id") String id) {
    ExampleEntity entity = service.getExampleEntity(id);
    return new ExampleEntityJaxbImpl(entity);
  }

  @WebMethod()
  public void storeExampleEntity(@WebParam(name="example-entity") ExampleEntityJaxbImpl jaxbEntity) {
    logger.info("Storing " + jaxbEntity);
    service.saveExampleEntity(jaxbEntity.getEntity());
  }
}
