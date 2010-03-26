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

  MediaAnalysisServiceSupport analysis = null;
  MediaPackageElementFlavor resultingFlavor = MediaAnalysisFlavor.TEXTS_FLAVOR;
  MediaPackageElementFlavor[] requiredFlavors = new MediaPackageElementFlavor[] { MediaAnalysisFlavor.SEGMENTS_FLAVOR };
  MediaPackage mediaPackage = null;
  String trackId = "track-1";

  /**
   * {@inheritDoc}
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    analysis = new MediaAnalysisTestService(resultingFlavor, requiredFlavors);
    mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    Track t = TrackImpl.fromURI(new URI("http://localhost/track.mov"));
    t.setIdentifier(trackId);
    mediaPackage.add(t);
  }

  /**
   * Test method for
   * {@link org.opencastproject.analysis.api.MediaAnalysisServiceSupport#analyze(org.opencastproject.media.mediapackage.MediaPackage, java.lang.String)}
   * .
   */
  public void testAnalyzeMediaPackageString() {
    assertNotNull(analysis.analyze(mediaPackage, trackId));
    try {
      analysis.analyze(mediaPackage, "non-existent");
      fail("Should have failed due to non-existent track id");
    } catch (MediaAnalysisException e) {
      // expected
    }
  }

  /**
   * Test method for {@link org.opencastproject.analysis.api.MediaAnalysisServiceSupport#produces()}.
   */
  public void testProduces() {
    assertNotNull(analysis.produces());
    assertEquals(MediaAnalysisFlavor.TEXTS_FLAVOR, analysis.produces());
  }

  /**
   * Test method for {@link org.opencastproject.analysis.api.MediaAnalysisServiceSupport#requires()}.
   */
  public void testRequires() {
    assertNotNull(analysis.requires());
    assertEquals(1, analysis.requires().length);
    assertEquals(MediaAnalysisFlavor.SEGMENTS_FLAVOR, analysis.requires()[0]);
  }

  /**
   * Test method for
   * {@link org.opencastproject.analysis.api.MediaAnalysisServiceSupport#hasRequirementsFulfilled(org.opencastproject.media.mediapackage.MediaPackage)}
   * .
   */
  public void testHasRequirementsFulfilled() {
    assertFalse(analysis.hasRequirementsFulfilled(mediaPackage));
    Mpeg7CatalogImpl catalog = Mpeg7CatalogImpl.newInstance();
    catalog.setFlavor(MediaAnalysisFlavor.SEGMENTS_FLAVOR);
    mediaPackage.add(catalog);
    assertTrue(analysis.hasRequirementsFulfilled(mediaPackage));
  }

}
