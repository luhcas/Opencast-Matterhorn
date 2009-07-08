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

import org.opencastproject.repository.api.OpencastRepository;

import org.opencastproject.sampleservice.api.SampleService;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Path;

@Path("/")
public class SampleServiceImpl implements SampleService {
  private static final Logger logger = LoggerFactory.getLogger(SampleServiceImpl.class);
  private static final String DOCS;
  static {
    DOCS = "the documentation goes here";
  }

  protected OpencastRepository repo;

  public SampleServiceImpl(OpencastRepository repo) {
    this.repo = repo;
  }
  public SampleServiceImpl() {}
  public void setRepository(OpencastRepository repo) {
    this.repo = repo;
  }

  public InputStream getFileFromRepository(String path) {
    path = "/" + path;
    logger.debug("Getting content from path " + path);
    return repo.getObject(InputStream.class, path);
  }

  public void setFileInRepository(String path, MultipartBody body) {
    path = "/" + path;
    logger.debug("Setting " + body + " to path " + path);
    Attachment a = body.getAllAttachments().get(0);
    try {
      InputStream in = a.getDataHandler().getInputStream();
      repo.putObject(in, path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public String getDocumentation() {
    return DOCS;
  }
}
