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
package org.opencastproject.distribution.local.endpoint;

import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

/**
 * @see DistributionWebService
 */
@WebService()
public class DistributionWebServiceImpl implements DistributionWebService {
  private static final Logger logger = LoggerFactory.getLogger(DistributionWebServiceImpl.class);
  
  private DistributionService service;
  public void setService(DistributionService service) {
    this.service = service;
  }

  public void unsetService(DistributionService service) {
    this.service = null;
  }
  
  @WebMethod()
  public void distribute(@WebParam(name="distribution-entity") MediapackageType mediaPackage) {
    logger.info("Distributing " + mediaPackage);
    try {
      service.distribute(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromManifest(
              IOUtils.toInputStream(mediaPackage.toXml())));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
