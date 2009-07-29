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

/**
 * Default implementation for a {@link BundleReference}.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class BundleReferenceImpl implements BundleReference {

  /** The reference identifier */
  protected String identifier = null;

  /** The reference type */
  protected String type = null;

  /** External representation */
  private String externalForm = null;

  /**
   * Creates a reference to the containing bundle (<code>self</code>).
   */
  public BundleReferenceImpl() {
    type = TYPE_BUNDLE;
    identifier = SELF;
  }

  /**
   * Creates a reference to the specified bundle.
   * 
   * @param bundle
   *          the bundle to refer to
   */
  public BundleReferenceImpl(Bundle bundle) {
    if (bundle == null)
      throw new IllegalArgumentException("Parameter bundle must not be null");
    type = TYPE_BUNDLE;
    identifier = bundle.getIdentifier().toString();
  }

  /**
   * Creates a reference to the specified bundle element.
   * <p>
   * Note that the referenced element must already be part of the bundle,
   * otherwise a <code>BundleException</code> will be thrown as the object
   * holding this reference is added to the bundle.
   * 
   * @param bundleElement
   *          the bundle element to refer to
   */
  public BundleReferenceImpl(BundleElement bundleElement) {
    if (bundleElement == null)
      throw new IllegalArgumentException(
          "Parameter bundle element must not be null");
    type = bundleElement.getElementType().toString().toLowerCase();
    identifier = bundleElement.getIdentifier();
  }

  /**
   * Creates a reference to the entity identified by <code>type</code> and
   * <code>identifier</code>.
   * 
   * @param type
   *          the reference type
   * @param identifier
   *          the reference identifier
   */
  public BundleReferenceImpl(String type, String identifier) {
    if (type == null)
      throw new IllegalArgumentException("Parameter type must not be null");
    if (identifier == null)
      throw new IllegalArgumentException(
          "Parameter identifier must not be null");
    this.type = type;
    this.identifier = identifier;
  }

  /**
   * Returns a bundle reference from the given string.
   * 
   * @return the bundle reference
   * @throws IllegalArgumentException
   *           if the string is malformed
   */
  public static BundleReference fromString(String reference)
      throws IllegalArgumentException {
    if (reference == null)
      throw new IllegalArgumentException("Reference is null");

    // Check for special reference
    if ("self".equals(reference))
      return new BundleReferenceImpl("bundle", "self");

    // Check syntax
    String[] parts = reference.split(":");
    if (parts.length != 2)
      throw new IllegalArgumentException("Reference " + reference
          + " is malformed");

    return new BundleReferenceImpl(parts[0], parts[1]);
  }

  /**
   * @see org.opencastproject.media.bundle.BundleReference#getIdentifier()
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * @see org.opencastproject.media.bundle.BundleReference#getType()
   */
  public String getType() {
    return type;
  }

  /**
   * @see org.opencastproject.media.bundle.BundleReference#matches(org.opencastproject.media.bundle.BundleReference)
   */
  public boolean matches(BundleReference reference) {
    if (reference == null)
      throw new IllegalArgumentException("Argument reference must not be null");
    if (!type.equals(reference.getType()))
      return false;
    if (identifier.equals(reference.getIdentifier()))
      return true;
    else if (identifier.equals("*"))
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
    if (obj == null)
      return false;
    return externalForm.equals(obj.toString());
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (externalForm == null) {
      StringBuffer buf = new StringBuffer();
      if (TYPE_BUNDLE.equals(type) && SELF.equals(identifier)) {
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

}