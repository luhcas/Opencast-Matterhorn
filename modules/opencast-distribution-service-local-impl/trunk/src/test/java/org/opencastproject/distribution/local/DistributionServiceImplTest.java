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
  
  private DistributionServiceImpl service = null;
  private MediaPackage mp = null;
  private File distributionRoot = null;

  @Before
  public void setup() throws Exception {
    File mediaPackageRoot = new File("./target/test-classes");
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    builder.setSerializer(new DefaultMediaPackageSerializerImpl(mediaPackageRoot));
    mp = builder.loadFromManifest(this.getClass().getResourceAsStream("/mediapackage.xml"));
    
    distributionRoot = new File("./target/static");
    service = new DistributionServiceImpl(distributionRoot);
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    service.setWorkspace(workspace);

    EasyMock.expect(workspace.get((URL)EasyMock.anyObject())).andReturn(new File(mediaPackageRoot, "media.mov"));
    EasyMock.expect(workspace.get((URL)EasyMock.anyObject())).andReturn(new File(mediaPackageRoot, "dublincore.xml"));
    EasyMock.expect(workspace.get((URL)EasyMock.anyObject())).andReturn(new File(mediaPackageRoot, "mpeg7.xml"));
    EasyMock.expect(workspace.get((URL)EasyMock.anyObject())).andReturn(new File(mediaPackageRoot, "attachment.txt"));
    EasyMock.replay(workspace);
  }

  @After
  public void teardown() throws Exception {
    FileUtils.deleteDirectory(distributionRoot);
  }
  
  @Test
  public void testDistribution() throws Exception {
    service.distribute(mp, new String[] {"track-1", "catalog-1"}); // "catalog-2" and "notes" are not to be distributed
    File mpDir = new File(distributionRoot, mp.getIdentifier().compact());
    Assert.assertTrue(mpDir.exists());
    File mediaDir = new File(mpDir, "media");
    File metadataDir = new File(mpDir, "metadata");
    File attachmentsDir = new File(mpDir, "attachments");
    Assert.assertTrue(mediaDir.exists());
    Assert.assertTrue(metadataDir.exists());
    Assert.assertTrue(attachmentsDir.exists());
    Assert.assertTrue(new File(mediaDir, "media.mov").exists());
    Assert.assertTrue(new File(metadataDir, "dublincore.xml").exists());
    Assert.assertTrue( ! new File(metadataDir, "mpeg7.xml").exists());
    Assert.assertTrue( ! new File(attachmentsDir, "attachment.txt").exists());
  }

}
