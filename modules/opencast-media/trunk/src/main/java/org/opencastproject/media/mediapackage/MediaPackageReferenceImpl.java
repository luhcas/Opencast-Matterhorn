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

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Default implementation for a {@link MediaPackageReference}.
 */
public class MediaPackageReferenceImpl implements MediaPackageReference {

  /** Convenience reference that matches any media package */
  public static final MediaPackageReference ANY_MEDIAPACKAGE = new MediaPackageReferenceImpl(TYPE_MEDIAPACKAGE, ANY);

  /** Convenience reference that matches the current media package */
  public static final MediaPackageReference SELF_MEDIAPACKAGE = new MediaPackageReferenceImpl(TYPE_MEDIAPACKAGE, SELF);

  /** Convenience reference that matches any series */
  public static final MediaPackageReference ANY_SERIES = new MediaPackageReferenceImpl(TYPE_SERIES, "*");

  /** The reference identifier */
  protected String identifier = null;

  /** The reference type */
  protected String type = null;

  /** External representation */
  private String externalForm = null;

  /**
   * Creates a reference to the containing media package (<code>self</code>).
   */
  public MediaPackageReferenceImpl() {
    type = TYPE_MEDIAPACKAGE;
    identifier = SELF;
  }

  /**
   * Creates a reference to the specified media package.
   * 
   * @param mediaPackage
   *          the media package to refer to
   */
  public MediaPackageReferenceImpl(MediaPackage mediaPackage) {
    if (mediaPackage == null)
      throw new IllegalArgumentException("Parameter media package must not be null");
    type = TYPE_MEDIAPACKAGE;
    if (mediaPackage.getIdentifier() != null)
      identifier = mediaPackage.getIdentifier().toString();
    else
      identifier = SELF;
  }

  /**
   * Creates a reference to the specified media package element.
   * <p>
   * Note that the referenced element must already be part of the media package, otherwise a
   * <code>MediaPackageException</code> will be thrown as the object holding this reference is added to the media
   * package.
   * 
   * @param mediaPackageElement
   *          the media package element to refer to
   */
  public MediaPackageReferenceImpl(MediaPackageElement mediaPackageElement) {
    if (mediaPackageElement == null)
      throw new IllegalArgumentException("Parameter media package element must not be null");
    type = mediaPackageElement.getElementType().toString().toLowerCase();
    identifier = mediaPackageElement.getIdentifier();
  }

  /**
   * Creates a reference to the entity identified by <code>type</code> and <code>identifier</code>.
   * 
   * @param type
   *          the reference type
   * @param identifier
   *          the reference identifier
   */
  public MediaPackageReferenceImpl(String type, String identifier) {
    if (type == null)
      throw new IllegalArgumentException("Parameter type must not be null");
    if (identifier == null)
      throw new IllegalArgumentException("Parameter identifier must not be null");
    this.type = type;
    this.identifier = identifier;
  }

  /**
   * Returns a media package reference from the given string.
   * 
   * @return the media package reference
   * @throws IllegalArgumentException
   *           if the string is malformed
   */
  public static MediaPackageReference fromString(String reference) throws IllegalArgumentException {
    if (reference == null)
      throw new IllegalArgumentException("Reference is null");

    // Check for special reference
    if ("self".equals(reference))
      return new MediaPackageReferenceImpl(MediaPackageReference.TYPE_MEDIAPACKAGE, "self");

    // Check syntax
    String[] parts = reference.split(":");
    if (parts.length != 2)
      throw new IllegalArgumentException("Reference " + reference + " is malformed");

    return new MediaPackageReferenceImpl(parts[0], parts[1]);
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageReference#getIdentifier()
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageReference#getType()
   */
  public String getType() {
    return type;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageReference#matches(org.opencastproject.media.mediapackage.MediaPackageReference)
   */
  public boolean matches(MediaPackageReference reference) {
    if (reference == null)
      return false;
    if (!type.equals(reference.getType()))
      return false;
    if (identifier.equals(reference.getIdentifier()))
      return true;
    else if (identifier.equals("*") || reference.getIdentifier().equals("*"))
      return true;
    else if (identifier.equals("self") || reference.getIdentifier().equals("self"))
      return true;
    return false;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof MediaPackageReference))
      return false;
    MediaPackageReference ref = (MediaPackageReference) obj;
    return type.equals(ref.getType()) && identifier.equals(ref.getIdentifier());
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (externalForm == null) {
      StringBuffer buf = new StringBuffer();
      if (TYPE_MEDIAPACKAGE.equals(type) && SELF.equals(identifier)) {
        buf.append("self");
      } else {
        buf.append(type);
        buf.append(":");
        buf.append(identifier);
      }
      externalForm = buf.toString();
    }
    return externalForm;
  }

  public static class Adapter extends XmlAdapter<String, MediaPackageReference> {
    @Override
    public String marshal(MediaPackageReference ref) throws Exception {
      if(ref == null) return null;
      return ref.toString();
    }
    @Override
    public MediaPackageReference unmarshal(String ref) throws Exception {
      if(ref == null) return null;
      return MediaPackageReferenceImpl.fromString(ref);
    }
  }
}
