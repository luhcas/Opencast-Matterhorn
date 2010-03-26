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
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Convencience implementation that can be used to easily build media analysis service implementations.
 */
public abstract class MediaAnalysisServiceSupport implements MediaAnalysisService {

  /** The catalog flavor that is produced by this implementation */
  protected MediaPackageElementFlavor resultingFlavor = null;

  /** The flavors that are required by this media analysis */
  protected MediaPackageElementFlavor[] requiredFlavors = new MediaPackageElementFlavor[] {};

  /**
   * Creates a new media analysis support object with the given resulting flavor and requirements.
   * 
   * @param resultingFlavor
   *          the resulting catalog flavor
   * @param requiredFlavors
   *          the required catalog flavors
   */
  protected MediaAnalysisServiceSupport(MediaPackageElementFlavor resultingFlavor,
          MediaPackageElementFlavor[] requiredFlavors) {
    this.resultingFlavor = resultingFlavor;
    if (requiredFlavors != null)
      this.requiredFlavors = requiredFlavors;
  }

  /**
   * Creates a new media analysis support object with the given resulting flavor and no requirements.
   * 
   * @param resultingFlavor
   *          the resulting catalog flavor
   */
  protected MediaAnalysisServiceSupport(MediaPackageElementFlavor resultingFlavor) {
    this(resultingFlavor, new MediaPackageElementFlavor[] {});
  }

  /**
   * Creates a new media analysis support object with the given set of required flavors.
   * 
   * @param requiredFlavors
   *          the required catalog flavors
   */
  protected MediaAnalysisServiceSupport(MediaPackageElementFlavor[] requiredFlavors) {
    this(null, requiredFlavors);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.analysis.api.MediaAnalysisService#analyze(java.net.URL)
   */
  @Override
  public abstract Mpeg7Catalog analyze(URL mediaUrl) throws MediaAnalysisException;

  /**
   * {@inheritDoc}
   * <p>
   * This implementation will simply try to extract the element from the media package and then call
   * {@link #analyze(URL)}.
   * 
   * @see org.opencastproject.analysis.api.MediaAnalysisService#analyze(org.opencastproject.media.mediapackage.MediaPackage,
   *      java.lang.String)
   */
  @Override
  public Mpeg7Catalog analyze(MediaPackage mediaPackage, String elementId) throws MediaAnalysisException {
    if (mediaPackage == null)
      throw new MediaAnalysisException("Media package must not be null");
    if (elementId == null)
      throw new MediaAnalysisException("Element identifier must not be null");

    MediaPackageElement element = mediaPackage.getElementById(elementId);
    if (element == null)
      throw new MediaAnalysisException("Element '" + elementId + "' not found in mediapackage " + mediaPackage);

    try {
      return analyze(element.getURI().toURL());
    } catch (MalformedURLException e) {
      throw new MediaAnalysisException("URI of media package element " + element + " cannot be converted to URL", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.analysis.api.MediaAnalysisService#produces()
   */
  @Override
  public MediaPackageElementFlavor produces() {
    return resultingFlavor;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.analysis.api.MediaAnalysisService#requires()
   */
  @Override
  public MediaPackageElementFlavor[] requires() {
    return requiredFlavors;
  }

  /**
   * Returns <code>true</code> if the requirements that this media analysis service defines are met.
   * 
   * @return <code>true</code> if all requirements are met
   */
  protected boolean hasRequirementsFulfilled(MediaPackage mediaPackage) {
    if (mediaPackage == null)
      throw new IllegalArgumentException("Media package cannot be null");
    for (MediaPackageElementFlavor flavor : requires()) {
      if (mediaPackage.getCatalogs(flavor).length == 0)
        return false;
    }
    return true;
  }

}
