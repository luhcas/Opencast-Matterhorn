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

package org.opencastproject.media.mediapackage.mpeg7;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Parser implementation for mpeg-7 files. Note that this implementation does by far not cover the full mpeg-7 standard
 * but only deals with those parts relevant to matterhorn, mainly temporal decompositions.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 */
public class Mpeg7Parser extends DefaultHandler {

  /** The current parser state */
  enum ParserState {
    Document, MultimediaContent, Segment
  };

  /** The manifest */
  private Mpeg7CatalogImpl mpeg7Doc = null;

  /** The element content */
  private StringBuffer tagContent = new StringBuffer();

  /** The multimedia content */
  private MultimediaContentType multimediaContent = null;

  /** Current multimedia content type (audio, video, audiovisual) */
  private MultimediaContentType.Type contentType = null;

  /** The multimedia content identifier */
  private String contentId = null;

  /** The content media locator */
  private MediaLocator contentMediaLocator = null;

  /** The content media time and duration */
  private MediaTimeImpl contentMediaTime = null;

  /** The content media time point (will usually refer to 0:00:00) */
  private MediaTimePoint contentTimePoint = null;

  /** The content duration */
  private MediaDuration contentDuration = null;

  /** The segment time point (relative to the content time point) */
  private MediaTimePoint segmentTimePoint = null;

  /** The segment duration */
  private MediaDuration segmentDuration = null;

  /** The segment media time and duration */
  private MediaTime segmentMediaTime = null;

  /** The temporal decomposition container */
  private TemporalDecomposition<?> temporalDecomposition = null;

  /** The temporal segment */
  private ContentSegment segment = null;

  /** The text annoation */
  private TextAnnotation textAnnotation = null;

  /** The current parser state */
  private Mpeg7Parser.ParserState state = ParserState.Document;

  /** Flag to check if this is not just an arbitrary xml document */
  private boolean isMpeg7 = false;

  /**
   * Creates a new parser for mpeg-7 files.
   */
  public Mpeg7Parser() {
  }
  
  public Mpeg7Parser(Mpeg7CatalogImpl catalog) {
    this.mpeg7Doc = catalog;
  }

  /**
   * Parses the mpeg-7 catalog file and returns its object representation.
   * 
   * @param is
   *          the input stream containing the catalog
   * @return the catalog representation
   * @throws ParserConfigurationException
   *           if setting up the parser failed
   * @throws SAXException
   *           if an error occured while parsing the document
   * @throws IOException
   *           if the file cannot be accessed in a proper way
   * @throws IllegalArgumentException
   *           if the provided file does not contain mpeg-7 data
   */
  public Mpeg7CatalogImpl parse(InputStream is) throws ParserConfigurationException, SAXException, IOException {
    if (mpeg7Doc == null)
      mpeg7Doc = new Mpeg7CatalogImpl();
    SAXParserFactory factory = SAXParserFactory.newInstance();
    // REPLAY does not use a DTD here
    factory.setValidating(false);
    factory.setNamespaceAware(true);
    SAXParser parser = factory.newSAXParser();
    parser.parse(is, this);

    // Did we parse an mpeg-7 document?
    if (!isMpeg7)
      throw new IllegalArgumentException("Content of input stream is not mpeg-7");

    return mpeg7Doc;
  }

  /**
   * Read <code>type</code> attribute from track or catalog element.
   * 
   * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String,
   *      org.xml.sax.Attributes)
   */
  @Override
  public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
    super.startElement(uri, localName, name, attributes);
    tagContent = new StringBuffer();

    // Make sure this is an mpeg-7 catalog
    // TODO: Improve this test, add namespace awareness
    if (!isMpeg7 && name.equals("Mpeg7"))
      isMpeg7 = true;

    // Handle parser state
    if ("MultimediaContent".equals(localName)) {
      state = ParserState.MultimediaContent;
    }

    // Content type
    if ("Audio".equals(localName) || "Video".equals(localName) || "AudioVisual".equals(localName)) {
      contentType = MultimediaContentType.Type.valueOf(localName);
      contentId = attributes.getValue("id");
      if (MultimediaContentType.Type.Audio.equals(contentType))
        multimediaContent = mpeg7Doc.addAudioContent(contentId, contentMediaTime, contentMediaLocator);
      else if (MultimediaContentType.Type.Video.equals(contentType))
        multimediaContent = mpeg7Doc.addVideoContent(contentId, contentMediaTime, contentMediaLocator);
      else if (MultimediaContentType.Type.AudioVisual.equals(contentType))
        multimediaContent = mpeg7Doc.addAudioVisualContent(contentId, contentMediaTime, contentMediaLocator);
    }

    // Temporal decomposition
    if ("TemporalDecomposition".equals(localName)) {
      String hasGap = attributes.getValue("gap");
      String isOverlapping = attributes.getValue("overlap");
      String criteria = attributes.getValue("criteria");
      if (!"temporal".equals(criteria))
        throw new IllegalStateException("Decompositions other than temporal are not supported");
      temporalDecomposition = multimediaContent.getTemporalDecomposition();
      temporalDecomposition.setGap("true".equals(hasGap));
      temporalDecomposition.setOverlapping("overlap".equals(isOverlapping));
    }

