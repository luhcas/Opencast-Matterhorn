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
package org.opencastproject.conductor.impl;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * A helpful utility for workflow operation handlers, which need to make copies of mediapackages.
 */
public class MediaPackageUtil {
  public static MediaPackage clone(MediaPackage src) {
    // Generate a new media package for this operation, since we don't want to manipulate the existing media packages
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    src.toXmlStream(out, false);
    try {
      return MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromManifest(
              new ByteArrayInputStream(out.toByteArray()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
