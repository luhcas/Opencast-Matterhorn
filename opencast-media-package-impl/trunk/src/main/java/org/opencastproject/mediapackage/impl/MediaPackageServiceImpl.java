/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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
package org.opencastproject.mediapackage.impl;

import org.opencastproject.mediapackage.api.MediaPackage;
import org.opencastproject.mediapackage.api.MediaPackageList;
import org.opencastproject.mediapackage.api.MediaPackageService;
import org.opencastproject.mediapackage.api.Track;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class MediaPackageServiceImpl implements MediaPackageService {
  private static final Logger logger = LoggerFactory.getLogger(MediaPackageServiceImpl.class);
  
  public MediaPackage getMediaPackage(String handle) {
    return generateMediaPackage("sample");
  }

  public MediaPackageList getMediaPackages() {
    return generateMediaPackages();
  }

  protected static String dummy_url_prefix = "http://localhost:8080/tracks/";

  protected MediaPackage generateMediaPackage(String id) {
    MediaPackage mp = new MediaPackage();
    mp.setId(id);
    List<Track> tracks = new ArrayList<Track>();
    tracks.add(new Track(dummy_url_prefix + id + "/track1"));
    tracks.add(new Track(dummy_url_prefix + id + "/track2"));
    mp.setTracks(tracks);
    return mp;
  }

  public MediaPackageList generateMediaPackages() {
    List<MediaPackage> mps = new ArrayList<MediaPackage>();
    mps.add(generateMediaPackage("listsample1"));
    mps.add(generateMediaPackage("listsample2"));
    mps.add(generateMediaPackage("listsample3"));
    return new MediaPackageList(mps);
  }

  public String getDocumentation() {
    Class<?> clazz = getClass();
    String htmlDoc = "/docs/" + clazz.getSimpleName() + ".html";
    InputStream in = clazz.getResourceAsStream(htmlDoc);
    logger.debug("Found documentation " + in + " at " + htmlDoc);
    StringWriter writer = new StringWriter();
    try {
      IOUtils.copy(in, writer);
      IOUtils.closeQuietly(in);
      return writer.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
