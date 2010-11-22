/**
 *  Copyright 2010 The Regents of the University of California
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
package org.opencastproject.videosegmenter.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.metadata.mpeg7.MultimediaContentType;
import org.opencastproject.metadata.mpeg7.Segment;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.Iterator;

/**
 * Test class for video segmentation.
 */
public class VideoSegmenterTest {

  /** Video file to test. Contains a new scene at 00:12 */
  protected static final String mediaResource = "/scene-change.mov";

  /** Duration of whole movie */
  protected static final long mediaDuration = 20000L;

  /** Duration of the first segment */
  protected static final long firstSegmentDuration = 11000L;

  /** Duration of the seconds segment */
  protected static final long secondSegmentDuration = mediaDuration - firstSegmentDuration;

  /** The video segmenter */
  protected VideoSegmenterServiceImpl vsegmenter = null;

  protected Mpeg7CatalogService mpeg7Service = null;

  /** The media url */
  protected static TrackImpl track = null;

  /** Temp file */
  private File tempFile = null;

  /**
   * Copies test files to the local file system, since jmf is not able to access movies from the resource section of a
   * bundle.
   * 
   * @throws Exception
   *           if setup fails
   */
  @BeforeClass
  public static void setUpClass() throws Exception {
    track = TrackImpl.fromURI(VideoSegmenterTest.class.getResource(mediaResource).toURI());
    track.setFlavor(MediaPackageElements.PRESENTATION_SOURCE);
    track.setMimeType(MimeTypes.MJPEG);
    track.addStream(new VideoStreamImpl());
    track.setDuration(20000);
    System.setProperty("java.awt.headless", "true");
    System.setProperty("awt.toolkit", "sun.awt.HeadlessToolkit");
  }

  /**
   * Setup for the video segmenter service, including creation of a mock workspace.
   * 
   * @throws Exception
   *           if setup fails
   */
  @Before
  public void setUp() throws Exception {
    mpeg7Service = new Mpeg7CatalogService();
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(new File(track.getURI()));
    tempFile = File.createTempFile(getClass().getName(), "xml");
    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andAnswer(new IAnswer<URI>() {
      public URI answer() throws Throwable {
        InputStream in = (InputStream) EasyMock.getCurrentArguments()[2];
        IOUtils.copy(in, new FileOutputStream(tempFile));
        return tempFile.toURI();
      }
    });
    EasyMock.replay(workspace);
    Job receipt = new JobStub();

    ServiceRegistry remoteServiceManager = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(remoteServiceManager.createJob((String) EasyMock.anyObject())).andReturn(receipt).anyTimes();
    EasyMock.replay(remoteServiceManager);

    vsegmenter = new VideoSegmenterServiceImpl();
    vsegmenter.setExecutorThreads(1);
    vsegmenter.setMpeg7CatalogService(mpeg7Service);
    vsegmenter.setWorkspace(workspace);
    vsegmenter.setRemoteServiceManager(remoteServiceManager);
  }

  /**
   * @throws java.io.File.IOException
   */
  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(tempFile);
  }

  @Test
  public void testImageExtraction() {
    // int undefined = -16777216;

  }

  @Test
  public void testAnalyze() throws Exception {
    Job receipt = vsegmenter.segment(track, true);
    Catalog catalog = (Catalog) AbstractMediaPackageElement.getFromXml(receipt.getPayload());

    Mpeg7Catalog mpeg7 = new Mpeg7CatalogImpl(catalog.getURI().toURL().openStream());

    // Is there multimedia content in the mpeg7?
    assertTrue("Audiovisual content was expected", mpeg7.hasVideoContent());
    assertNotNull("Audiovisual content expected", mpeg7.multimediaContent().next().elements().hasNext());

    MultimediaContentType contentType = mpeg7.multimediaContent().next().elements().next();

    // Is there at least one segment?
    TemporalDecomposition<? extends Segment> segments = contentType.getTemporalDecomposition();
    Iterator<? extends Segment> si = segments.segments();
    assertTrue(si.hasNext());
    Segment firstSegment = si.next();
    MediaTime firstSegmentMediaTime = firstSegment.getMediaTime();
    long startTime = firstSegmentMediaTime.getMediaTimePoint().getTimeInMilliseconds();
    long duration = firstSegmentMediaTime.getMediaDuration().getDurationInMilliseconds();
    assertEquals("Unexepcted start time of second segment", 0, startTime);
    assertEquals("Unexpected duration of first segment", firstSegmentDuration, duration);

    // What about the second one?
    assertTrue("Video is expected to have more than one segment", si.hasNext());

    Segment secondSegment = si.next();
    MediaTime secondSegmentMediaTime = secondSegment.getMediaTime();
    startTime = secondSegmentMediaTime.getMediaTimePoint().getTimeInMilliseconds();
    duration = secondSegmentMediaTime.getMediaDuration().getDurationInMilliseconds();
    assertEquals("Unexpected start time of second segment", firstSegmentDuration, startTime);
    assertEquals("Unexpected duration of second segment", secondSegmentDuration, duration);

    // There should be no third segment
    assertFalse("Found an unexpected third video segment", si.hasNext());
  }

  class JobStub implements Job {
    String payload;
    Status status;

    public String getHost() {
      return null;
    }

    public long getId() {
      return -1;
    }

    public Status getStatus() {
      return status;
    }

    public String getJobType() {
      return "analysis-test";
    }

    public void setHost(String host) {
    }

    public void setId(long id) {
    }

    public void setStatus(Status status) {
      this.status = status;
    }

    public void setType(String type) {
    }

    public String toXml() {
      return null;
    }

    public Date getDateCompleted() {
      return null;
    }

    public Date getDateCreated() {
      return null;
    }

    public Date getDateStarted() {
      return null;
    }
    
    public String getPayload() {
      return payload;
    }
    
    public void setPayload(String payload) {
      this.payload = payload;
    }
  }

}
