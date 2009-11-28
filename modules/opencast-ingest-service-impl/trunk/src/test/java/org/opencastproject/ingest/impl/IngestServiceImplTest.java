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
package org.opencastproject.ingest.impl;

import org.opencastproject.media.mediapackage.Cover;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.Mpeg7Catalog;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.EventAdmin;

import java.io.InputStream;
import java.net.URI;

public class IngestServiceImplTest {
  private IngestServiceImpl service = null;
  private Workspace workspace = null;
  private MediaPackage mediaPackage = null;
  private URI urlTrack;
  private URI urlTrack1;
  private URI urlTrack2;
  private URI urlCatalog;
  private URI urlCatalog1;
  private URI urlCatalog2;
  private URI urlAttachment;
  private URI urlPackage;

  @Before
  public void setup() throws Exception {
    urlTrack = IngestServiceImplTest.class.getResource("/av.mov").toURI();
    urlTrack1 = IngestServiceImplTest.class.getResource("/vonly.mov").toURI();
    urlTrack2 = IngestServiceImplTest.class.getResource("/aonly.mov").toURI();
    urlCatalog = IngestServiceImplTest.class.getResource("/mpeg-7.xml").toURI();
    urlCatalog1 = IngestServiceImplTest.class.getResource("/dublincore.xml").toURI();
    urlCatalog2 = IngestServiceImplTest.class.getResource("/series-dublincore.xml").toURI();
    urlAttachment = IngestServiceImplTest.class.getResource("/cover.png").toURI();
    urlPackage = IngestServiceImplTest.class.getResource("/data.zip").toURI();
    // set up service and mock workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (InputStream) EasyMock
                    .anyObject())).andReturn(urlTrack);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (InputStream) EasyMock
                    .anyObject())).andReturn(urlCatalog);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (InputStream) EasyMock
                    .anyObject())).andReturn(urlAttachment);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (InputStream) EasyMock
                    .anyObject())).andReturn(urlTrack1);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (InputStream) EasyMock
                    .anyObject())).andReturn(urlTrack2);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (InputStream) EasyMock
                    .anyObject())).andReturn(urlCatalog1);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (InputStream) EasyMock
                    .anyObject())).andReturn(urlCatalog2);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (InputStream) EasyMock
                    .anyObject())).andReturn(urlCatalog);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog).anyTimes();
    EasyMock.replay(workspace);
    service = new IngestServiceImpl();
    service.setEventAdmin(EasyMock.createNiceMock(EventAdmin.class));
    service.setTempFolder("target/temp/");
    service.setWorkspace(workspace);
  }

  @After
  public void teardown() {

  }

  @Test
  public void testThinClient() throws Exception {
    mediaPackage = service.createMediaPackage();
    service.addTrack(urlTrack, MediaPackageElements.INDEFINITE_TRACK, mediaPackage);
    service.addCatalog(urlCatalog, Mpeg7Catalog.FLAVOR, mediaPackage);
    service.addAttachment(urlAttachment, Cover.FLAVOR, mediaPackage);
    service.ingest(mediaPackage);
  }

  @Test
  public void testThickClient() throws Exception {
    mediaPackage = service.addZippedMediaPackage(urlPackage.toURL().openStream());
  }

}
