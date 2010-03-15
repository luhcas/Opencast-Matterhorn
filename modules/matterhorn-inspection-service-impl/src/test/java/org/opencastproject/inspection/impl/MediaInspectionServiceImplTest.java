/**
 *  Copyright 2009, 2010 The Regents of the University of California
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
package org.opencastproject.inspection.impl;

import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.MimeType;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;

public class MediaInspectionServiceImplTest {
  private MediaInspectionServiceImpl service = null;
  private Workspace workspace = null;

  private URI uriTrack;
  private Track track;

  @Before
  public void setup() throws Exception {
    uriTrack = MediaInspectionServiceImpl.class.getResource("/av.mov").toURI();
    File f = new File(uriTrack);
    // set up services and mock objects
    service = new MediaInspectionServiceImpl();
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(uriTrack)).andReturn(f);
    EasyMock.expect(workspace.get(uriTrack)).andReturn(f);
    EasyMock.expect(workspace.get(uriTrack)).andReturn(f);
    EasyMock.replay(workspace);
    service.setWorkspace(workspace);
  }

  @After
  public void teardown() {

  }

  @Test
  public void testInspection() throws Exception {
    track = service.inspect(uriTrack);
    // test the returned values
    Checksum cs = Checksum.create(ChecksumType.fromString("md5"), "9d3523e464f18ad51f59564acde4b95a");
    Assert.assertEquals(track.getChecksum(), cs);
    Assert.assertEquals(track.getMimeType().getType(), "video");
    Assert.assertEquals(track.getMimeType().getSubtype(), "quicktime");
    Assert.assertEquals(track.getDuration(), 14546);
  }

  @Test
  public void testEnrichment() throws Exception {
    // init a track with inspect
    track = service.inspect(uriTrack);
    // make changes to metadata
    Checksum cs = track.getChecksum();
    track.setChecksum(null);
    MimeType mt = new MimeType("video", "flash");
    track.setMimeType(mt);
    // test the enrich scenario
    Track newTrack = service.enrich(track, false);
    Assert.assertEquals(newTrack.getChecksum(), cs);
    Assert.assertEquals(newTrack.getMimeType(), mt);
    Assert.assertEquals(newTrack.getDuration(), 14546);
    // test the override scenario
    newTrack = service.enrich(track, true);
    Assert.assertEquals(newTrack.getChecksum(), cs);
    Assert.assertNotSame(newTrack.getMimeType(), mt);
    Assert.assertEquals(newTrack.getDuration(), 14546);
  }

}
