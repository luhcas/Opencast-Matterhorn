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
package org.opencastproject.analysis.text.ocropus;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents an ocropus output frame that holds a number of words found on an image along with several
 * formatting information for priority calculation.
 */
public class OcropusTextFrame {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(OcropusTextFrame.class);

  /** Words found on an output frame */
  protected ArrayList<OcropusWord> words = new ArrayList<OcropusWord>();

  /**
   * Parses the ocropus output file and extracts the text information contained therein.
   * 
   * TODO:
   * This implementation right now only extracts the line itself, but there is more information available from
   * the ocr processor that might be of value.
   * 
   * @param is
   *          the input stream
   * @return the ocropus text information
   * @throws IOException
   *           if reading the ocropus output fails
   */
  public static OcropusTextFrame parse(InputStream is) throws IOException {

    String ocropusFrame = new String(IOUtils.toByteArray(is));

    OcropusTextFrame textFrame = new OcropusTextFrame();
    Pattern linePattern = Pattern.compile("<span class='ocr_line' title='bbox ([\\s|0-9]*)'>([^<]*)</span>");
    Matcher m = linePattern.matcher(ocropusFrame);
    while (m.find()) {
      String bbox = m.group(1);
      String line = StringEscapeUtils.unescapeXml(m.group(2));

      String[] values = bbox.split(" ");
      if (values.length != 4) {
        logger.warn("Found unexpected format for ocropus text box format: '{}'", bbox);
        continue;
      }

      // Note: this box is per line (instead of per word). 
      Rectangle textBoundaries = new Rectangle(
        Integer.parseInt(values[0]), 
        Integer.parseInt(values[1]),
        Integer.parseInt(values[2]) - Integer.parseInt(values[0]),
        Integer.parseInt(values[3]) - Integer.parseInt(values[1])
      );

      // for every word:
      for (String word : line.split(" ")) {

        // remove beginning and tailing whitespace and punctuation from every word
        word = word.replaceAll("^[\\W]*|[\\W]*$", "");

        // if word is not empty string
        if (!word.equals("")) {

          // add the word an the hints to the item list
          OcropusWord item = new OcropusWord(word, textBoundaries);

          textFrame.words.add(item);
        }
      }
    }

    return textFrame;
  }
  
  /**
   * Returns <code>true</code> if text was found.
   * 
   * @return <code>true</code> if there is text
   */
  public boolean hasText() {
    return words.size() > 0;
  }
  
  /**
   * Returns the words found on the frame or an empty array if no words have been found at all.
   * 
   * @return the words
   */
  public OcropusWord[] getWords() {
    return words.toArray(new OcropusWord[words.size()]);
  }

}
