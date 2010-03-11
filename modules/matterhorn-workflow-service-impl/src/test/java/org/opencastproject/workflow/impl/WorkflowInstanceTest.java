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
package org.opencastproject.workflow.impl;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;

import junit.framework.Assert;

import org.junit.Test;

import java.net.URI;

public class WorkflowInstanceTest {
  @Test
  public void testWorkflowWithoutOperations() throws Exception {
    WorkflowInstanceImpl workflow = new WorkflowInstanceImpl();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    workflow.setMediaPackage(mp);
    Assert.assertEquals(mp.getIdentifier(), workflow.getMediaPackage().getIdentifier());
  }

  @Test
  public void testMediaPackageSerializationInWorkflowInstance() throws Exception {
    WorkflowInstanceImpl workflow = new WorkflowInstanceImpl();
    MediaPackage src = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    Track track = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
      new URI("http://sample"), Track.TYPE, MediaPackageElements.PRESENTER_SOURCE);
    src.add(track);
    MediaPackage deserialized = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(src.toXml());
    workflow.setMediaPackage(deserialized);
    Assert.assertEquals(1, workflow.getMediaPackage().getTracks().length);
  }
  
  @Test
  public void testMediaPackageDeserialization() throws Exception {
    WorkflowInstanceImpl workflow = new WorkflowInstanceImpl();
    String xml = "<ns2:mediapackage xmlns:ns2=\"http://mediapackage.opencastproject.org\" start=\"2007-12-05T13:40:00\" duration=\"1004400000\"><media><track id=\"track-1\" type=\"presenter/source\"><mimetype>audio/mp3</mimetype><url>http://localhost:8080/workflow/samples/audio.mp3</url><checksum type=\"md5\">950f9fa49caa8f1c5bbc36892f6fd062</checksum><duration>10472</duration><audio><channels>2</channels><bitdepth>0</bitdepth><bitrate>128004.0</bitrate><samplingrate>44100</samplingrate></audio></track><track id=\"track-2\" type=\"presenter/source\"><mimetype>video/quicktime</mimetype><url>http://localhost:8080/workflow/samples/camera.mpg</url><checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum><duration>14546</duration><video><device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" /><encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" /><resolution>640x480</resolution><scanType type=\"progressive\" /><bitrate>540520</bitrate><frameRate>2</frameRate></video></track></media><metadata><catalog id=\"catalog-1\" type=\"metadata/dublincore\"><mimetype>text/xml</mimetype><url>http://localhost:8080/workflow/samples/dc-1.xml</url><checksum type=\"md5\">20e466615251074e127a1627fd0dae3e</checksum></catalog></metadata></ns2:mediapackage>";
    MediaPackage src = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(xml);
    workflow.setMediaPackage(src);
    Assert.assertEquals(2, workflow.getMediaPackage().getTracks().length);
  }
}
