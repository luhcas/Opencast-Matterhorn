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
package org.opencastproject.inspection.impl;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.MediaPackageElement.Type;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Dictionary;

/**
 * Inspects media via the 3rd party MediaInfo tool by default, and can be configured to use other media analyzers.
 */
public class MediaInspectionServiceImpl implements MediaInspectionService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceImpl.class);
  
  Workspace workspace;
  public void setWorkspace(Workspace workspace) {
    logger.info("###### setting " + workspace);
    this.workspace = workspace;
  }

  public void unsetWorkspace(Workspace workspace) {
    logger.info("###### unsetting " + workspace);
  }

  public Track inspect(URL url) {
    logger.info("inspect(" + url + ") called, using workspace " + workspace);
    
    // Get the file from the URL
    File file = workspace.get(url);
    
    MediaContainerMetadata metadata = null;
    try {
      MediaAnalyzerFactory analyzerFactory = MediaAnalyzerFactory.newInstance();
      MediaAnalyzer mediaAnalyzer = analyzerFactory.newMediaAnalyzer();
      metadata = mediaAnalyzer.analyze(file);
    } catch (Throwable t) {
      throw new IllegalStateException("Unable to create media analyzer", t);
    }
    
    if(metadata == null) {
      logger.warn("Unable to acquire media metadata for " + url);
      return null;
    } else {
      MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      try {
        MediaPackageElement element = elementBuilder.elementFromURL(url, Type.Track,
                new MediaPackageElementFlavor("track", "presenter"));
        return (Track)element;
      } catch (MediaPackageException e) {
        throw new RuntimeException(e);
      } // FIXME: how should we determine flavor?
    }
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary properties) throws ConfigurationException {
    logger.info("Updating configuration on " + this.getClass().getName());
    // TODO Update the local path to the mediainfo binary
  }

}
