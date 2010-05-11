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
package org.opencastproject.analysis.vsegmenter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.track.TrackImpl;
import org.opencastproject.media.mediapackage.track.VideoStreamImpl;
import org.opencastproject.metadata.mpeg7.ContentSegment;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.MultimediaContentType;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import org.opencastproject.receipt.api.Receipt;
import org.opencastproject.receipt.api.ReceiptService;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import de.schlichtherle.io.File;
import de.schlichtherle.io.FileOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;

/**
 * Test class for video segmentation.
 */
@Ignore
public class VideoSegmenterTest {

  /** Video file to test. Contains a new scene at 00:12 */
  protected static final String mediaResource = "/scene-change.mov";

  /** Duration of whole movie */
  protected static final long mediaDuration = 20000L;

  /** Duration of the first segment */
  protected static final long firstSegmentDuration = 12000L;

  /** Duration of the seconds segment */
  protected static final long secondSegmentDuration = mediaDuration - firstSegmentDuration;

  /** The video segmenter */
  protected VideoSegmenter vsegmenter = null;

  /** The media url */
  protected static TrackImpl track = null;

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
    System.setProperty("java.awt.headless", "true");
    System.setProperty("awt.toolkit", "sun.awt.HeadlessToolkit");
  }

  static String collection;
  static String filename;

  /**
   * Setup for the video segmenter service, including creation of a mock workspace.
   * 
   * @throws Exception
   *           if setup fails
   */
  @Before
  public void setUp() throws Exception {
    WorkingFileRepository fileRepo = EasyMock.createNiceMock(WorkingFileRepository.class);
    EasyMock.expect(
            fileRepo.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andAnswer(new IAnswer<URI>() {
      public URI answer() throws Throwable {
        Object[] args = EasyMock.getCurrentArguments();
        collection = (String) args[0];
        filename = (String) args[1] + ".xml";
        InputStream in = (InputStream) args[2];
        File file = new File("target", filename);
        FileOutputStream out = new FileOutputStream(file);
        IOUtils.copy(in, out);
        IOUtils.closeQuietly(out);
        IOUtils.closeQuietly(in);
        return file.toURI();
      }
    });
    EasyMock.replay(fileRepo);
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(new File(track.getURI()));
    EasyMock.replay(workspace);
    Receipt receipt = new ReceiptStub();

    ReceiptService receiptService = EasyMock.createNiceMock(ReceiptService.class);
    EasyMock.expect(receiptService.createReceipt((String) EasyMock.anyObject())).andReturn(receipt).anyTimes();
    EasyMock.replay(receiptService);

    vsegmenter = new VideoSegmenter();
    vsegmenter.setFileRepository(fileRepo);
    vsegmenter.setWorkspace(workspace);
    vsegmenter.setReceiptService(receiptService);
  }

  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(new File("target", filename));
  }

  @Test
  public void testImageExtraction() {
    // int undefined = -16777216;

  }

  @Test
  public void testAnalyze() {
    Receipt receipt = vsegmenter.analyze(track, true);
    Mpeg7Catalog catalog = (Mpeg7Catalog) receipt.getElement();

    // Is there multimedia content in the catalog?
    assertTrue("Audiovisual content was expected", catalog.hasVideoContent());
    assertNotNull("Audiovisual content expected", catalog.multimediaContent().next().elements().hasNext());

    MultimediaContentType contentType = catalog.multimediaContent().next().elements().next();

    // Is there at least one segment?
    TemporalDecomposition<? extends ContentSegment> segments = contentType.getTemporalDecomposition();
    Iterator<? extends ContentSegment> si = segments.segments();
    assertTrue(si.hasNext());
    ContentSegment firstSegment = si.next();
    MediaTime firstSegmentMediaTime = firstSegment.getMediaTime();
    long startTime = firstSegmentMediaTime.getMediaTimePoint().getTimeInMilliseconds();
    long duration = firstSegmentMediaTime.getMediaDuration().getDurationInMilliseconds();
    assertEquals("Unexepcted start time of second segment", 0, startTime);
    assertEquals("Unexpected duration of first segment", firstSegmentDuration, duration);

    // What about the second one?
    assertTrue("Video is expected to have more than one segment", si.hasNext());

    ContentSegment secondSegment = si.next();
    MediaTime secondSegmentMediaTime = secondSegment.getMediaTime();
    startTime = secondSegmentMediaTime.getMediaTimePoint().getTimeInMilliseconds();
    duration = secondSegmentMediaTime.getMediaDuration().getDurationInMilliseconds();
    assertEquals("Unexpected start time of second segment", firstSegmentDuration, startTime);
    assertEquals("Unexpected duration of second segment", secondSegmentDuration, duration);

    // There should be no third segment
    assertFalse("Found an unexpected third video segment", si.hasNext());
  }

  class ReceiptStub implements Receipt {
    MediaPackageElement element;
    Status status;

    public MediaPackageElement getElement() {
      return element;
    }

    public String getHost() {
      return null;
    }

    public String getId() {
      return null;
    }

    public Status getStatus() {
      return status;
    }

    public String getType() {
      return "analysis-test";
    }

    public void setElement(MediaPackageElement element) {
      this.element = element;
    }

    public void setHost(String host) {
    }

    public void setId(String id) {
    }

    public void setStatus(Status status) {
      this.status = status;
    }

    public void setType(String type) {
    }
  }

}
