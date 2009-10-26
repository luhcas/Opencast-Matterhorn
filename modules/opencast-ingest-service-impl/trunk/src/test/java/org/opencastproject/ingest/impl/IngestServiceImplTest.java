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

import static org.junit.Assert.fail;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.media.mediapackage.Cover;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.Mpeg7Catalog;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.handle.HandleException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.workingfilerepository.impl.WorkingFileRepositoryImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

public class IngestServiceImplTest {
	private IngestService service = null;
	private MediaPackage mediaPackage = null;
	private URL urlTrack;
	private URL urlCatalog;
	private URL urlAttachment;
	private URL urlPackage;

	@Before
	public void setup() {
		service = new IngestServiceImpl();
		service.setWorkingFileRepository(new WorkingFileRepositoryImpl());
		//service.setMediaInspection(new MediaInspectionServiceImpl());
		urlTrack = IngestServiceImplTest.class.getResource("/av.mov");
		urlCatalog = IngestServiceImplTest.class.getResource("/mpeg-7.xml");
		urlAttachment = IngestServiceImplTest.class.getResource("/cover.png");
		urlPackage = IngestServiceImplTest.class.getResource("/data.zip");
		//urlPackage = IngestServiceImplTest.class.getResource("/media.zip");
	}

	@After
	public void teardown() {
		service = null;
		urlTrack = null;
		urlCatalog = null;
		urlAttachment = null;
		urlPackage = null;
	}

	@Test
	public void testThinClient() {
		try {
			mediaPackage = service.createMediaPackage();
			service.addTrack(urlTrack, MediaPackageElements.INDEFINITE_TRACK,
					mediaPackage);
			service.addCatalog(urlCatalog, Mpeg7Catalog.FLAVOR, mediaPackage);
			service.addAttachment(urlAttachment, Cover.FLAVOR, mediaPackage);
			service.ingest(mediaPackage);
		} catch (MediaPackageException e) {
			fail("Unable to create or access media Package: " + e.getMessage());
		} catch (NullPointerException e) {
			fail("Media package not found in local ingest memory");
		} catch (UnsupportedElementException e) {
			fail("Error thrown while trying to load media into media package: "
					+ e.getMessage());
		} catch (ConfigurationException e) {
			fail("Unable to create a handle for the media package.");
		} catch (HandleException e) {
			fail("Unable to create a handle for the media package.");
		}
		// TODO: listen to event of ingest finish check in file repo if files
		// are really there.
	}

	// TODO: Check problem with local url paths
	@Test
	public void testThickClient() {
		try {
			mediaPackage = service.addMediaPackage(urlPackage.openStream());
		} catch (IOException e) {
			fail("Unable to load media package");
		} catch (MediaPackageException e) {
			fail("Unable to consume a media package");
		} catch (Exception e) {
			fail("Unable to consume a media package");
		}
		// TODO: listen to event of ingest finish check in file repo if files
		// are really there.
	}

	// @Test
	// public void testDiscard() {
	// service.discardMediaPackage(mediaPackage);
	// try {
	// Track t = mediaPackage.getTracks()[0];
	// //TODO
	// } catch (Exception e) {
	// // exception expected and wanted
	// return;
	// }
	// fail("Media package could not be discarded");
	// }

}
