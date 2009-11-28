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

import org.junit.Test;

import java.net.URL;

/**
 * TODO: Add meaningful tests
 */
public class MediaInspectionServiceImplTest {

  @Test
  public void inspectMedia() throws Exception {
    MediaInspectionServiceImpl service = new MediaInspectionServiceImpl();
    URL url = this.getClass().getResource("test-video.mp4");
    // FIXME -- the test movie should always be available
    if(url != null) {
      service.inspect(url.toURI());
    }
  }
}
