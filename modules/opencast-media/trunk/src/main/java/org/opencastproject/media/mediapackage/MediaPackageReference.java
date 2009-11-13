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

/**
 * A <code>MediaPackageElementRef</code> provides means of pointing to other elements in the media package.
 * <p>
 * A metadata catalog could for example contain a reference to the track that was used to extract the data contained in
 * it.
 * </p>
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageReference.java 1736 2008-12-19 11:19:43Z wunden $
 */
public interface MediaPackageReference {

  public static final String TYPE_MEDIAPACKAGE = "mediapackage";
  public static final String TYPE_TRACK = "track";
  public static final String TYPE_CATALOG = "catalog";
  public static final String TYPE_ATTACHMENT = "attachment";
  public static final String TYPE_SERIES = "series";
  public static final String SELF = "self";
  public static final String ANY = "*";

  /**
   * Returns the reference type.
   * <p>
   * There is a list of well known types describing media package elements:
   * <ul>
   * <li><code>mediapackage</code> a reference to the parent media package</li>
   * <li><code>track</code> referes to a track inside the media package</li>
   * <li><code>catalog</code> referes to a catalog inside the media package</li>
   * <li><code>attachment</code> referes to an attachment inside the media package</li>
   * <li><code>series</code> referes to a series</li>
   * </ul>
   * 
   * @return the reference type
   */
  String getType();

  /**
   * Returns the reference identifier.
   * <p>
   * The identifier will usually refer to the id of the media package element, should the reference point to an element
   * inside the media package (see {@link MediaPackageElement#getIdentifier()}).
   * <p>
   * In case of a reference to another media package, this will reflect the media package id (see
   * {@link MediaPackage#getIdentifier()}) or <code>self</code> if it refers to the parent media package.
   * 
   * @return the reference identifier
   */
  String getIdentifier();

  /**
   * Returns <code>true</code> if this reference matches <code>reference</code> by means of type and identifier.
   * 
   * @param reference
   *          the media package reference
   * @return <code>true</code> if the reference matches
   */
  boolean matches(MediaPackageReference reference);

}
