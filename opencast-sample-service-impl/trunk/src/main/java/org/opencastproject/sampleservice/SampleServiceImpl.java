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

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

public class SampleServiceImpl implements SampleService {

  public static final String PROPERTY_KEY = "sample-property";

  protected OpencastRepository repo;

  public SampleServiceImpl(OpencastRepository repo) {
    this.repo = repo;
  }

  public String getSomething(String path) {
    InputStream in = repo.getObject(InputStream.class, path);
    StringWriter writer = new StringWriter();
    try {
      IOUtils.copy(in, writer);
      return writer.getBuffer().toString();
    } catch (IOException e) {
      e.printStackTrace();
      return e.toString();
    }
  }

  public void setSomething(String path, String content) {
    try {
      ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes("UTF8"));
      repo.putObject(in, path);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
