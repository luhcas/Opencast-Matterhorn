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

package org.opencastproject.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the mime type. Note that mime types should not be
 * instantiated directly but be retreived from the mime type registry
 * {@link MimeTypes}.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MimeType.java 1086 2008-09-10 10:52:24Z wunden $
 */
public final class MimeType implements Cloneable, Comparable<MimeType>,
    Serializable {

  /** Serial version UID */
  private static final long serialVersionUID = -2895494708659187394L;

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
  private List<MIMEEquivalent> equivalents_ = null;

  /** Main file suffix */
  private String suffix_ = null;

  /** List of suffixes */
  private List<String> suffixes_ = null;

  /** Main description */
  private String description_ = null;

  /** The mime type flavor */
  private String flavor_ = null;

  /** The mime type flavor description */
  private String flavorDescription_ = null;

  /**
   * Creates a new mime type with the given type and subtype.
   * 
   * @param type
   *          the major type
   * @param subtype
   *          minor type
   */
  public MimeType(String type, String subtype) {
    this(type, subtype, null);
  }

  /**
   * Creates a new mime type with the given type, subtype and main file suffix.
   * 
   * @param type
   *          the major type
   * @param subtype
   *          minor type
   * @param suffix
   *          main file suffix
   */
  public MimeType(String type, String subtype, String suffix) {
    if (type == null)
      throw new IllegalArgumentException(
          "Argument 'type' of mime type may not be null!");
    if (subtype == null)
      throw new IllegalArgumentException(
          "Argument 'subtype' of mime type may not be null!");
    equivalents_ = new ArrayList<MIMEEquivalent>();
    type_ = type.trim().toLowerCase();
    subtype_ = subtype.trim().toLowerCase();
    suffixes_ = new ArrayList<String>();
    if (suffix != null) {
      suffix_ = suffix.trim().toLowerCase();
      addSuffix(suffix_);
    }
  }

  /**
   * Returns the major type of this mimetype.
   * <p>
   * For example, if the mimetype is ISO Motion JPEG 2000 which is represented
   * as <code>video/mj2</code>, this method will return <code>video</code>.
   * 
   * @return the type
   */
  public String getType() {
    return type_;
  }

  /**
   * Returns the minor type of this mimetype.
   * <p>
   * For example, if the mimetype is ISO Motion JPEG 2000 which is represented
   * as <code>video/mj2</code>, this method will return <code>mj2</code>.
   * 
   * @return the subtype
   */
  public String getSubtype() {
    return subtype_;
  }

  /**
   * Returns the main suffix for this mime type, that identifies files
   * containing data of this flavor.
   * <p>
   * For example, files with the suffix <code>mj2</code> will contain data of
   * type <code>video/mj2</code>.
   * 
   * @return the file suffix
   */
  public String getSuffix() {
    return suffix_;
  }

  /**
   * Returns the registered suffixes for this mime type, that identify files
   * containing data of this flavor. Note that the list includes the main suffix
   * returned by <code>getSuffix()</code>.
   * <p>
   * For example, files containing ISO Motion JPEG 2000 may have file suffixes
   * <code>mj2</code> and <code>mjp2</code>.
   * 
   * @return the registered file suffixes
   */
  public String[] getSuffixes() {
    return suffixes_.toArray(new String[suffixes_.size()]);
  }

  /**
   * Adds the suffix to the list of file suffixes.
   * 
   * @param suffix
   *          the suffix
   */
  public void addSuffix(String suffix) {
    if (suffix != null && !suffixes_.contains(suffix))
      suffixes_.add(suffix.trim().toLowerCase());
  }

  /**
   * Returns <code>true</code> if the mimetype supports the specified suffix.
   * 
   * @return <code>true</code> if the suffix is supported
   */
  public boolean supportsSuffix(String suffix) {
    return suffixes_.contains(suffix.toLowerCase());
  }

  /**
   * Returns the mime type description.
   * 
   * @return the description
   */
  public String getDescription() {
    return this.description_;
  }

  /**
   * Sets the mime type description.
   * 
   * @param description
   */
  public void setDescription(String description) {
    this.description_ = description;
  }

  /**
   * Returns the flavor of this mime type.
   * <p>
   * A flavor is a hint on a specialized variant of a general mime type. For
   * example, a dublin core file will have a mime type of <code>text/xml</code>.
   * Adding a flavor of <code>mpeg-7</code> gives an additional hint on the file
   * contents.
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
   * Sets the flavor of this mime type.
   * 
   * @param flavor
   *          the flavor
   */
  public void setFlavor(String flavor) {
    setFlavor(flavor, null);
  }

  /**
   * Sets the flavor of this mime type along with a flavor description.
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
   * Adds an equivalent type / subtype definition for this mime type.
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
      equivalents_ = new ArrayList<MIMEEquivalent>();
    equivalents_.add(new MIMEEquivalent(type, subtype));
  }

  /**
   * Returns the MimeType as a string of the form <code>type/subtype</code>
   */
  public String asString() {
    return type_ + "/" + subtype_;
  }

  /**
   * @see java.lang.Object#clone()
   */
  @Override
  public MimeType clone() throws CloneNotSupportedException {
    MimeType m = new MimeType(type_, subtype_, suffix_);
    m.equivalents_.addAll(equivalents_);
    m.suffixes_.addAll(suffixes_);
    m.flavor_ = flavor_;
    m.flavorDescription_ = flavorDescription_;
    return m;
  }

  /**
   * Returns <code>true</code> if this mime type is an equivalent for the
   * specified type and subtype.
   * <p>
   * For example, a gzipped file may have both of these mime types defined,
   * <code>application/x-compressed</code> or <code>application/x-gzip</code>.
   * 
   * @return <code>true</code> if this mime type is equal
   */
  public boolean isEquivalentTo(String type, String subtype) {
    if (type_.equalsIgnoreCase(type) && subtype_.equalsIgnoreCase(subtype))
      return true;
    if (equivalents_ != null) {
      for (MIMEEquivalent equivalent : equivalents_) {
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
    if (o instanceof MimeType) {
      MimeType m = (MimeType) o;
      return m.isEquivalentTo(type_, subtype_)
          || this.isEquivalentTo(m.getType(), m.getSubtype());
    }
    return super.equals(o);
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(MimeType m) {
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
   * Helper class to store type/subtype equivalents for a given mime type.
   * 
   * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
   */
  private class MIMEEquivalent {

    String type_;

    String subtype_;

    MIMEEquivalent(String type, String subtype) {
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
      if (o instanceof MIMEEquivalent) {
        MIMEEquivalent e = (MIMEEquivalent) o;
        return this.matches(e.getType(), e.getSubtype());
      }
      return super.equals(o);
    }

    boolean matches(String type, String subtype) {
      return type_.equalsIgnoreCase(type) && subtype_.equalsIgnoreCase(subtype);
    }

  }

}