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

import org.opencastproject.metadata.mpeg7.ContentSegment;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.MultimediaContentType;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URL;
import java.util.Iterator;

/**
 * Test class for video segmentation.
 */
public class VideoSegmenterTest {

  /** Video file to test. Contains a new scene at 00:12 */
  protected static final String mediaResource = "/scene-change.mov";

  /** Duration of whole movie */
  protected static final int mediaDuration = 20;

  /** Duration of the first segment */
  protected static final int firstSegmentDuration = 12;

  /** Duration of the seconds segment */
  protected static final int secondSegmentDuration = mediaDuration - firstSegmentDuration;

  /** The video segmenter */
  protected VideoSegmenter vsegmenter = null;

  /** The media url */
  protected static URL mediaUrl = null;

  /**
   * Copies test files to the local file system, since jmf is not able to access movies from the resource section of a
   * bundle.
   * 
   * @throws Exception
   *           if setup fails
   */
  @BeforeClass
  public static void setUpClass() throws Exception {
    mediaUrl = VideoSegmenterTest.class.getResource(mediaResource).toURI().toURL();
  }

  /**
   * Setup for the video segmenter service, including creation of a mock workspace.
   * 
   * @throws Exception
   *           if setup fails
   */
  @Before
  public void setUp() throws Exception {
    vsegmenter = new VideoSegmenter();
  }

  @Test
  @Ignore
  public void testImageExtraction() {
    // int undefined = -16777216;

  }

  @Test
  @Ignore
  public void testAnalyze() {
    Mpeg7Catalog catalog = vsegmenter.analyze(mediaUrl);

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
    long startTime = firstSegmentMediaTime.getMediaTimePoint().getSeconds();
    long duration = firstSegmentMediaTime.getMediaDuration().getDurationInMilliseconds()/1000;
    assertEquals("Unexepcted start time of second segment", 0, startTime);
    assertEquals("Unexpected duration of first segment", firstSegmentDuration, duration);

    // What about the second one?
    assertTrue("Video is expected to have more than one segment", si.hasNext());

    ContentSegment secondSegment = si.next();
    MediaTime secondSegmentMediaTime = secondSegment.getMediaTime();
    startTime = secondSegmentMediaTime.getMediaTimePoint().getTimeInMilliseconds()/1000;
    duration = secondSegmentMediaTime.getMediaDuration().getDurationInMilliseconds()/1000;
    assertEquals("Unexpected start time of second segment", firstSegmentDuration, startTime);
    assertEquals("Unexpected duration of second segment", secondSegmentDuration, duration);

    // There should be no third segment
    assertFalse("Found an unexpected third video segment", si.hasNext());
  }

}
