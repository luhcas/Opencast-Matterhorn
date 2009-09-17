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
package org.opencastproject.delivery.impl;

import org.opencastproject.delivery.api.DeliveryService;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

public class DeliveryServiceImplTest {
  private DeliveryService service = null;
  @Before
  public void setup() {
    service = new DeliveryServiceImpl();
  }

  @After
  public void teardown() {
    service = null;
  }
  
  @Test
  public void testUpDown() throws Exception
  {
    InputStream in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");

    System.out.println(in);
    
    service.upload(in, "opencast_header.gif");
    
    in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
    byte[] bytesUpload = IOUtils.toByteArray(in);
    
    InputStream downIS = service.download("opencast_header.gif");
    byte[] bytesDownload = IOUtils.toByteArray(downIS);
 
    Assert.assertEquals(bytesDownload.length, bytesUpload.length);
  }
}
