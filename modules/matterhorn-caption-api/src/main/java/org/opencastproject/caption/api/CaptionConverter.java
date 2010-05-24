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
package org.opencastproject.caption.api;

/**
 * Imports captions to {@link CaptionCollection} and exports from {@link CaptionCollection} to String presentation.
 */
public interface CaptionConverter {
  /**
   * Parse captions to the CaptionCollection. If exception is encountered during input parsing
   * IllegalCaptionFormatException is thrown.
   * 
   * @param in
   *          String representation of captions
   * @return CaptionCollection parsed captions
   * @throws IllegalCaptionFormatException
   *           if exception occurred while parsing captions
   */
  CaptionCollection importCaption(String in) throws IllegalCaptionFormatException;

  /**
   * Export caption collection to string representation.
   * 
   * @param {@link CaptionCollection} to be exported
   * @return string representation of the collection
   */
  String exportCaption(CaptionCollection captionCollection);

  // FIXME -> are they needed
  String getName();

  String getAbout();

  String getVersion();

  String getFileExtension();

  String getIdPattern();

  boolean allowsTextStyles();
}
