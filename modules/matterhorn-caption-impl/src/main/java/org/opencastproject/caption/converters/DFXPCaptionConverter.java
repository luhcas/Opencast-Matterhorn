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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Converter engine for DFXP caption format.
 * 
 */
public class DFXPCaptionConverter implements CaptionConverter {

  private final String NAME = "Timed Text (TT) Authoring Format 1.0 - Distribution Format Exchange Profile (DFXP)";
  private final String ABOUT = "This is the most comprehensive caption format ever !! \n XML based. \n Additional information at <a href=\"http://www.w3.org/TR/2006/CR-ttaf1-dfxp-20061116/\">W3-dfxp</a>";
  private final String VERSION = "1.0";
  private final String EXTENSION = "dfxp.xml";
  private final String PATTERN = "xmlns=\"http://www\\.w3\\.org/2006/(10|04)/ttaf1";

  private final String TT_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
          + "<tt xmlns=\"http://www.w3.org/2006/04/ttaf1\""
          + "xmlns:tts=\"http://www.w3.org/2006/04/ttaf1#styling\" xml:lang=\"en\">" + "<head>" + "<styling>"
          + "<style id=\"defaultSpeaker\" tts:fontSize=\"12\" tts:fontFamily=\"SansSerif\""
          + "tts:fontWeight=\"normal\" tts:fontStyle=\"normal\" tts:textDecoration=\"none\""
          + "tts:color=\"white\" tts:backgroundColor=\"black\" tts:textAlign=\"left\" />"
          + "<style id=\"defaultCaption\" tts:fontSize=\"12\" tts:fontFamily=\"SansSerif\""
          + "tts:fontWeight=\"normal\" tts:fontStyle=\"normal\" tts:textDecoration=\"none\""
          + "tts:color=\"white\" tts:backgroundColor=\"black\" tts:textAlign=\"left\" />" + "</styling>" + "</head>"
          + "<body id=\"ccbody\" style=\"defaultCaption\">" + "<div xml:lang=\"en\">";
  private final String TT_FOOTER = "</div>" + "</body>" + "</tt>";

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionFormat#importCaption(java.lang.String)
   */
  @Override
  public CaptionCollection importCaption(String in) throws IllegalCaptionFormatException {

    // create caption collection
    CaptionCollection collection = new CaptionCollectionImpl();

    // get document builder
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder builder = dbf.newDocumentBuilder();
      // or just use input streams ?
      Document dom = builder.parse(new ByteArrayInputStream(in.getBytes()));

      // get root element -> FIXME go to body
      Element rootElement = dom.getDocumentElement();
      // FIXME <p> tag inside <p> </p> (is it even possible?)
      NodeList list = rootElement.getElementsByTagName("p");
      for (int i = 0; i < list.getLength(); i++) {
        Caption caption = parseCaption((Element) list.item(i));

        collection.addCaption(caption);
      }
    } catch (ParserConfigurationException e) {
      throw new IllegalCaptionFormatException(e.getMessage());
    } catch (SAXException e) {
      throw new IllegalCaptionFormatException(e.getMessage());
    } catch (IOException e) {
      throw new IllegalCaptionFormatException(e.getMessage());
    } catch (IllegalTimeFormatException e) {
      throw new IllegalCaptionFormatException(e.getMessage());
    }

    return collection;
  }

  /**
   * Create {@link Caption} instance by parsing one p element.</br>Sample format: &lt;p begin="0:00:00.785"
   * end="0:00:02.765"&gt; Hello! &lt;/p&gt;
   * 
   * @param element
   *          element to be parsed
   * @return Caption instance
   * @throws IllegalTimeFormatException
   *           if time format cannot be parsed
   */
  private Caption parseCaption(Element element) throws IllegalTimeFormatException {
    // get start time
    String startTimeS = element.getAttribute("begin");
    Time startTime = TimeUtil.importDFXP(startTimeS.trim());
    // get end time
    String endTimeS = element.getAttribute("end");
    Time endTime = TimeUtil.importDFXP(endTimeS.trim());
    // parse text core and create caption
    Caption caption = new CaptionImpl(startTime, endTime, getTextCore(element));
    return caption;
  }

  /**
   * Parsing text data from caption, stripping it of all xml tags and substituting <br />
   * tag with newline character.
   * 
   * @param node
   *          {@link Node} to be parsed
   * @return caption text without tags
   */
  private String getTextCore(Node node) {
    StringBuffer captionText = new StringBuffer();
    // get children
    NodeList list = node.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      if (list.item(i).getNodeType() == Node.TEXT_NODE) {
        captionText.append(list.item(i).getTextContent());
      } else if (list.item(i).getNodeName().equals("br")) {
        // FIXME newlines
        captionText.append("\n");
      } else {
        captionText.append(getTextCore(list.item(i)));
      }
    }
    return captionText.toString().trim();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionFormat#exportCaption(org.opencastproject.caption.api.CaptionCollection)
   */
  @Override
  public String exportCaption(CaptionCollection captionCollection) {
    // create buffer with header
    StringBuffer buffer = new StringBuffer(TT_HEADER);
    // get caption collection iterator
    Iterator<Caption> iter = captionCollection.getCollectionIterator();
    while (iter.hasNext()) {
      Caption caption = iter.next();
      String startTime = TimeUtil.exportToDFXP(caption.getStartTime());
      String endTime = TimeUtil.exportToDFXP(caption.getStopTime());
      String captionText = caption.getCaption().replace("\n", "<br/>");
      String DFXPCaption = String.format("<p begin=\"%s\" end=\"%s\">%s</p>", startTime, endTime, captionText);
      buffer.append(DFXPCaption);
    }
    // add footer
    buffer.append(TT_FOOTER);
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
    return true;
  }
}
