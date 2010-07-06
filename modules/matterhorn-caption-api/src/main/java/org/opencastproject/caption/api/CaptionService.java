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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Provides captioning support. This service makes use of {@link CaptionConverter} instances that need to be registered
 * in the OSGi registry.
 */
public interface CaptionService {

  /**
   * Converts captions from one format to another. Language parameter is used for those formats that store information
   * about language.
   * 
   * @param input
   *          stream from where captions are read
   * @param inputFormat
   *          format of imported captions
   * @param output
   *          stream to where captions are written
   * @param outputFormat
   *          format of exported captions
   * @param language
   *          (optional) language of captions
   * @throws UnsupportedCaptionFormatException
   *           if there is no matching engine registered for given input or output
   * @throws IllegalCaptionFormatException
   *           if importing captions fails
   * @throws IOException
   *           if exception occurs during exporting captions
   */
  void convert(InputStream input, String inputFormat, OutputStream output, String outputFormat, String language)
          throws UnsupportedCaptionFormatException, IllegalCaptionFormatException, IOException;

  /**
   * Returns list of languages available in captions (if such information is stored).
   * 
   * @param input
   *          stream from where captions are read
   * @param format
   *          captions' format
   * @return {@link List} of languages available in captions
   * @throws UnsupportedCaptionFormatException
   *           if there is no matching engine registered for given input or output
   * @throws IllegalCaptionFormatException
   *           if parser encounters exception
   */
  List<String> getLanguageList(InputStream input, String format) throws UnsupportedCaptionFormatException,
          IllegalCaptionFormatException;
}
