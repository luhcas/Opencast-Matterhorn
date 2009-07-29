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
package org.opencastproject.media.bundle.mpeg7;

import org.opencastproject.media.bundle.XmlElement;

import java.util.Iterator;

/**
 * A video segment represents a temporal decomposition of the video stream that
 * may have properties like text annotations attached to it.
 * 
 * <pre>
 * &lt;complexType name=&quot;SegmentType&quot; abstract=&quot;true&quot;&gt;
 *   &lt;complexContent&gt;
 *       &lt;extension base=&quot;mpeg7:DSType&quot;&gt;
 *           &lt;sequence&gt;
 *               &lt;choice minOccurs=&quot;0&quot;&gt;
 *                   &lt;element name=&quot;MediaInformation&quot; type=&quot;mpeg7:MediaInformationType&quot;/&gt;
 *                   &lt;element name=&quot;MediaInformationRef&quot; type=&quot;mpeg7:ReferenceType&quot;/&gt;
 *                   &lt;element name=&quot;MediaLocator&quot; type=&quot;mpeg7:MediaLocatorType&quot;/&gt;
 *               &lt;/choice&gt;
 *               &lt;element name=&quot;StructuralUnit&quot; type=&quot;mpeg7:ControlledTermUseType&quot; minOccurs=&quot;0&quot;/&gt;
 *               &lt;choice minOccurs=&quot;0&quot;&gt;
 *                   &lt;element name=&quot;CreationInformation&quot; type=&quot;mpeg7:CreationInformationType&quot;/&gt;
 *                   &lt;element name=&quot;CreationInformationRef&quot; type=&quot;mpeg7:ReferenceType&quot;/&gt;
 *               &lt;/choice&gt;
 *               &lt;choice minOccurs=&quot;0&quot;&gt;
 *                   &lt;element name=&quot;UsageInformation&quot; type=&quot;mpeg7:UsageInformationType&quot;/&gt;
 *                   &lt;element name=&quot;UsageInformationRef&quot; type=&quot;mpeg7:ReferenceType&quot;/&gt;
 *               &lt;/choice&gt;
 *               &lt;element name=&quot;TextAnnotation&quot; minOccurs=&quot;0&quot; maxOccurs=&quot;unbounded&quot;&gt;
 *                   &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                           &lt;extension base=&quot;mpeg7:TextAnnotationType&quot;&gt;
 *                               &lt;attribute name=&quot;type&quot; use=&quot;optional&quot;&gt;
 *                                   &lt;simpleType&gt;
 *                                       &lt;union memberTypes=&quot;mpeg7:termReferenceType string&quot;/&gt;
 *                                   &lt;/simpleType&gt;
 *                               &lt;/attribute&gt;
 *                           &lt;/extension&gt;
 *                       &lt;/complexContent&gt;
 *                   &lt;/complexType&gt;
 *               &lt;/element&gt;
 *               &lt;choice minOccurs=&quot;0&quot; maxOccurs=&quot;unbounded&quot;&gt;
 *                   &lt;element name=&quot;Semantic&quot; type=&quot;mpeg7:SemanticType&quot;/&gt;
 *                   &lt;element name=&quot;SemanticRef&quot; type=&quot;mpeg7:ReferenceType&quot;/&gt;
 *               &lt;/choice&gt;
 *               &lt;element name=&quot;MatchingHint&quot; type=&quot;mpeg7:MatchingHintType&quot; minOccurs=&quot;0&quot; maxOccurs=&quot;unbounded&quot;/&gt;
 *               &lt;element name=&quot;PointOfView&quot; type=&quot;mpeg7:PointOfViewType&quot; minOccurs=&quot;0&quot; maxOccurs=&quot;unbounded&quot;/&gt;
 *               &lt;element name=&quot;Relation&quot; type=&quot;mpeg7:RelationType&quot; minOccurs=&quot;0&quot; maxOccurs=&quot;unbounded&quot;/&gt;
 *           &lt;/sequence&gt;
 *       &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface ContentSegment extends XmlElement {

  /** The segment type */
  enum Type {
    AudioSegment, VideoSegment, AudioVisualSegment
  };

  /**
   * Returns the segment identifier.
   * 
   * @return the identifier
   */
  String getIdentifier();

  /**
   * Sets the segment's media time constraints.
   * 
   * @param mediaTime
   *          the media time
   */
  void setMediaTime(MediaTime mediaTime);

  /**
   * Returns the segment's time constraints.
   * 
   * @return the media time
   */
  MediaTime getMediaTime();

  /**
   * Returns <code>true</code> if the segment contains any text annotations.
   * 
   * @return <code>true</code> if there are text annotations
   */
  boolean hasTextAnnotations();

  /**
   * Returns the number of text annotations. Note that text annotations are
   * containers themselves, containing a number of keywords and free text
   * entries.
   * 
   * @return the number of text annotations
   */
  int getTextAnnotationCount();

  /**
   * Returns <code>true</code> if the segment contains text annotations in the
   * specified language.
   * 
   * @return <code>true</code> if there are text annotations
   */
  boolean hasTextAnnotations(String language);

  /**
   * Returns <code>true</code> if the segment contains text annotations that
   * satisfy the given relevance and confidence values.
   * 
   * @return <code>true</code> if there are text annotations
   */
  boolean hasTextAnnotations(float relevance, float confidence);

  /**
   * Returns <code>true</code> if the segment contains text annotations that
   * satisfy the given relevance, confidence and language constraints.
   * 
   * @return <code>true</code> if there are text annotations
   */
  boolean hasTextAnnotations(float relevance, float confidence, String language);

  /**
   * Creates a new text annotation that will hold keywords and free text
   * comments.
   * 
   * @param relevance
   *          the relevance value
   * @param confidence
   *          the confidence
   * @param language
   *          the language identifier
   * @return the new text annotation
   */
  TextAnnotation createTextAnnotation(float relevance, float confidence,
      String language);

  /**
   * Returns this segment's text annotations.
   * 
   * @return the text annotations
   */
  Iterator<TextAnnotation> textAnnotations();

  /**
   * Returns this segment's text annotations, sorted by relevance.
   * 
   * @return the text annotations
   */
  Iterator<TextAnnotation> textAnnotationsByRelevance();

  /**
   * Returns this segment's text annotations, sorted by relevance.
   * 
   * @return the text annotations
   */
  Iterator<TextAnnotation> textAnnotationsByConfidence();

}