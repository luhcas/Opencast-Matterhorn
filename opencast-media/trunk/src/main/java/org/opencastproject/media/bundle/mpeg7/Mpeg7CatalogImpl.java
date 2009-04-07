/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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

package org.opencastproject.media.bundle.mpeg7;

import org.opencastproject.media.bundle.Bundle;
import org.opencastproject.media.bundle.Mpeg7Catalog;
import org.opencastproject.media.bundle.XMLCatalog;
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
import java.io.OutputStream;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerException;

/**
 * Implements the mpeg-7 metadata container.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class Mpeg7CatalogImpl extends XMLCatalog implements Mpeg7Catalog {

  /** Serial version UID */
  private static final long serialVersionUID = 5521535164920498997L;

  /** The multimedia content list */
  private Map<MultimediaContent.Type, MultimediaContentImpl<? extends MultimediaContentType>> multimediaContent = null;

  /** The default element namespace */
  public static final String NS = "mpeg7";

  /** the logging facility provided by log4j */
  final static Logger log_ = LoggerFactory.getLogger(Mpeg7CatalogImpl.class);

  /**
   * Creates a new mpeg-7 metadata container from the specified file.
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
  private Mpeg7CatalogImpl(File file) throws IOException,
      UnknownFileTypeException, NoSuchAlgorithmException {
    super(Mpeg7Catalog.FLAVOR, file);
    multimediaContent = new HashMap<MultimediaContent.Type, MultimediaContentImpl<? extends MultimediaContentType>>();
  }

  /**
   * Reads the metadata from the specified file and returns it encapsulated in a
   * {@link Mpeg7Catalog} object.
   * 
   * @param catalog
   *          the dublin core metadata container file
   * @return the dublin core object
   * @throws IOException
   *           if reading the metadata fails
   * @throws UnknownFileTypeException
   *           if the dublin core file is of an unknown file type
   * @throws ParserConfigurationException
   *           if the dublin core parser cannot be created
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   * @throws SAXException
   *           if reading the catalog fails
   */
  public static Mpeg7Catalog fromFile(File catalog) throws IOException,
      UnknownFileTypeException, ParserConfigurationException,
      NoSuchAlgorithmException, SAXException {
    MPEG7Parser parser = new MPEG7Parser();
    Mpeg7Catalog doc = parser.parse(catalog);
    return doc;
  }

  /**
   * Creates a new mpeg-7 metadata catalog for the given bundle.
   * 
   * @param bundle
   *          the bundle
   * @return the new mpeg-7 metadata container
   * @throws UnknownFileTypeException
   *           if the mpeg-7 file type is unknown (very unlikely)
   * @throws IOException
   *           if creating the mpeg-7 file fails
   * @throws TransformerException
   *           if saving the mpeg-7 file fails
   * @throws ParserConfigurationException
   *           if creating the xml parser fails
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  static Mpeg7CatalogImpl newInstance(Bundle bundle) throws IOException,
      UnknownFileTypeException, ParserConfigurationException,
      TransformerException, NoSuchAlgorithmException {
    Mpeg7CatalogImpl mpeg7 = new Mpeg7CatalogImpl(new File(bundle.getRoot(),
        Mpeg7Catalog.FILENAME));
    mpeg7.save();
    return mpeg7;
  }

  /**
   * Creates a new mpeg-7 metadata container file.
   * 
   * @return the new mpeg-7 metadata container
   * @throws UnknownFileTypeException
   *           if the mpeg-7 file type is unknown (very unlikely)
   * @throws IOException
   *           if creating the mpeg-7 file fails
   * @throws NoSuchAlgorithmException
   *           if the md5 checksum cannot be computed
   */
  public static Mpeg7CatalogImpl newInstance() throws IOException,
      UnknownFileTypeException, NoSuchAlgorithmException {
    String[] name = Mpeg7Catalog.FILENAME.split("\\.");
    File file = File.createTempFile(name[0], "." + name[1]);
    Mpeg7CatalogImpl mpeg7 = new Mpeg7CatalogImpl(file);
    return mpeg7;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#multimediaContent()
   */
  public Iterator<MultimediaContent<? extends MultimediaContentType>> multimediaContent() {
    List<MultimediaContent<? extends MultimediaContentType>> result = new ArrayList<MultimediaContent<? extends MultimediaContentType>>();
    for (MultimediaContent<? extends MultimediaContentType> o : multimediaContent
        .values()) {
      result.add(o);
    }
    return result.iterator();
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#getMultimediaContent(org.opencastproject.media.bundle.mpeg7.MultimediaContent.Type)
   */
  public MultimediaContent<? extends MultimediaContentType> getMultimediaContent(
      MultimediaContent.Type type) {
    return multimediaContent.get(type);
  }

  /**
   * Saves the mpeg-7 metadata container to disk.
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
    Document doc = createDocument();

    // Root element
    Element root = doc.getDocumentElement();

    // Description
    Element descriptionNode = doc.createElement("Description");
    descriptionNode.setAttribute("xsi:type", "ContentEntityType");
    root.appendChild(descriptionNode);

    // MultimediaContent
    for (MultimediaContent<? extends MultimediaContentType> mc : multimediaContent
        .values()) {
      descriptionNode.appendChild(mc.toXml(doc));
    }

    // Save document to disk
    saveToXml(doc);
  }

  /**
   * Outputs the mpeg-7 metadata container to the given output stream.
   * 
   * @param out
   *          the output stream
   * @throws ParserConfigurationException
   *           if the xml parser environment is not correctly configured
   * @throws TransformerException
   *           if serialization of the metadata document fails
   * @throws IOException
   *           if an error with catalog file handling occurs
   */
  public void save(OutputStream out) throws ParserConfigurationException,
      TransformerException, IOException {
    saveToXml(createDocument(), out);
  }

  /**
   * Create a DOM representation of the DublinCore.
   */
  private Document createDocument() throws ParserConfigurationException {
    Document doc = newDocument();
    Element rootElement = doc.createElementNS("urn:mpeg:mpeg7:schema:2001",
        "Mpeg7");
    rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:mpeg7",
        "urn:mpeg7:schema:2001");
    rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi",
        "http://www.w3.org/2001/XMLSchema-instance/");
    doc.appendChild(rootElement);
    return doc;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "MPEG-7";
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#addAudioContent(java.lang.String,
   *      org.opencastproject.media.bundle.mpeg7.MediaTime,
   *      org.opencastproject.media.bundle.mpeg7.MediaLocator)
   */
  @SuppressWarnings("unchecked")
  public Audio addAudioContent(String id, MediaTime time, MediaLocator locator) {
    MultimediaContentImpl<Audio> content = (MultimediaContentImpl<Audio>) getMultimediaContent(MultimediaContent.Type.AudioType);
    if (content == null) {
      content = new MultimediaContentImpl<Audio>(
          MultimediaContent.Type.AudioType);
      multimediaContent.put(MultimediaContent.Type.AudioType, content);
    }
    MultimediaContentTypeImpl audio = new MultimediaContentTypeImpl(
        MultimediaContentType.Type.Audio, id);
    audio.setMediaTime(time);
    audio.setMediaLocator(locator);
    content.add(audio);
    return audio;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#removeAudioContent(java.lang.String)
   */
  public Audio removeAudioContent(String id) {
    MultimediaContentType element = removeContentElement(id,
        MultimediaContent.Type.AudioType);
    if (element != null)
      return (Audio) element;
    return null;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#hasAudioContent()
   */
  public boolean hasAudioContent() {
    MultimediaContent<?> content = getMultimediaContent(MultimediaContent.Type.AudioType);
    return content != null && content.size() > 0;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#audioContent()
   */
  @SuppressWarnings("unchecked")
  public Iterator<Audio> audioContent() {
    MultimediaContent<Audio> content = (MultimediaContent<Audio>) getMultimediaContent(MultimediaContent.Type.AudioType);
    if (content != null)
      return content.elements();
    return null;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#addVideoContent(java.lang.String,
   *      org.opencastproject.media.bundle.mpeg7.MediaTime,
   *      org.opencastproject.media.bundle.mpeg7.MediaLocator)
   */
  @SuppressWarnings("unchecked")
  public Video addVideoContent(String id, MediaTime time, MediaLocator locator) {
    MultimediaContentImpl<Video> content = (MultimediaContentImpl<Video>) getMultimediaContent(MultimediaContent.Type.VideoType);
    if (content == null) {
      content = new MultimediaContentImpl<Video>(
          MultimediaContent.Type.VideoType);
      multimediaContent.put(MultimediaContent.Type.VideoType, content);
    }
    MultimediaContentTypeImpl video = new MultimediaContentTypeImpl(
        MultimediaContentType.Type.Video, id);
    content.add(video);
    video.setMediaTime(time);
    video.setMediaLocator(locator);
    return video;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#removeVideoContent(java.lang.String)
   */
  public Video removeVideoContent(String id) {
    MultimediaContentType element = removeContentElement(id,
        MultimediaContent.Type.VideoType);
    if (element != null)
      return (Video) element;
    return null;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#hasVideoContent()
   */
  public boolean hasVideoContent() {
    MultimediaContent<?> content = getMultimediaContent(MultimediaContent.Type.VideoType);
    return content != null && content.size() > 0;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#videoContent()
   */
  @SuppressWarnings("unchecked")
  public Iterator<Video> videoContent() {
    MultimediaContent<Video> content = (MultimediaContent<Video>) getMultimediaContent(MultimediaContent.Type.VideoType);
    if (content != null)
      return content.elements();
    return null;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#addAudioVisualContent(java.lang.String,
   *      org.opencastproject.media.bundle.mpeg7.MediaTime,
   *      org.opencastproject.media.bundle.mpeg7.MediaLocator)
   */
  @SuppressWarnings("unchecked")
  public AudioVisual addAudioVisualContent(String id, MediaTime time,
      MediaLocator locator) {
    MultimediaContentImpl<AudioVisual> content = (MultimediaContentImpl<AudioVisual>) getMultimediaContent(MultimediaContent.Type.AudioVisualType);
    if (content == null) {
      content = new MultimediaContentImpl<AudioVisual>(
          MultimediaContent.Type.AudioVisualType);
      multimediaContent.put(MultimediaContent.Type.AudioVisualType, content);
    }
    MultimediaContentTypeImpl audioVisual = new MultimediaContentTypeImpl(
        MultimediaContentType.Type.AudioVisual, id);
    audioVisual.setMediaTime(time);
    audioVisual.setMediaLocator(locator);
    content.add(audioVisual);
    return audioVisual;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#removeAudioVisualContent(java.lang.String)
   */
  public AudioVisual removeAudioVisualContent(String id) {
    MultimediaContentType element = removeContentElement(id,
        MultimediaContent.Type.AudioVisualType);
    if (element != null)
      return (AudioVisual) element;
    return null;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#hasAudioVisualContent()
   */
  public boolean hasAudioVisualContent() {
    MultimediaContent<?> content = getMultimediaContent(MultimediaContent.Type.AudioVisualType);
    return content != null && content.size() > 0;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#audiovisualContent()
   */
  @SuppressWarnings("unchecked")
  public Iterator<AudioVisual> audiovisualContent() {
    MultimediaContent<AudioVisual> content = (MultimediaContent<AudioVisual>) getMultimediaContent(MultimediaContent.Type.AudioVisualType);
    if (content != null)
      return content.elements();
    return null;
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#getAudioById(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  public Audio getAudioById(String id) {
    MultimediaContent<Audio> content = (MultimediaContent<Audio>) getMultimediaContent(MultimediaContent.Type.AudioType);
    if (content == null)
      return null;
    return content.getElementById(id);
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#getAudioVisualById(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  public AudioVisual getAudioVisualById(String id) {
    MultimediaContent<AudioVisual> content = (MultimediaContent<AudioVisual>) getMultimediaContent(MultimediaContent.Type.AudioVisualType);
    if (content == null)
      return null;
    return content.getElementById(id);
  }

  /**
   * @see org.opencastproject.media.bundle.mpeg7.Mpeg7#getVideoById(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  public Video getVideoById(String id) {
    MultimediaContent<Video> content = (MultimediaContent<Video>) getMultimediaContent(MultimediaContent.Type.VideoType);
    if (content == null)
      return null;
    return content.getElementById(id);
  }

  /**
   * Removes the content element of the specified type with the given
   * identifier.
   * 
   * @param id
   *          the content element identifier
   * @param type
   *          the content type
   * @return the element or <code>null</code>
   */
  private MultimediaContentType removeContentElement(String id,
      MultimediaContent.Type type) {
    MultimediaContentImpl<? extends MultimediaContentType> content = multimediaContent
        .get(type);
    if (content != null)
      return content.remove(id);
    return null;
  }

  /**
   * Parser implementation for mpeg-7 files. Note that this implementation does
   * by far not cover the full mpeg-7 standard but only deals with those parts
   * relevant to replay, mainly temporal decompositions.
   * 
   * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
   */
  static class MPEG7Parser extends DefaultHandler {

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
    private ParserState state = ParserState.Document;

    /** Flag to check if this is not just an arbitrary xml document */
    private boolean isMpeg7 = false;

    /**
     * Creates a new parser for mpeg-7 files.
     */
    MPEG7Parser() {
    }

    /**
     * Parses the mpeg-7 catalog file and returns its object representation.
     * 
     * @param file
     *          the file containing the catalog
     * @return the catalog representation
     * @throws ParserConfigurationException
     *           if setting up the parser failed
     * @throws SAXException
     *           if an error occured while parsing the document
     * @throws IOException
     *           if the file cannot be accessed in a proper way
     * @throws UnknownFileTypeException
     *           if the catalog file type is unknown
     * @throws NoSuchAlgorithmException
     *           if the checksum cannot be calculated
     * @throws IllegalArgumentException
     *           if the provided file does not contain mpeg-7 data
     */
    public Mpeg7Catalog parse(File file) throws ParserConfigurationException,
        SAXException, IOException, NoSuchAlgorithmException,
        UnknownFileTypeException {
      mpeg7Doc = new Mpeg7CatalogImpl(file);
      SAXParserFactory factory = SAXParserFactory.newInstance();
      // REPLAY does not use a DTD here
      factory.setValidating(false);
      factory.setNamespaceAware(true);
      SAXParser parser = factory.newSAXParser();
      parser.parse(file, this);

      // Did we parse an mpeg-7 document?
      if (!isMpeg7)
        throw new IllegalArgumentException(file + " is no mpeg-7 document");

      return mpeg7Doc;
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
      if ("Audio".equals(localName) || "Video".equals(localName)
          || "AudioVisual".equals(localName)) {
        contentType = MultimediaContentType.Type.valueOf(localName);
        contentId = attributes.getValue("id");
        if (MultimediaContentType.Type.Audio.equals(contentType))
          multimediaContent = mpeg7Doc.addAudioContent(contentId,
              contentMediaTime, contentMediaLocator);
        else if (MultimediaContentType.Type.Video.equals(contentType))
          multimediaContent = mpeg7Doc.addVideoContent(contentId,
              contentMediaTime, contentMediaLocator);
        else if (MultimediaContentType.Type.AudioVisual.equals(contentType))
          multimediaContent = mpeg7Doc.addAudioVisualContent(contentId,
              contentMediaTime, contentMediaLocator);
      }

      // Temporal decomposition
      if ("TemporalDecomposition".equals(localName)) {
        String hasGap = attributes.getValue("gap");
        String isOverlapping = attributes.getValue("overlap");
        String criteria = attributes.getValue("criteria");
        if (!"temporal".equals(criteria))
          throw new IllegalStateException(
              "Decompositions other than temporal are not supported");
        temporalDecomposition = multimediaContent.getTemporalDecomposition();
        temporalDecomposition.setGap("true".equals(hasGap));
        temporalDecomposition.setOverlapping("overlap".equals(isOverlapping));
      }

      // Segment
      if ("AudioSegment".equals(localName) || "VideoSegment".equals(localName)
          || "AudioVisualSegment".equals(localName)) {
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
        textAnnotation = segment.createTextAnnotation(confidence, relevance,
            language);
      }
    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String uri, String localName, String name)
        throws SAXException {
      super.endElement(uri, localName, name);

      // Handle parser state
      if ("MultimediaContent".equals(localName))
        state = ParserState.Document;
      else if ("AudioSegment".equals(localName)
          || "VideoSegment".equals(localName)
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
          contentMediaTime = new MediaTimeImpl(contentTimePoint,
              contentDuration);
          multimediaContent.setMediaTime(contentMediaTime);
        } else if (ParserState.Segment.equals(state)) {
          segmentMediaTime = new MediaTimeImpl(segmentTimePoint,
              segmentDuration);
          segment.setMediaTime(segmentMediaTime);
        }
      }

      // Media/Segment time point
      if ("MediaTimePoint".equals(localName)) {
        MediaTimePointImpl tp = MediaTimePointImpl
            .parseTimePoint(getTagContent());
        if (ParserState.MultimediaContent.equals(state))
          contentTimePoint = tp;
        else if (ParserState.Segment.equals(state)) {
          segmentTimePoint = tp;
        }
      }

      // Media/Segment time point
      if ("MediaRelTimePoint".equals(localName)) {
        MediaTimePointImpl tp = MediaTimePointImpl
            .parseTimePoint(getTagContent());
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
        KeywordAnnotation keyword = new KeywordAnnotationImpl(tagContent
            .toString());
        textAnnotation.addKeywordAnnotation(keyword);
      }

      // Free text
      if ("FreeTextAnnotation".equals(localName)) {
        FreeTextAnnotation freeText = new FreeTextAnnotationImpl(tagContent
            .toString());
        textAnnotation.addFreeTextAnnotation(freeText);
      }

    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char[] ch, int start, int length)
        throws SAXException {
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
      log_.warn("Error while parsing mpeg-7 catalog: " + e.getMessage());
      super.error(e);
    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      log_.warn("Fatal error while parsing mpeg-7 catalog: " + e.getMessage());
      super.fatalError(e);
    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(SAXParseException e) throws SAXException {
      log_.warn("Warning while parsing mpeg-7 catalog: " + e.getMessage());
      super.warning(e);
    }

  }

}