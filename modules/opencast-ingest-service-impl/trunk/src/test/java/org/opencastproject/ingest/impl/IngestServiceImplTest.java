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

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.media.mediapackage.Cover;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.Mpeg7Catalog;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.fail;

import java.net.URL;

public class IngestServiceImplTest {
  private IngestService service = null;
  private String mediaPackageId = null;
  private URL urlTrack;
  private URL urlCatalog;
  private URL urlAttachment;

  // private URL urlManifest;

  @Before
  public void setup() {
    service = new IngestServiceImpl();
    urlTrack = IngestServiceImplTest.class.getResource("/av.mov");
    urlCatalog = IngestServiceImplTest.class.getResource("/mpeg-7.xml");
    urlAttachment = IngestServiceImplTest.class.getResource("/cover.png");
    // urlManifest = IngestServiceImplTest.class.getResource("/source-manifest.xml");
  }

  @After
  public void teardown() {
    service = null;
    urlTrack = null;
    urlCatalog = null;
    urlAttachment = null;
    // urlManifest = null;
  }

  @Test
  public void testThinClient() {
    try {
      mediaPackageId = service.createMediaPackage();
      service.addTrack(urlTrack, MediaPackageElements.INDEFINITE_TRACK, mediaPackageId);
      service.addCatalog(urlCatalog, Mpeg7Catalog.FLAVOR, mediaPackageId);
      service.addAttachment(urlAttachment, Cover.FLAVOR, mediaPackageId);
      service.ingest(mediaPackageId);
    } catch (MediaPackageException e) {
      fail("Unable to create or access media Package: " + e.getMessage());
    } catch (NullPointerException e) {
      fail("Media package not found in local ingest memory");
    } catch (UnsupportedElementException e) {
      fail("Error thrown while trying to load media into media package: " + e.getMessage());
    }
    // TODO: listen to event of ingest finish check in file repo if files are really there.
  }

  // TODO: Problem with local paths in mediapackage parser
  // @Test
  // public void testThickClient() {
  // try {
  // mediaPackageId = service.addMediaPackage(urlManifest.openStream());
  // service.ingest(mediaPackageId);
  // } catch (IOException e) {
  // fail("Unable to load mock manifest file");
  // } catch (MediaPackageException e) {
  // fail("Unable to create a mock media package");
  // }
  // // TODO: listen to event of ingest finish check in file repo if files are really there.
  // }

  @Test
  public void testDiscard() {
    try {
      mediaPackageId = service.createMediaPackage();
    } catch (MediaPackageException e1) {
      fail("Media package could not be created");
    }
    service.discardMediaPackage(mediaPackageId);
    try {
      service.addTrack(urlTrack, MediaPackageElements.INDEFINITE_TRACK, mediaPackageId);
    } catch (Exception e) {
      // exception expected and wanted
      return;
    }
    fail("Media package could not be discarded");
  }

}
