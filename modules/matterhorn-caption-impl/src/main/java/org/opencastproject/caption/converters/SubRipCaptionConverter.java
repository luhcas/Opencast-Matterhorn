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
package org.opencastproject.caption.converters;

import org.opencastproject.caption.api.Caption;
import org.opencastproject.caption.api.CaptionCollection;
import org.opencastproject.caption.api.CaptionConverter;
import org.opencastproject.caption.api.IllegalCaptionFormatException;
import org.opencastproject.caption.api.IllegalTimeFormatException;
import org.opencastproject.caption.api.Time;
import org.opencastproject.caption.impl.CaptionCollectionImpl;
import org.opencastproject.caption.impl.CaptionImpl;
import org.opencastproject.caption.util.TimeUtil;

import java.util.Iterator;
import java.util.Scanner;

/**
 * Converter engine for SubRip srt caption format. It does not support advanced SubRip format (SubRip format with
 * annotations). Advanced format will be parsed but all annotations will be stripped off.
 * 
 */
public class SubRipCaptionConverter implements CaptionConverter {

  private final String LINE_ENDING = "\r\n";

  private final String NAME = "SubRip - Srt";
  private final String EXTENSION = "srt";
  private final String PATTERN = "([0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}) (-->) ([0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3})";

  /**
   * {@inheritDoc}
   * 
   * @throws IllegalCaptionFormatException
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#importCaption(java.lang.String)
   */
  @Override
  public CaptionCollection importCaption(String in) throws IllegalCaptionFormatException {

    CaptionCollection collection = new CaptionCollectionImpl();

    // initialize scanner object
    Scanner scanner = new Scanner(in);
    scanner.useDelimiter("[\n(\r\n)]{2}");

    while (scanner.hasNext()) {
      String captionString = scanner.next();
      // convert line endings to \n
      captionString = captionString.replace("\r\n", "\n");

      // split to number, time and caption
      String[] captionParts = captionString.split("\n", 3);
      // check for table length
      if (captionParts.length != 3) {
        throw new IllegalCaptionFormatException("Invalid caption for SubRip format: " + captionString);
      }

      // get time part
      String[] timePart = captionParts[1].split("-->");

      // parse time
      Time inTime;
      Time outTime;
      try {
        inTime = TimeUtil.importSrt(timePart[0].trim());
        outTime = TimeUtil.importSrt(timePart[1].trim());
      } catch (IllegalTimeFormatException e) {
        throw new IllegalCaptionFormatException(e.getMessage());
      }

      // get text captions -- is it possible to get null?
      String[] captionLines = createCaptionLines(captionParts[2]);
      if (captionLines == null) {
        throw new IllegalCaptionFormatException("Caption does not contain any caption text: " + captionString);
      }

      // create caption object and add to caption collection
      Caption caption = new CaptionImpl(inTime, outTime, captionLines);
      collection.addCaption(caption);
    }

    return collection;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#exportCaption(org.opencastproject.caption.api.CaptionCollection)
   */
  @Override
  public String exportCaption(CaptionCollection captionCollection) {

    // initialize string buffer
    StringBuffer buffer = new StringBuffer();
    // get caption collection iterator
    Iterator<Caption> iter = captionCollection.getCollectionIterator();
    // initialize counter
    int counter = 1;

    while (iter.hasNext()) {
      Caption caption = iter.next();
      String captionString = String.format("%2$d%1$s%3$s --> %4$s%1$s%5$s%1$s%1$s", LINE_ENDING, counter, TimeUtil
              .exportToSrt(caption.getStartTime()), TimeUtil.exportToSrt(caption.getStopTime()),
              createCaptionText(caption.getCaption()));
      buffer.append(captionString);
      counter++;
    }

    return buffer.toString();
  }

  /**
   * Helper function that creates caption text String from array of lines.
   * 
   * @param captionLines
   * @return
   */
  private String createCaptionText(String[] captionLines) {
    StringBuilder builder = new StringBuilder(captionLines[0]);
    for (int i = 1; i < captionLines.length; i++) {
      builder.append(LINE_ENDING);
      builder.append(captionLines[i]);
    }
    return builder.toString();
  }

  /**
   * Helper function that splits text into lines and remove any style annotation
   * 
   * @param captionText
   * @return
   */
  private String[] createCaptionLines(String captionText) {
    String[] captionLines = captionText.split("\n");
    if (captionLines.length == 0) {
      return null;
    }
    for (int i = 0; i < captionLines.length; i++) {
      captionLines[i] = captionLines[i].replaceAll("(<\\s*.\\s*>)|(</\\s*.\\s*>)", "").trim();
    }
    return captionLines;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#getName()
   */
  @Override
  public String getName() {
    return NAME;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#getFileExtension()
   */
  @Override
  public String getFileExtension() {
    return EXTENSION;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#getIdPattern()
   */
  @Override
  public String getIdPattern() {
    return PATTERN;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#allowsTextStyles()
   */
  @Override
  public boolean allowsTextStyles() {
    return false;
  }
}
