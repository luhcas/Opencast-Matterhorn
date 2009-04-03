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

/**
 * Models a keyword annotation with relevance, confidence and the keyword
 * itself.
 * 
 * <pre>
 * <complexType name="KeywordAnnotationType">
 *   <sequence>
 *       <element name="Keyword" minOccurs="1" maxOccurs="unbounded">
 *           <complexType>
 *               <simpleContent>
 *                   <extension base="mpeg7:TextualType">
 *                       <attribute name="type" use="optional" default="main">
 *                           <simpleType>
 *                               <restriction base="NMTOKEN">
 *                                   <enumeration value="main"/>
 *                                   <enumeration value="secondary"/>
 *                                   <enumeration value="other"/>
 *                               </restriction>
 *                           </simpleType>
 *                       </attribute>
 *                   </extension>
 *               </simpleContent>
 *           </complexType>
 *       </element>
 *   </sequence>
 *   <attribute ref="xml:lang" use="optional"/>
 * </complexType>
 * </pre>
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface KeywordAnnotation extends XmlElement {

	/**
	 * Enumeration defining possible types for a keyword annotation.
	 * 
	 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
	 * @version $Id$
	 */
	enum Type { main, secondary, other };
	
	/**
	 * Returns the keyword.
	 * 
	 * @return the keyword
	 */
	String getKeyword();

	/**
	 * Returns the type of this keyword annotation. The default value is
	 * <code>main</code>.
	 * 
	 * @return the keyword type
	 */
	Type getType();
	
}