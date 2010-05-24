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
 * Converter engine for SubRip srt caption format.
 * 
 */
public class SubRipCaptionConverter implements CaptionConverter {

  private final String LINE_ENDING = "\r\n";

  private final String NAME = "SubRip - Srt";
  private final String ABOUT = "";
  private final String VERSION = "";
  private final String EXTENSION = "srt";
  private final String PATTERN = "([0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}) (-->) ([0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3})";

  /**
   * {@inheritDoc}
   * @throws IllegalCaptionFormatException 
   * 
   * @see org.opencastproject.caption.api.CaptionFormat#importCaption(java.lang.String)
   */
  @Override
  public CaptionCollection importCaption(String in) throws IllegalCaptionFormatException {

    // replace windows style ending (though by specification windows style endings are used)
    in.replaceAll("\r\n", "\n");

    // initialize collection -> TODO export add, getcollection
    CaptionCollection collection = new CaptionCollectionImpl();

    // initialize scanner object
    // accepts stream as well -> define charset?
    Scanner scanner = new Scanner(in);
    scanner.useDelimiter("\n\n");

    while (scanner.hasNext()) {
      String captionString = scanner.next();

      // split to number, time and caption
      String[] captionParts = captionString.split("\n", 3);
      // check for table length
      if (captionParts.length != 3){
        throw new IllegalCaptionFormatException("Invalid caption for SubRip format: " + captionString);
      }
      
      // get time part
      String[] timePart = captionParts[1].split("-->");

      try {
        Time inTime = TimeUtil.importSrt(timePart[0].trim());
        Time outTime = TimeUtil.importSrt(timePart[1].trim());
        // create caption object and add to caption collection
        Caption caption = new CaptionImpl(inTime, outTime, captionParts[2].trim());
        collection.addCaption(caption);
      } catch (IllegalTimeFormatException e) {
        throw new IllegalCaptionFormatException(e.getMessage());
      }

    }

    return collection;
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionFormat#exportCaption(org.opencastproject.caption.api.CaptionCollection)
   */
  @Override
  public String exportCaption(CaptionCollection captionCollection) {

    // initialize string buffer
    StringBuffer buffer = new StringBuffer();
    // get caption collection iterator
    Iterator<Caption> iter = captionCollection.getCollectionIterator();
    // initialize counter
    int counter = 1;
    
    while(iter.hasNext()) {
      Caption caption = iter.next();
      // FIXME line endings in caption string
      String captionString = String.format("%2$d%1$s%3$s --> %4$s%1$s%5$s%1$s%1$s", LINE_ENDING, counter, TimeUtil
              .exportToSrt(caption.getStartTime()), TimeUtil.exportToSrt(caption.getStopTime()), caption.getCaption());
      buffer.append(captionString);
      counter++;
    }

    return buffer.toString();
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionFormat#getName()
   */
  @Override
  public String getName() {
    return NAME;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionFormat#getVersion()
   */
  @Override
  public String getVersion() {
    return VERSION;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionFormat#getAbout()
   */
  @Override
  public String getAbout() {
    return ABOUT;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionFormat#getFileExtension()
   */
  @Override
  public String getFileExtension() {
    return EXTENSION;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionFormat#getIdPattern()
   */
  @Override
  public String getIdPattern() {
    return PATTERN;
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionFormat#allowsTextStyles()
   */
  @Override
  public boolean allowsTextStyles() {
    return false;
  }
}
