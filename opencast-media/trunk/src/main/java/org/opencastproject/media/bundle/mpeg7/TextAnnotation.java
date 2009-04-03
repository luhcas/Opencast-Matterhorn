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
 * Base interface for text annotations with relevance and confidence values.
 * 
 * <pre>
 * <complexType name="TextAnnotationType">
 *   <choice minOccurs="1" maxOccurs="unbounded">
 *       <element name="FreeTextAnnotation" type="mpeg7:TextualType"/>
 *       <element name="StructuredAnnotation" type="mpeg7:StructuredAnnotationType"/>
 *       <element name="DependencyStructure" type="mpeg7:DependencyStructureType"/>
 *       <element name="KeywordAnnotation" type="mpeg7:KeywordAnnotationType"/>
 *   </choice>
 *   <attribute name="relevance" type="mpeg7:zeroToOneType" use="optional"/>
 *   <attribute name="confidence" type="mpeg7:zeroToOneType" use="optional"/>
 *   <attribute ref="xml:lang"/>
 * </complexType>
 * </pre>
 * 
 * TODO: How to encode source and version? Maybe use MediaInformation in
 * VideoSegment
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface TextAnnotation extends XmlElement {

	/**
	 * Returns the relevance of this annotation.
	 * 
	 * @return the relevance value
	 */
	float getRelevance();
	
	/**
	 * Returns the confidence of the validity of this annotation. The value
	 * will be in the range of <code>0.0</code> and <code>1.0</code>. 
	 * 
	 * The confidence may vary with the technique that was used to create the
	 * annotation. For example, extracting a word using optical character
	 * recognition usually has a better confidence value than speech
	 * recognition. 
	 * 
	 * @return the confidence value
	 */
	float getConfidence();
	
	/**
	 * Returns the language of this annotation in ISO represenatation, e. g. 
	 * <code>en</code> for annotations in English.
	 * 
	 * @return the language
	 */
	String getLanguage();
	
	/**
	 * Adds a new keyword annotation.
	 * 
	 * @param keywordAnnotation the annotation
	 */
	void addKeywordAnnotation(KeywordAnnotation keywordAnnotation);

	/**
	 * Adds a free text annotation.
	 * 
	 * @param freeTextAnnotation the annotation
	 */
	void addFreeTextAnnotation(FreeTextAnnotation freeTextAnnotation);
	
	/**
	 * Returns an iteration of the keyword annotations.
	 * 
	 * @return the keyword annotations
	 */
	Iterator<KeywordAnnotation> keywordAnnotations();

	/**
	 * Returns an iteration of the free text annotations.
	 * 
	 * @return the free text annotations
	 */
	Iterator<FreeTextAnnotation> freeTextAnnotations();

}