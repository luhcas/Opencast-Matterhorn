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

import org.opencastproject.media.bundle.XmlElement;

import java.util.Iterator;

/**
 * A video segment represents a temporal decomposition of the video stream that
 * may have properties like text annotations attached to it.
 * 
 * <pre>
 * <complexType name="SegmentType" abstract="true">
 *   <complexContent>
 *       <extension base="mpeg7:DSType">
 *           <sequence>
 *               <choice minOccurs="0">
 *                   <element name="MediaInformation" type="mpeg7:MediaInformationType"/>
 *                   <element name="MediaInformationRef" type="mpeg7:ReferenceType"/>
 *                   <element name="MediaLocator" type="mpeg7:MediaLocatorType"/>
 *               </choice>
 *               <element name="StructuralUnit" type="mpeg7:ControlledTermUseType" minOccurs="0"/>
 *               <choice minOccurs="0">
 *                   <element name="CreationInformation" type="mpeg7:CreationInformationType"/>
 *                   <element name="CreationInformationRef" type="mpeg7:ReferenceType"/>
 *               </choice>
 *               <choice minOccurs="0">
 *                   <element name="UsageInformation" type="mpeg7:UsageInformationType"/>
 *                   <element name="UsageInformationRef" type="mpeg7:ReferenceType"/>
 *               </choice>
 *               <element name="TextAnnotation" minOccurs="0" maxOccurs="unbounded">
 *                   <complexType>
 *                       <complexContent>
 *                           <extension base="mpeg7:TextAnnotationType">
 *                               <attribute name="type" use="optional">
 *                                   <simpleType>
 *                                       <union memberTypes="mpeg7:termReferenceType string"/>
 *                                   </simpleType>
 *                               </attribute>
 *                           </extension>
 *                       </complexContent>
 *                   </complexType>
 *               </element>
 *               <choice minOccurs="0" maxOccurs="unbounded">
 *                   <element name="Semantic" type="mpeg7:SemanticType"/>
 *                   <element name="SemanticRef" type="mpeg7:ReferenceType"/>
 *               </choice>
 *               <element name="MatchingHint" type="mpeg7:MatchingHintType" minOccurs="0" maxOccurs="unbounded"/>
 *               <element name="PointOfView" type="mpeg7:PointOfViewType" minOccurs="0" maxOccurs="unbounded"/>
 *               <element name="Relation" type="mpeg7:RelationType" minOccurs="0" maxOccurs="unbounded"/>
 *           </sequence>
 *       </extension>
 *   </complexContent>
 * </complexType>
 * </pre>
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface ContentSegment extends XmlElement {

	/** The segment type */
	enum Type { AudioSegment, VideoSegment, AudioVisualSegment };
	
	/**
	 * Returns the segment identifier.
	 * 
	 * @return the identifier
	 */
	String getIdentifier();

	/**
	 * Sets the segment's media time constraints.
	 * 
	 * @param mediaTime the media time
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
	 * @param relevance the relevance value
	 * @param confidence the confidence
	 * @param language the language identifier
	 * @return the new text annotation
	 */
	TextAnnotation createTextAnnotation(float relevance, float confidence, String language);
	
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