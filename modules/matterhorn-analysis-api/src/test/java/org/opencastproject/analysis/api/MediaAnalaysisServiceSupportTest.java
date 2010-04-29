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
package org.opencastproject.analysis.api;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.track.TrackImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;

import junit.framework.TestCase;

import java.net.URI;

/**
 * Test case for {@link MediaAnalysisServiceSupport}.
 * 
 */
public class MediaAnalaysisServiceSupportTest extends TestCase {

  MediaAnalysisServiceSupport analyzer = null;
  MediaPackageElementFlavor resultingFlavor = MediaAnalysisFlavor.TEXTS_FLAVOR;
  MediaPackageElementFlavor[] requiredFlavors = new MediaPackageElementFlavor[] { MediaAnalysisFlavor.SEGMENTS_FLAVOR };
  MediaPackage mediaPackage = null;
  Track track = null;
  String trackId = "track-1";

  /**
   * {@inheritDoc}
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    analyzer = new MediaAnalysisTestService(resultingFlavor, requiredFlavors);
    mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    track = TrackImpl.fromURI(new URI("http://localhost/track.mov"));
    track.setIdentifier(trackId);
    mediaPackage.add(track);
  }

  /**
   * Test method for
   * {@link org.opencastproject.analysis.api.MediaAnalysisServiceSupport#analyze(org.opencastproject.media.mediapackage.MediaPackage, java.lang.String)}
   * .
   */
  public void testAnalyzeMediaPackageString() {
    assertNotNull(analyzer.analyze(track, true));
  }

  /**
   * Test method for {@link org.opencastproject.analysis.api.MediaAnalysisServiceSupport#produces()}.
   */
  public void testProduces() {
    assertNotNull(analyzer.produces());
    assertEquals(MediaAnalysisFlavor.TEXTS_FLAVOR, analyzer.produces());
  }

  /**
   * Test method for {@link org.opencastproject.analysis.api.MediaAnalysisServiceSupport#requires()}.
   */
  public void testRequires() {
    assertNotNull(analyzer.requires());
    assertEquals(1, analyzer.requires().length);
    assertEquals(MediaAnalysisFlavor.SEGMENTS_FLAVOR, analyzer.requires()[0]);
  }

  /**
   * Test method for
   * {@link org.opencastproject.analysis.api.MediaAnalysisServiceSupport#isSupported(org.opencastproject.media.mediapackage.MediaPackageElement)}
   * .
   */
  public void isSupported() {
    try {
      analyzer.isSupported(null);
    } catch (IllegalArgumentException e) {
      // This is expected
    }
    
    assertFalse(analyzer.isSupported(track));

    Mpeg7CatalogImpl catalog = Mpeg7CatalogImpl.newInstance();
    catalog.setFlavor(MediaAnalysisFlavor.SEGMENTS_FLAVOR);
    mediaPackage.add(catalog);
    assertTrue(analyzer.isSupported(track));
  }

}
