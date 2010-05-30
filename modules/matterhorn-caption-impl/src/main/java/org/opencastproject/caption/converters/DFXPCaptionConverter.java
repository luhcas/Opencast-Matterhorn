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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Converter engine for DFXP caption format. Uses SAX parser to parse DFXP document and DOM parser to construct DFXP
 * document. Currently style information is not parsed.
 * 
 */
public class DFXPCaptionConverter implements CaptionConverter {

  private final String NAME = "Timed Text (TT) Authoring Format 1.0 - Distribution Format Exchange Profile (DFXP)";
  private final String EXTENSION = "dfxp.xml";
  private final String PATTERN = "xmlns=\"http://www\\.w3\\.org/2006/(10|04)/ttaf1";

  // template for DFXP caption format
  // includes two default style definitions and empty div tag where captions will be added
  // TODO instead of hardcoded template maybe there should be external file with it?
  private final String TEMPLATE =  
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    + "<tt xmlns=\"http://www.w3.org/2006/04/ttaf1\""
    + "  xmlns:tts=\"http://www.w3.org/2006/04/ttaf1#styling\" xml:lang=\"en\">"
    + "  <head>" 
    + "    <styling>"
    + "      <style id=\"defaultSpeaker\" tts:fontSize=\"12\" tts:fontFamily=\"SansSerif\" "
    + "        tts:fontWeight=\"normal\" tts:fontStyle=\"normal\" tts:textDecoration=\"none\" "
    + "        tts:color=\"white\" tts:backgroundColor=\"black\" tts:textAlign=\"left\" />"
    + "      <style id=\"defaultCaption\" tts:fontSize=\"12\" tts:fontFamily=\"SansSerif\" "
    + "        tts:fontWeight=\"normal\" tts:fontStyle=\"normal\" tts:textDecoration=\"none\" "
    + "        tts:color=\"white\" tts:backgroundColor=\"black\" tts:textAlign=\"left\" />"
    + "    </styling>" 
    + "  </head>"
    + "  <body id=\"ccbody\" style=\"defaultCaption\">"
    + "    <div xml:lang=\"en\">"
    + "    </div>"
    + "  </body>"
    + "</tt>";

  /**
   * {@inheritDoc} Parser used for parsing XML document is SAX parser.
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#importCaption(java.lang.String)
   */
  @Override
  public CaptionCollection importCaption(String in) throws IllegalCaptionFormatException {

    // convert to input stream
    InputStream stream = new ByteArrayInputStream(in.getBytes());

    // create new collection
    CaptionCollection collection = new CaptionCollectionImpl();

    // get SAX parser and parse xml document
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser parser = factory.newSAXParser();
      DefaultHandler handler = new XMLHandler(collection);
      parser.parse(stream, handler);
    } catch (ParserConfigurationException e) {
      throw new IllegalCaptionFormatException(e.getMessage());
    } catch (SAXException e) {
      throw new IllegalCaptionFormatException(e.getMessage());
    } catch (IOException e) {
      throw new IllegalCaptionFormatException(e.getMessage());
    } finally {
      try {
        stream.close();
      } catch (IOException e) {
      }
    }

    // return collection
    return collection;
  }

  /**
   * {@inheritDoc} DOM parser is used to parse template from which whole document is then constructed.
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#exportCaption(org.opencastproject.caption.api.CaptionCollection)
   */
  @Override
  public String exportCaption(CaptionCollection captionCollection) {
    // get document builder factory and parse template
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    Document doc = null;
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputStream is = new ByteArrayInputStream(TEMPLATE.getBytes());
      doc = builder.parse(is);
      is.close();
    } catch (ParserConfigurationException e) {
      // should not happen
      throw new RuntimeException(e);
    } catch (SAXException e) {
      // should not happen unless template is invalid (FIXME?)
      throw new RuntimeException(e);
    } catch (IOException e) {
      // should not happen
      throw new RuntimeException(e);
    }

    // retrieve first div node - captions will be inside this div element
    Node divNode = doc.getElementsByTagName("div").item(0);

    // update document
    Iterator<Caption> iterator = captionCollection.getCollectionIterator();
    while (iterator.hasNext()) {
      Caption caption = iterator.next();
      Element newNode = doc.createElement("p");
      newNode.setAttribute("begin", TimeUtil.exportToDFXP(caption.getStartTime()));
      newNode.setAttribute("end", TimeUtil.exportToDFXP(caption.getStopTime()));
      String[] captionText = caption.getCaption();
      // text part
      newNode.appendChild(doc.createTextNode(captionText[0]));
      for (int i = 1; i < captionText.length; i++) {
        newNode.appendChild(doc.createElement("br"));
        newNode.appendChild(doc.createTextNode(captionText[i]));
      }
      divNode.appendChild(newNode);
    }

    // convert document to the string
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    DOMSource source = new DOMSource(doc);
    TransformerFactory tfactory = TransformerFactory.newInstance();
    Transformer transformer;
    try {
      transformer = tfactory.newTransformer();
      transformer.transform(source, result);
    } catch (TransformerConfigurationException e) {
      // should not happen
      throw new RuntimeException(e);
    } catch (TransformerException e) {
      // should not happen
      throw new RuntimeException(e);
    }

    return writer.toString();
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
    return true;
  }

  /**
   * Handler class that handles SAX parser events. Style information is not handled.
   * 
   */
  private class XMLHandler extends DefaultHandler {

    // time and text attributes
    boolean captionPhase;
    ArrayList<String> captionLineList;
    Time startTime;
    Time endTime;

    // caption collection used when parsing xml document
    CaptionCollection collection;

    XMLHandler(CaptionCollection collection) {
      this.collection = collection;
      this.captionLineList = new ArrayList<String>();
    }

    /**
     * Stores each caption line in {@link ArrayList}. When new characters are encountered, last line is retrieved and
     * concatenation is performed. This way more exotic captions can be parsed, for example:</br> &lt;p
     * begin="0:00:00.00" end="0:00:05.00"&gt;Alex &lt;span tts:color="red"&gt;Cross&lt;/span&gt;&lt;br /&gt;This is my
     * 1st caption&lt;/p&gt;
     * 
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      if (captionPhase) {
        String captionLine = new String(ch, start, length);
        captionLineList.add(captionLineList.remove(captionLineList.size() - 1).concat(captionLine));
      }
    }

    /**
     * When end of <code>p</code> tag is encountered, start time, end time and caption text is collected and new
     * {@link Caption} instance is created, which is added to the collection. When <code>br</code> tag is encountered,
     * new empty string is entered in ArrayList.
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      // </p>
      if (captionPhase && qName.equals("p")) {
        Caption caption = new CaptionImpl(startTime, endTime, captionLineList.toArray(new String[0]));
        collection.addCaption(caption);
        // cleaning
        captionLineList.clear();
        startTime = null;
        endTime = null;
        captionPhase = false;
      }
      // <br/>
      if (captionPhase && qName.equals("br")) {
        captionLineList.add("");
      }
    }

    /**
     * When <code>p</code> tag is encountered begin and end time of a caption are parsed and stored. New empty String is
     * entered in ArrayList. If exception occurs during time parsing, SAXException is thrown.
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String,
     *      org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      // <p>
      if (qName.equals("p")) {
        try {
          startTime = TimeUtil.importDFXP(attributes.getValue("begin"));
          endTime = TimeUtil.importDFXP(attributes.getValue("end"));
          // set captionlist
          captionLineList.add("");
        } catch (IllegalTimeFormatException e) {
          throw new SAXException(e);
        }
        captionPhase = true;
      }
    }
  }
}
