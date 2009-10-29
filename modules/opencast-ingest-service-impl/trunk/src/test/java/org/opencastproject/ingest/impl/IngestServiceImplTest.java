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

import org.opencastproject.media.mediapackage.Cover;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.Mpeg7Catalog;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.handle.HandleException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class IngestServiceImplTest {
	private IngestServiceImpl service = null;
	private Workspace workspace = null;
	private MediaPackage mediaPackage = null;
	private URL urlTrack;
	private URL urlTrack1;
	private URL urlTrack2;
	private URL urlCatalog;
	private URL urlCatalog1;
	private URL urlCatalog2;
	private URL urlAttachment;
	private URL urlPackage;

	@Before
	public void setup() {
		urlTrack = IngestServiceImplTest.class.getResource("/av.mov");
		urlTrack1 = IngestServiceImplTest.class.getResource("/vonly.mov");
		urlTrack2 = IngestServiceImplTest.class.getResource("/aonly.mov");
		urlCatalog = IngestServiceImplTest.class.getResource("/mpeg-7.xml");
		urlCatalog1 = IngestServiceImplTest.class
				.getResource("/dublincore.xml");
		urlCatalog2 = IngestServiceImplTest.class
				.getResource("/series-dublincore.xml");
		urlAttachment = IngestServiceImplTest.class.getResource("/cover.png");
		urlPackage = IngestServiceImplTest.class.getResource("/data.zip");
		// set up service and mock workspace
		service = new IngestServiceImpl();
		workspace = EasyMock.createNiceMock(Workspace.class);
		try {
			EasyMock.expect(
					workspace.getURL((String) EasyMock.anyObject(),
							(String) EasyMock.anyObject())).andReturn(urlTrack);
			EasyMock.expect(
					workspace.getURL((String) EasyMock.anyObject(),
							(String) EasyMock.anyObject())).andReturn(
					urlCatalog);
			EasyMock.expect(
					workspace.getURL((String) EasyMock.anyObject(),
							(String) EasyMock.anyObject())).andReturn(
					urlAttachment);
			EasyMock.expect(
					workspace.getURL((String) EasyMock.anyObject(),
							(String) EasyMock.anyObject()))
					.andReturn(urlTrack1);
			EasyMock.expect(
					workspace.getURL((String) EasyMock.anyObject(),
							(String) EasyMock.anyObject()))
					.andReturn(urlTrack2);
			EasyMock.expect(
					workspace.getURL((String) EasyMock.anyObject(),
							(String) EasyMock.anyObject())).andReturn(
					urlCatalog1);
			EasyMock.expect(
					workspace.getURL((String) EasyMock.anyObject(),
							(String) EasyMock.anyObject())).andReturn(
					urlCatalog2);
			EasyMock.expect(
					workspace.getURL((String) EasyMock.anyObject(),
							(String) EasyMock.anyObject())).andReturn(
					urlCatalog);
			EasyMock.replay(workspace);
		} catch (MalformedURLException e) {
			fail("Failed to set up mock workspace");
		}
		service.setWorkspace(workspace);
		service.setTempFolder("target/temp/");

	}

	@After
	public void teardown() {

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
		} catch (IOException e) {
			fail("Unable to put files into repo.");
		}
	}

	@Test
	public void testThickClient() throws Exception {
		try {
			mediaPackage = service.addZippedMediaPackage(urlPackage
					.openStream());
		} catch (IOException e) {
			fail("Unable to load media package");
		} catch (MediaPackageException e) {
			fail("Unable to consume a media package");
		} catch (Exception e) {
			fail("Unable to consume a media package");
		}
	}

}
