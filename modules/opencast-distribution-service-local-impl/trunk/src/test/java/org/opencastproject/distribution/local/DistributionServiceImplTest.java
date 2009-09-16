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
package org.opencastproject.distribution.local;

import org.opencastproject.distribution.local.DistributionServiceImpl;
import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

public class DistributionServiceImplTest {
  private DistributionServiceImpl service;
  private MediaPackage mp;

  @Before
  public void setup() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    builder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("./target/test-classes")));
    mp = builder.loadFromManifest(this.getClass().getResourceAsStream("/mediapackage.xml"));
    service = new DistributionServiceImpl();
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    service.setWorkspace(workspace);
    EasyMock.expect(workspace.get((URL)EasyMock.anyObject())).andReturn(new File("target/test-classes/media.mov"));
    EasyMock.expect(workspace.get((URL)EasyMock.anyObject())).andReturn(new File("target/test-classes/dublincore.xml"));
    EasyMock.expect(workspace.get((URL)EasyMock.anyObject())).andReturn(new File("target/test-classes/mpeg7.xml"));
    EasyMock.expect(workspace.get((URL)EasyMock.anyObject())).andReturn(new File("target/test-classes/attachment.txt"));
    EasyMock.replay(workspace);
  }

  @After
  public void teardown() throws Exception {
    File distDirectory = new File(System.getProperty("java.io.tmpdir") + File.separator + "opencast" +
            File.separator + "static" + File.separator + mp.getIdentifier().getLocalName());
    FileUtils.deleteDirectory(distDirectory);
  }
  
  @Test
  public void testDistribution() throws Exception {
    service.distribute(mp);
    String staticDir = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "static";
    File mpDir = new File(staticDir, mp.getIdentifier().getLocalName());
    Assert.assertTrue(mpDir.exists());
    File mediaDir = new File(mpDir, "media");
    Assert.assertTrue(mediaDir.exists());
    Assert.assertTrue(new File(mediaDir, "media.mov").exists());
  }
}
