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
package org.opencastproject.media.mediapackage.track;

import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.elementbuilder.PresenterTrackBuilderPlugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;

/**
 * Test case to Test the implementation of {@link TrackImpl}.
 */
public class TrackTest {

  /** The track to test */
  TrackImpl track = null;
  
  /** HTTP track url */
  URI httpUrl = null;

  /** RTMP track url */
  URI rtmpUrl = null;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    httpUrl = new URI("http://downloads.opencastproject.org/media/movie.m4v");
    rtmpUrl = new URI("rtmp://downloads.opencastproject.org/media/movie.m4v");
    track = TrackImpl.fromURI(httpUrl);
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.track.TrackImpl#fromURI(URI)}.
   */
  @Test
  public void testFromURL() {
    //track = TrackImpl.fromURL(httpUrl);
    //track = TrackImpl.fromURL(rtmpUrl);
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.track.TrackImpl#setDuration(long)}.
   */
  @Test
  public void testSetDuration() {
    track.setDuration(-1);
    track.setDuration(10);
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.track.TrackImpl#getDuration()}.
   */
  @Test
  public void testGetDuration() {
    assertEquals(-1, track.getDuration());
  }

  /**
   * Test method for {@link PresenterTrackBuilderPlugin#accept(URI, org.opencastproject.media.mediapackage.MediaPackageElement.Type, org.opencastproject.media.mediapackage.MediaPackageElementFlavor)}
   * @throws Exception
   */
  @Test
  public void testPresenterTrackAccept() throws Exception {
    assertTrue(new PresenterTrackBuilderPlugin().accept(new URI("uri"), Track.TYPE, MediaPackageElements.PRESENTER_TRACK));
  }
  /**
   * Test method for {@link org.opencastproject.media.mediapackage.track.TrackImpl#getStreams()}.
   */
  @Test @Ignore
  public void testGetStreams() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.track.TrackImpl#addStream(org.opencastproject.media.mediapackage.track.AbstractStreamImpl)}.
   */
  @Test @Ignore
  public void testAddStream() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.track.TrackImpl#getDescription()}.
   */
  @Test @Ignore
  public void testGetDescription() {
    fail("Not yet implemented"); // TODO
  }

}
