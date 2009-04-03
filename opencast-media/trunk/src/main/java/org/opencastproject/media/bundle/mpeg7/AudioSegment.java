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

/**
 * An audio segment represents a temporal decomposition of the audio stream that
 * may have properties like text annotations attached to it.
 * 
 * <pre>
 * <complexType name="AudioSegmentType">
 *   <complexContent>
 *       <extension base="mpeg7:SegmentType">
 *           <sequence>
 *               <choice minOccurs="0">
 *                   <element name="MediaTime" type="mpeg7:MediaTimeType"/>
 *                   <element name="TemporalMask" type="mpeg7:TemporalMaskType"/>
 *               </choice>
 *               <choice minOccurs="0" maxOccurs="unbounded">
 *                   <element name="AudioDescriptor" type="mpeg7:AudioDType"/>
 *                   <element name="AudioDescriptionScheme" type="mpeg7:AudioDSType"/>
 *               </choice>
 *               <choice minOccurs="0" maxOccurs="unbounded">
 *                   <element name="TemporalDecomposition"
 *                       type="mpeg7:AudioSegmentTemporalDecompositionType"/>
 *                   <element name="MediaSourceDecomposition"
 *                       type="mpeg7:AudioSegmentMediaSourceDecompositionType"/>
 *               </choice>
 *           </sequence>
 *       </extension>
 *   </complexContent>
 * </complexType>
 * </pre>
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface AudioSegment extends ContentSegment {

	// Marker interface only
	
}