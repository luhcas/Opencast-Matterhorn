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

import java.io.File;
import java.util.Map;

/**
 * Interface for tools that analyze media files.
 * 
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public interface MediaAnalyzer {

  MediaContainerMetadata analyze(File media) throws MediaAnalyzerException;

  /*
   * Needed for https://issues.opencastproject.org/jira/browse/MH-2157 -AZ
   */
  void setConfig(Map<String, Object> config);

}
