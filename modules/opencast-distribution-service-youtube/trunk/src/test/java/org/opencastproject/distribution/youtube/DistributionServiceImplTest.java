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
package org.opencastproject.distribution.youtube;

import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URI;
@Ignore // We can't test youtube distribution without youTube credentials
public class DistributionServiceImplTest {
  
  private YoutubeDistributionService service = null;
  private MediaPackage mp = null;

  @Before
  public void setup() throws Exception {
    File mediaPackageRoot = new File("./target/test-classes");
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    builder.setSerializer(new DefaultMediaPackageSerializerImpl(mediaPackageRoot));
    mp = builder.loadFromXml(this.getClass().getResourceAsStream("/mediapackage.xml"));
    service = new YoutubeDistributionService();
    service.activate(null);
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    service.setWorkspace(workspace);

    EasyMock.expect(workspace.get((URI)EasyMock.anyObject())).andReturn(new File(mediaPackageRoot, "media.mov"));
    EasyMock.expect(workspace.get((URI)EasyMock.anyObject())).andReturn(new File(mediaPackageRoot, "dublincore.xml"));
    EasyMock.expect(workspace.get((URI)EasyMock.anyObject())).andReturn(new File(mediaPackageRoot, "mpeg7.xml"));
    EasyMock.expect(workspace.get((URI)EasyMock.anyObject())).andReturn(new File(mediaPackageRoot, "attachment.txt"));
    EasyMock.replay(workspace);
  }

  @Test
  public void testDistribution() throws Exception {
    service.distribute(mp, new String[] {"track-1", "catalog-1"}); // "catalog-2" and "notes" are not to be distributed

    // CHANGE ME: figure out assertaions for Youtube delivery here

    // File mpDir = new File(distributionRoot, mp.getIdentifier().compact());
    // Assert.assertTrue(mpDir.exists());
    // File mediaDir = new File(mpDir, "media");
    // File metadataDir = new File(mpDir, "metadata");
    // File attachmentsDir = new File(mpDir, "attachments");
    // Assert.assertTrue(mediaDir.exists());
    // Assert.assertTrue(metadataDir.exists());
    // Assert.assertTrue(attachmentsDir.exists());
    // Assert.assertTrue(new File(mediaDir, "track-1.mov").exists()); // the filenames are changed to reflect the element ID
    // Assert.assertTrue(new File(metadataDir, "catalog-1.xml").exists());
    // Assert.assertTrue( ! new File(metadataDir, "mpeg7.xml").exists());
    // Assert.assertTrue( ! new File(attachmentsDir, "attachment.txt").exists());
  }

}
