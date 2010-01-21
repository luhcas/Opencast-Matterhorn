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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: TemporalDecompositionImpl.java 1404 2008-11-04 17:50:38Z wunden $
 */
public class TemporalDecompositionImpl<T extends ContentSegment> implements TemporalDecomposition<T> {

  /** <code>True</code> if there is a gap */
  protected boolean gap = false;

  /** <code>True</code> if the segment overlaps */
  protected boolean overlap = false;

  /** The decomposition criteria */
  protected DecompositionCriteria criteria = DecompositionCriteria.Temporal;

  /** The list of segments in this temporal decomposition */
  protected List<T> segments = null;

  /** Indicator for the presence of gaps in between segments */
  boolean hasGap = false;

  /** Indicator for the presence of overlapping segments */
  boolean isOverlapping = false;

  /** The segment type */
  private ContentSegment.Type segmentType = null;

  /**
   * Creates a new temporal decomposition container.
   * 
   * @param segmentType
   *          the segment type
   */
  public TemporalDecompositionImpl(ContentSegment.Type segmentType) {
    segments = new ArrayList<T>();
    this.segmentType = segmentType;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.TemporalDecomposition#setGap(boolean)
   */
  public void setGap(boolean hasGap) {
    this.hasGap = hasGap;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.TemporalDecomposition#hasGap()
   */
  public boolean hasGap() {
    return gap;
  }

  @SuppressWarnings("unchecked")
  public T createSegment(String id) {
    T segment = (T) new ContentSegmentImpl(segmentType, id);
    if (segments.contains(segment))
      throw new IllegalArgumentException("Duplicate segment id detected: " + id);
    segments.add(segment);
    return segment;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.TemporalDecomposition#hasSegments()
   */
  public boolean hasSegments() {
    return segments.size() > 0;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.TemporalDecomposition#setOverlapping(boolean)
   */
  public void setOverlapping(boolean isOverlapping) {
    this.isOverlapping = isOverlapping;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.TemporalDecomposition#setCriteria(org.opencastproject.media.mediapackage.mpeg7.TemporalDecomposition.DecompositionCriteria)
   */
  public void setCriteria(DecompositionCriteria criteria) {
    if (criteria == null)
      throw new IllegalArgumentException("Decomposition criteria must not be null");
    this.criteria = criteria;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.TemporalDecomposition#isOverlapping()
   */
  public boolean isOverlapping() {
    return overlap;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.TemporalDecomposition#getCriteria()
   */
  public DecompositionCriteria getCriteria() {
    return DecompositionCriteria.Temporal;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.TemporalDecomposition#getSegmentById(java.lang.String)
   */
  public T getSegmentById(String segmentId) {
    for (T segment : segments) {
      if (segmentId.equals(segment.getIdentifier()))
        return segment;
    }
    return null;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.TemporalDecomposition#segments()
   */
  public Iterator<T> segments() {
    return segments.iterator();
  }

  /**
   * @see org.opencastproject.media.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  public Node toXml(Document document) {
    Element node = document.createElement("TemporalDecomposition");
    node.setAttribute("gap", (gap ? "true" : "false"));
    node.setAttribute("overlap", (overlap ? "true" : "false"));
    node.setAttribute("criteria", criteria.toString().toLowerCase());
    for (ContentSegment segment : segments) {
      node.appendChild(segment.toXml(document));
    }
    return node;
  }

}
