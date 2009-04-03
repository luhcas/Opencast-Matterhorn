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
 * A video segment represents a temporal decomposition of the video stream that
 * may have properties like text annotations attached to it.
 * 
 * <pre>
 * <complexType name="VideoSegmentType">
 *   <complexContent>
 *       <extension base="mpeg7:SegmentType">
 *           <sequence>
 *               <choice minOccurs="0">
 *                   <element name="MediaTime" type="mpeg7:MediaTimeType"/>
 *                   <element name="TemporalMask" type="mpeg7:TemporalMaskType"/>
 *               </choice>
 *               <choice minOccurs="0" maxOccurs="unbounded">
 *                   <element name="VisualDescriptor" type="mpeg7:VisualDType"/>
 *                   <element name="VisualDescriptionScheme" type="mpeg7:VisualDSType"/>
 *                   <element name="VisualTimeSeriesDescriptor" type="mpeg7:VisualTimeSeriesType"/>
 *               </choice>
 *               <element name="MultipleView" type="mpeg7:MultipleViewType" minOccurs="0"/>
 *               <element name="Mosaic" type="mpeg7:MosaicType" minOccurs="0" maxOccurs="unbounded"/>
 *               <choice minOccurs="0" maxOccurs="unbounded">
 *                   <element name="SpatialDecomposition" type="mpeg7:VideoSegmentSpatialDecompositionType"/>
 *                   <element name="TemporalDecomposition" type="mpeg7:VideoSegmentTemporalDecompositionType"/>
 *                   <element name="SpatioTemporalDecomposition" type="mpeg7:VideoSegmentSpatioTemporalDecompositionType"/>
 *                   <element name="MediaSourceDecomposition" type="mpeg7:VideoSegmentMediaSourceDecompositionType"/>
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
public interface VideoSegment extends ContentSegment {

	// Marker interface only

}