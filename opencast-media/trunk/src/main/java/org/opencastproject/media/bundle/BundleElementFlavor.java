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

package org.opencastproject.media.bundle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the element type.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class BundleElementFlavor implements Cloneable,
    Comparable<BundleElementFlavor>, Serializable {

  /** Serial version uid */
  private static final long serialVersionUID = 1L;

  int NO_MATCH = -1;
  int MATCH_TYPE = 1;
  int MATCH_SPECIFIC_TYPE = 2;
  int MATCH_SUBTYPE = 3;
  int MATCH_SPECIFIC_SUBTYPE = 4;

  /** String representation of type */
  private String type_ = null;

  /** String representation of subtype */
  private String subtype_ = null;

  /** Alternate representations for type/subtype */
  private List<ElementTypeEquivalent> equivalents_ = null;

  /** Main description */
  private String description_ = null;

  /** The element type flavor */
  private String flavor_ = null;

  /** The element type flavor description */
  private String flavorDescription_ = null;

  /**
   * Creates a new element type with the given type and subtype.
   * 
   * @param type
   *          the major type
   * @param subtype
   *          minor type
   */
  public BundleElementFlavor(String type, String subtype) {
    if (type == null)
      throw new IllegalArgumentException(
          "Argument 'type' of element type may not be null!");
    if (subtype == null)
      throw new IllegalArgumentException(
          "Argument 'subtype' of element type may not be null!");
    equivalents_ = new ArrayList<ElementTypeEquivalent>();
    type_ = type.trim().toLowerCase();
    subtype_ = subtype.trim().toLowerCase();
  }

  /**
   * Returns the major type of this element type.
   * <p>
   * For example, if the element type is a presentation movie which is
   * represented as <code>video/x-presentation</code>, this method will return
   * <code>video</code>.
   * 
   * @return the type
   */
  public String getType() {
    return type_;
  }

  /**
   * Returns the minor type of this element type.
   * <p>
   * For example, if the element type is a presentation movie which is
   * represented as <code>video/x-presentation</code>, this method will return
   * <code>x-presentation</code>.
   * 
   * @return the subtype
   */
  public String getSubtype() {
    return subtype_;
  }

  /**
   * Returns the element type description.
   * 
   * @return the description
   */
  public String getDescription() {
    return this.description_;
  }

  /**
   * Sets the element type description.
   * 
   * @param description
   */
  public void setDescription(String description) {
    this.description_ = description;
  }

  /**
   * Returns the flavor of this element type.
   * <p>
   * A flavor is a hint on a specialized variant of a general element type. For
   * example, a dublin core file will have a element type of
   * <code>text/xml</code>. Adding a flavor of <code>Dublin Core</code> gives an
   * additional hint on the file contents.
   * 
   * @return the file's flavor
   */
  public String getFlavor() {
    return flavor_;
  }

  /**
   * Returns the flavor description.
   * 
   * @return the flavor description
   */
  public String getFlavorDescription() {
    return flavorDescription_;
  }

  /**
   * Sets the flavor of this element type.
   * 
   * @param flavor
   *          the flavor
   */
  public void setFlavor(String flavor) {
    setFlavor(flavor, null);
  }

  /**
   * Sets the flavor of this element type along with a flavor description.
   * 
   * @param flavor
   *          the flavor
   * @param description
   *          the flavor description
   */
  public void setFlavor(String flavor, String description) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor must not be null!");
    this.flavor_ = flavor.trim();
    this.flavorDescription_ = description;
  }

  /**
   * Returns <code>true</code> if the file has the given flavor associated.
   * 
   * @return <code>true</code> if the file has that flavor
   */
  public boolean hasFlavor(String flavor) {
    if (flavor_ == null)
      return false;
    return flavor_.equalsIgnoreCase(flavor);
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
  public void addEquivalent(String type, String subtype)
      throws IllegalArgumentException {
    if (type == null)
      throw new IllegalArgumentException("Type must not be null!");
    if (subtype == null)
      throw new IllegalArgumentException("Subtype must not be null!");

    if (equivalents_ == null)
      equivalents_ = new ArrayList<ElementTypeEquivalent>();
    equivalents_.add(new ElementTypeEquivalent(type, subtype));
  }

  /**
   * @see java.lang.Object#clone()
   */
  @Override
  public BundleElementFlavor clone() throws CloneNotSupportedException {
    BundleElementFlavor m = new BundleElementFlavor(type_, subtype_);
    m.equivalents_.addAll(equivalents_);
    m.flavor_ = flavor_;
    m.flavorDescription_ = flavorDescription_;
    return m;
  }

  /**
   * Returns <code>true</code> if this element type is an equivalent for the
   * specified type and subtype.
   * <p>
   * For example, a gzipped file may have both of these element types defined,
   * <code>application/x-compressed</code> or <code>application/x-gzip</code>.
   * 
   * @return <code>true</code> if this mime type is an equivalent
   */
  public boolean isEquivalentTo(String type, String subtype) {
    if (type_.equalsIgnoreCase(type) && subtype_.equalsIgnoreCase(subtype))
      return true;
    if (equivalents_ != null) {
      for (ElementTypeEquivalent equivalent : equivalents_) {
        if (equivalent.matches(type, subtype))
          return true;
      }
    }
    return false;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (o instanceof BundleElementFlavor) {
      BundleElementFlavor m = (BundleElementFlavor) o;
      return type_.equals(m.getType()) && subtype_.equals(m.getSubtype());
    }
    return super.equals(o);
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(BundleElementFlavor m) {
    return toString().compareTo(m.toString());
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (flavorDescription_ != null)
      return flavorDescription_;
    else if (description_ != null)
      return description_;
    else
      return type_ + "/" + subtype_;
  }

  /**
   * Creates a new bundle element flavor.
   * 
   * @param s
   *          the bundle flavor
   * @return the bundle element flavor object
   * @throws IllegalArgumentException
   *           if the string <code>s</code> does not contain a <t>dash</t> to
   *           divide the type from subtype.
   */
  public static BundleElementFlavor parseFlavor(String s)
      throws IllegalArgumentException {
    if (s == null)
      throw new IllegalArgumentException(
          "Unable to create element flavor from 'null'");
    String[] parts = s.split("/");
    if (parts.length < 2)
      throw new IllegalArgumentException(
          "Unable to create element flavor from '" + s + "'");
    return new BundleElementFlavor(parts[0], parts[1]);
  }

  /**
   * Helper class to store type/subtype equivalents for a given element type.
   * 
   * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
   */
  private class ElementTypeEquivalent implements Serializable {

    /** Serial version uid */
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