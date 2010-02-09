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

package org.opencastproject.metadata.mpeg7;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * TODO: Comment me!
 */
public class ContentSegmentImpl implements ContentSegment, AudioSegment, VideoSegment, AudioVisualSegment {

  /** The content type */
  protected ContentSegment.Type type = null;

  /** The content element identifier */
  protected String id = null;

  /** The content time contraints */
  protected MediaTime mediaTime = null;

  /** The text annotations */
  protected List<TextAnnotation> annotations = null;

  /**
   * Creates a new content segment.
   * 
   * @param type
   *          the segment type
   * @param id
   *          the segment identifier
   */
  public ContentSegmentImpl(ContentSegment.Type type, String id) {
    this.type = type;
    this.id = id;
    annotations = new ArrayList<TextAnnotation>();
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.ContentSegment#getIdentifier()
   */
  public String getIdentifier() {
    return id;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.ContentSegment#setMediaTime(org.opencastproject.media.mediapackage.mpeg7.MediaTime)
   */
  public void setMediaTime(MediaTime mediaTime) {
    this.mediaTime = mediaTime;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.ContentSegment#getMediaTime()
   */
  public MediaTime getMediaTime() {
    return mediaTime;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.ContentSegment#hasTextAnnotations()
   */
  public boolean hasTextAnnotations() {
    return annotations.size() > 0;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.ContentSegment#hasTextAnnotations(java.lang.String)
   */
  public boolean hasTextAnnotations(String language) {
    return hasTextAnnotations(0.0f, 0.0f, language);
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.ContentSegment#hasTextAnnotations(float, float)
   */
  public boolean hasTextAnnotations(float relevance, float confidence) {
    return hasTextAnnotations(relevance, confidence, null);
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.ContentSegment#hasTextAnnotations(float, float, java.lang.String)
   */
  public boolean hasTextAnnotations(float relevance, float confidence, String language) {
    for (TextAnnotation annotation : annotations) {
      if (annotation.getRelevance() >= relevance && annotation.getConfidence() >= confidence) {
        if (language != null) {
          if (language.equals(annotation.getLanguage()))
            return true;
        } else {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.ContentSegment#getTextAnnotationCount()
   */
  public int getTextAnnotationCount() {
    return annotations.size();
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.ContentSegment#textAnnotationsByConfidence()
   */
  public Iterator<TextAnnotation> textAnnotationsByConfidence() {
    SortedSet<TextAnnotation> set = new TreeSet<TextAnnotation>(new Comparator<TextAnnotation>() {
      public int compare(TextAnnotation a1, TextAnnotation a2) {
        if (a1.getConfidence() > a2.getConfidence())
          return 1;
        else if (a1.getConfidence() > a2.getConfidence())
          return -1;
        return 0;
      }
    });
    set.addAll(annotations);
    return set.iterator();
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.ContentSegment#textAnnotationsByRelevance()
   */
  public Iterator<TextAnnotation> textAnnotationsByRelevance() {
    SortedSet<TextAnnotation> set = new TreeSet<TextAnnotation>(new Comparator<TextAnnotation>() {
      public int compare(TextAnnotation a1, TextAnnotation a2) {
        if (a1.getRelevance() > a2.getRelevance())
          return 1;
        else if (a1.getRelevance() > a2.getRelevance())
          return -1;
        return 0;
      }
    });
    set.addAll(annotations);
    return set.iterator();
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.ContentSegment#createTextAnnotation(float, float, String)
   */
  public TextAnnotation createTextAnnotation(float relevance, float confidence, String language) {
    TextAnnotationImpl annotation = new TextAnnotationImpl(relevance, confidence, language);
    annotations.add(annotation);
    return annotation;
  }

  /**
   * @see org.opencastproject.media.mediapackage.mpeg7.ContentSegment#textAnnotations()
   */
  public Iterator<TextAnnotation> textAnnotations() {
    return annotations.iterator();
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return id.hashCode();
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ContentSegment) {
      return id.equals(((ContentSegment) obj).getIdentifier());
    }
    return super.equals(obj);
  }

  /**
   * @see org.opencastproject.media.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  public Node toXml(Document document) {
    Element node = document.createElement(type.toString());
    node.setAttribute("id", id);
    node.appendChild(mediaTime.toXml(document));
    for (TextAnnotation annotation : annotations) {
      node.appendChild(annotation.toXml(document));
    }
    return node;
  }

}
