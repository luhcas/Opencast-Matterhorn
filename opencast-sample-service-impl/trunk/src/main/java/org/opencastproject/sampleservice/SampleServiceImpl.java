/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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
package org.opencastproject.sampleservice;


import org.opencastproject.sampleservice.api.SampleService;

import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import javax.ws.rs.Path;

@Path("/")
public class SampleServiceImpl implements SampleService {
  private static final Logger logger = LoggerFactory.getLogger(SampleServiceImpl.class);
  private static final String DOCS;
  static {
    DOCS = "the documentation goes here";
  }

  public InputStream getFileFromRepository(String path) {
    logger.warn("not implemented");
    return null;
  }

  public void setFileInRepository(String path, MultipartBody body) {
    logger.warn("not implemented");
  }
  
  public String getDocumentation() {
    logger.warn("not implemented");
    return DOCS;
  }
}
