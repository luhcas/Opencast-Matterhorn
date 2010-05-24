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
 * Provides captioning support. This service makes use of {@link CaptionConverter} instances that need to be registered
 * in the OSGi registry.
 */
public interface CaptionService {

  /**
   * Converts the captions from the input format to the output format. If support for either of these two formats is
   * missing, an {@link UnsupportedCaptionFormatException} is thrown.
   * 
   * @param input
   *          input string
   * @param inputType
   *          caption format of the input
   * @param outputType
   *          caption format to output
   * @return the converted captions
   * @throws UnsupportedCaptionFormatException
   *           if support for either input or output format is missing
   */
  String convert(String input, String inputFormat, String outputFormat) throws UnsupportedCaptionFormatException,
          IllegalCaptionFormatException;

  /**
   * Converts the captions from the input format to the output format. Input format is determined based on the captions'
   * format. If format cannot be determined or support for output formats is missing, an
   * {@link UnsupportedCaptionFormatException} is thrown.
   * 
   * @param input
   *          input string
   * @param outputType
   *          caption format to output
   * @return converted captions
   * @throws UnsupportedCaptionFormatException
   *           if input format cannot be determined or support for output format is missing
   */
  String convert(String input, String outputFormat) throws UnsupportedCaptionFormatException,
          IllegalCaptionFormatException;
}
