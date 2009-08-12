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

package org.opencastproject.media.mediapackage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ELement flavors describe {@link MediaPackageElement}s in a semantic way. They reveal or give at least a hint about
 * the meaning of an element.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageElementFlavor.java 2905 2009-07-15 16:16:05Z ced $
 */
public class MediaPackageElementFlavor implements Cloneable, Comparable<MediaPackageElementFlavor>, Serializable {

  /**
   * Serial version uid
   */
  private static final long serialVersionUID = 1L;

  int NO_MATCH = -1;
  int MATCH_TYPE = 1;
  int MATCH_SPECIFIC_TYPE = 2;
  int MATCH_SUBTYPE = 3;
  int MATCH_SPECIFIC_SUBTYPE = 4;

  /**
   * String representation of type
   */
  private String type = null;

  /**
   * String representation of subtype
   */
  private String subtype = null;

  /**
   * Alternate representations for type/subtype
   */
  private List<ElementTypeEquivalent> equivalents = new ArrayList<ElementTypeEquivalent>();

  /**
   * Main description
   */
  private String description = null;

  /**
   * Creates a new element type with the given type, subtype and a description.
   * 
   * @param type
   *          the major type
   * @param subtype
   *          minor type
   * @param description
   *          an optional description
   */
  public MediaPackageElementFlavor(String type, String subtype, String description) {
    if (type == null)
      throw new IllegalArgumentException("Argument 'type' of element type may not be null!");
    if (subtype == null)
      throw new IllegalArgumentException("Argument 'subtype' of element type may not be null!");
    this.type = type.trim().toLowerCase();
    this.subtype = subtype.trim().toLowerCase();
    this.description = description;
  }

  /**
   * Creates a new element type with the given type and subtype.
   * 
   * @param type
   *          the major type
   * @param subtype
   *          minor type
   */
  public MediaPackageElementFlavor(String type, String subtype) {
    this(type, subtype, null);
  }

  /**
   * Returns the major type of this element type. Major types are more of a technical description.
   * <p/>
   * For example, if the element type is a presentation movie which is represented as <code>track/presentation</code>,
   * this method will return <code>track</code>.
   * 
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Returns the minor type of this element type. Minor types define the meaning.
   * <p/>
   * For example, if the element type is a presentation movie which is represented as <code>track/presentation</code>,
   * this method will return <code>presentation</code>.
   * 
   * @return the subtype
   */
  public String getSubtype() {
    return subtype;
  }

  /**
   * Returns the element type description.
   * 
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the element type description.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Adds an equivalent type / subtype definition for this element type.
   * 
   * @param type
   *          major type
   * @param subtype
   *          minor type
   * @throws IllegalArgumentException
   *           if any of the arguments is <code>null</code>
   */
  public void addEquivalent(String type, String subtype) throws IllegalArgumentException {
    if (type == null)
      throw new IllegalArgumentException("Type must not be null!");
    if (subtype == null)
      throw new IllegalArgumentException("Subtype must not be null!");

    equivalents.add(new ElementTypeEquivalent(type, subtype));
  }

  /**
   * @see java.lang.Object#clone()
   */
  @Override
  public MediaPackageElementFlavor clone() throws CloneNotSupportedException {
    MediaPackageElementFlavor m = new MediaPackageElementFlavor(type, subtype, description);
    m.equivalents.addAll(equivalents);
    return m;
  }

  /**
   * Returns <code>true</code> if this element type is an equivalent for the specified type and subtype.
   * <p/>
   * For example, a gzipped file may have both of these element types defined, <code>application/x-compressed</code> or
   * <code>application/x-gzip</code>.
   * 
   * @return <code>true</code> if this mime type is an equivalent
   */
  public boolean isEquivalentTo(String type, String subtype) {
    if (this.type.equalsIgnoreCase(type) && this.subtype.equalsIgnoreCase(subtype))
      return true;
    if (equivalents != null) {
      for (ElementTypeEquivalent equivalent : equivalents) {
        if (equivalent.matches(type, subtype))
          return true;
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof MediaPackageElementFlavor) {
      MediaPackageElementFlavor m = (MediaPackageElementFlavor) o;
      return type.equals(m.getType()) && subtype.equals(m.getSubtype());
    }
    return super.equals(o);
  }

  /**
   * Defines equality between flavors and strings.
   * 
   * @param flavor
   *          string of the form "type/subtype"
   */
  public boolean eq(String flavor) {
    return flavor != null && flavor.equals(toString());
  }

  /**
   * @see java.lang.String#compareTo(java.lang.Object)
   */
  public int compareTo(MediaPackageElementFlavor m) {
    return toString().compareTo(m.toString());
  }

  /**
   * Returns the flavor as a string "type/subtype".
   */
  @Override
  public String toString() {
    return type + "/" + subtype;
  }

  /**
   * Creates a new media package element flavor.
   * 
   * @param s
   *          the media package flavor
   * @return the media package element flavor object
   * @throws IllegalArgumentException
   *           if the string <code>s</code> does not contain a <t>dash</t> to divide the type from subtype.
   */
  public static MediaPackageElementFlavor parseFlavor(String s) throws IllegalArgumentException {
    if (s == null)
      throw new IllegalArgumentException("Unable to create element flavor from 'null'");
    String[] parts = s.split("/");
    if (parts.length < 2)
      throw new IllegalArgumentException("Unable to create element flavor from '" + s + "'");
    return new MediaPackageElementFlavor(parts[0], parts[1]);
  }

  /**
   * Helper class to store type/subtype equivalents for a given element type.
   * 
   * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
   */
  private class ElementTypeEquivalent implements Serializable {

    /**
     * Serial version uid
     */
    private static final long serialVersionUID = 1L;

    String type_;

    String subtype_;

    ElementTypeEquivalent(String type, String subtype) {
      type_ = type.trim().toLowerCase();
      subtype_ = subtype.trim().toLowerCase();
    }

    String getType() {
      return type_;
    }

    String getSubtype() {
      return subtype_;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof ElementTypeEquivalent) {
        ElementTypeEquivalent e = (ElementTypeEquivalent) o;
        return this.matches(e.getType(), e.getSubtype());
      }
      return super.equals(o);
    }

    boolean matches(String type, String subtype) {
      return type_.equalsIgnoreCase(type) && subtype_.equalsIgnoreCase(subtype);
    }

  }

}