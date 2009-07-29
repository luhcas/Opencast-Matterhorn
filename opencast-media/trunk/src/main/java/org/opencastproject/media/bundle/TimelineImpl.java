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

package org.opencastproject.media.bundle;

import org.opencastproject.util.UnknownFileTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerException;

/**
 * Implements the bundle timeline.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class TimelineImpl extends XMLCatalog implements Timeline {

  /** Serial version UID */
  private static final long serialVersionUID = 547207613726299055L;

  /** the logging facility provided by log4j */
  final static Logger log_ = LoggerFactory.getLogger(TimelineImpl.class);

  /**
   * Creates a new timeline from the specified file.
   * 
   * @param file
   *          the timeline file
   * @throws IOException
   *           if reading of the file fails
   * @throws UnknownFileTypeException
   *           if the file is of an unknown filetype
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  private TimelineImpl(File file) throws IOException, UnknownFileTypeException,
      NoSuchAlgorithmException {
    super(Timeline.FLAVOR, file);
  }

  /**
   * Saves the timeline to disk.
   * 
   * @throws ParserConfigurationException
   *           if the xml parser environment is not correctly configured
   * @throws TransformerException
   *           if serialization of the metadata document fails
   * @throws IOException
   *           if an error with catalog file handling occurs
   */
  public void save() throws ParserConfigurationException, TransformerException,
      IOException {
    Document doc = newDocument();

    // Root element "timeline"
    Element timeline = doc.createElement("timeline");
    doc.appendChild(timeline);

    // Save docuement to disk
    saveToXml(doc);
  }

  /**
   * Creates a new timeline file at the specified location for a bundle with the
   * given identifier.
   * 
   * @return the new timeline
   * @throws UnknownFileTypeException
   *           if the timeline file type is unknown (very unlikely)
   * @throws IOException
   *           if creating the timeline file fails
   * @throws TransformerException
   *           if saving the xml file fails
   * @throws ParserConfigurationException
   *           if creating the xml parser fails
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  static TimelineImpl newInstance(Bundle bundle) throws IOException,
      UnknownFileTypeException, ParserConfigurationException,
      TransformerException, NoSuchAlgorithmException {
    TimelineImpl timeline = new TimelineImpl(new File(bundle.getRoot(),
        Timeline.FILENAME));
    timeline.save();
    return timeline;
  }

  /**
   * Creates a new timeline file.
   * 
   * @return the new timeline
   * @throws UnknownFileTypeException
   *           if the timeline file type is unknown (very unlikely)
   * @throws IOException
   *           if creating the timeline file fails
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  public static TimelineImpl newInstance() throws IOException,
      UnknownFileTypeException, NoSuchAlgorithmException {
    String[] name = Timeline.FILENAME.split("\\.");
    File file = File.createTempFile(name[0], "." + name[1]);
    TimelineImpl timeline = new TimelineImpl(file);
    return timeline;
  }

  /**
   * Reads a timeline from the specified file and returns it encapsulated in a
   * {@link Timeline} object.
   * 
   * @param file
   *          the timeline file
   * @return the timeline object
   * @throws IOException
   *           if reading the timeline file fails
   * @throws UnknownFileTypeException
   *           if the timeline file is of an unknown file type
   * @throws ParserConfigurationException
   *           if the timeline parser cannot be created
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   * @throws SAXException
   *           if reading the timeline fails
   */
  public static TimelineImpl fromFile(File file) throws IOException,
      UnknownFileTypeException, ParserConfigurationException,
      NoSuchAlgorithmException, SAXException {
    SAXParserFactory saxFactory = SAXParserFactory.newInstance();
    saxFactory.setValidating(false);
    saxFactory.setNamespaceAware(true);
    SAXParser parser = saxFactory.newSAXParser();
    TimelineImpl timeline = new TimelineImpl(file);
    TimelineSAXHandler timelineSAXHandler = new TimelineSAXHandler(timeline);
    parser.parse(file, timelineSAXHandler);
    return timeline;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "timeline";
  }

  /**
   * Class to parse a timeline.
   * 
   * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
   */
  static class TimelineSAXHandler extends DefaultHandler {

    /** The timeline */
    private TimelineImpl timelineDoc = null;

    /** The element content */
    private StringBuffer content = new StringBuffer();

    /**
     * Creates a new SAX handler for timelines.
     * 
     * @param timeline
     *          the timeline document
     */
    TimelineSAXHandler(TimelineImpl timeline) {
      this.timelineDoc = timeline;
    }

    @Override
    public void characters(char[] ch, int start, int length)
        throws SAXException {
      super.characters(ch, start, length);
      content.append(ch, start, length);
    }

    /**
     * Returns the element content.
     * 
     * @return the element content
     */
    private String getContent() {
      String str = content.toString().trim();
      content = new StringBuffer();
      return str;
    }

    /**
     * Read <code>type</code> attribute from track or catalog element.
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String name,
        Attributes attributes) throws SAXException {
      super.startElement(uri, localName, name, attributes);
      // TODO: Handle xsi:type attributes
      // See http://dublincore.org/documents/dc-xml-guidelines/
    }

    @Override
    public void endElement(String uri, String localName, String name)
        throws SAXException {
      super.endElement(uri, localName, name);
      timelineDoc.addElement(new EName(uri, name), getContent());
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
      log_.warn("Error parsing timeline: " + e.getMessage());
      super.error(e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      log_.warn("Fatal error timeline: " + e.getMessage());
      super.fatalError(e);
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
      log_.warn("Warning while timeline: " + e.getMessage());
      super.warning(e);
    }

  }

}