    // Segment
    if ("AudioSegment".equals(localName) || "VideoSegment".equals(localName) || "AudioVisualSegment".equals(localName)) {
      String segmentId = attributes.getValue("id");
      segment = temporalDecomposition.createSegment(segmentId);
      state = ParserState.Segment;
    }

    // TextAnnotation
    if ("TextAnnotation".equals(localName)) {
      String language = attributes.getValue("xml:lang");
      float confidence = -1.0f;
      float relevance = -1.0f;
      try {
        confidence = Float.parseFloat(attributes.getValue("confidence"));
      } catch (Exception e) {
        confidence = -1.0f;
      }
      try {
        relevance = Float.parseFloat(attributes.getValue("relevance"));
      } catch (Exception e) {
        relevance = -1.0f;
      }
      textAnnotation = segment.createTextAnnotation(confidence, relevance, language);
    }
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public void endElement(String uri, String localName, String name) throws SAXException {
    super.endElement(uri, localName, name);

    // Handle parser state
    if ("MultimediaContent".equals(localName))
      state = ParserState.Document;
    else if ("AudioSegment".equals(localName) || "VideoSegment".equals(localName)
            || "AudioVisualSegment".equals(localName))
      state = ParserState.MultimediaContent;

    // Media locator uri
    if ("MediaUri".equals(localName)) {
      MediaLocatorImpl locator = new MediaLocatorImpl();
      URI mediaUri = URI.create(getTagContent());
      locator.setMediaURI(mediaUri);
      if (ParserState.MultimediaContent.equals(state)) {
        multimediaContent.setMediaLocator(locator);
      }
    }

    // Media/Segment time
    if ("MediaTime".equals(localName)) {
      if (ParserState.MultimediaContent.equals(state)) {
        contentMediaTime = new MediaTimeImpl(contentTimePoint, contentDuration);
        multimediaContent.setMediaTime(contentMediaTime);
      } else if (ParserState.Segment.equals(state)) {
        segmentMediaTime = new MediaTimeImpl(segmentTimePoint, segmentDuration);
        segment.setMediaTime(segmentMediaTime);
      }
    }

    // Media/Segment time point
    if ("MediaTimePoint".equals(localName)) {
      MediaTimePointImpl tp = MediaTimePointImpl.parseTimePoint(getTagContent());
      if (ParserState.MultimediaContent.equals(state))
        contentTimePoint = tp;
      else if (ParserState.Segment.equals(state)) {
        segmentTimePoint = tp;
      }
    }

    // Media/Segment time point
    if ("MediaRelTimePoint".equals(localName)) {
      MediaTimePointImpl tp = MediaTimePointImpl.parseTimePoint(getTagContent());
      if (ParserState.MultimediaContent.equals(state))
        contentTimePoint = tp;
      else if (ParserState.Segment.equals(state)) {
        segmentTimePoint = tp;
        tp.setReferenceTimePoint(contentTimePoint);
      }
    }

    // Media/Segment duration
    if ("MediaDuration".equals(localName)) {
      MediaDuration td = MediaDurationImpl.parseDuration(getTagContent());
      if (ParserState.MultimediaContent.equals(state))
        contentDuration = td;
      else if (ParserState.Segment.equals(state))
        segmentDuration = td;
    }

    // Keyword
    if ("Keyword".equals(localName)) {
      KeywordAnnotation keyword = new KeywordAnnotationImpl(tagContent.toString());
      textAnnotation.addKeywordAnnotation(keyword);
    }

    // Free text
    if ("FreeTextAnnotation".equals(localName)) {
      FreeTextAnnotation freeText = new FreeTextAnnotationImpl(tagContent.toString());
      textAnnotation.addFreeTextAnnotation(freeText);
    }

  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
   */
  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    super.characters(ch, start, length);
    tagContent.append(ch, start, length);
  }

  /**
   * Returns the element content.
   * 
   * @return the element content
   */
  private String getTagContent() {
    String str = tagContent.toString().trim();
    return str;
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
   */
  @Override
  public void error(SAXParseException e) throws SAXException {
    Mpeg7CatalogImpl.log_.warn("Error while parsing mpeg-7 catalog: " + e.getMessage());
    super.error(e);
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
   */
  @Override
  public void fatalError(SAXParseException e) throws SAXException {
    Mpeg7CatalogImpl.log_.warn("Fatal error while parsing mpeg-7 catalog: " + e.getMessage());
    super.fatalError(e);
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException)
   */
  @Override
  public void warning(SAXParseException e) throws SAXException {
    Mpeg7CatalogImpl.log_.warn("Warning while parsing mpeg-7 catalog: " + e.getMessage());
    super.warning(e);
  }

}
