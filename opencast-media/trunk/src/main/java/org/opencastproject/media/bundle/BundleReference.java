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
 * A <code>BundleElementRef</code> provides means of pointing to other elements
 * in the bundle.
 * <p>
 * A metadata catalog could for example contain a reference to the track that
 * was used to extract the data contained in it.
 * </p>
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface BundleReference {

  static final String TYPE_BUNDLE = "bundle";
  static final String TYPE_TRACK = "track";
  static final String TYPE_CATALOG = "track";
  static final String TYPE_ATTACHMENT = "track";
  static final String TYPE_SERIES = "series";
  static final String SELF = "self";

  /**
   * Returns the reference type.
   * <p>
   * There is a list of well known types describing bundle elements:
   * <ul>
   * <li><code>bundle</code> a reference to the parent bundle</li>
   * <li><code>track</code> referes to a track inside the bundle</li>
   * <li><code>catalog</code> referes to a catalog inside the bundle</li>
   * <li><code>attachment</code> referes to an attachment inside the bundle</li>
   * <li><code>series</code> referes to a series</li>
   * </ul>
   * 
   * @return the reference type
   */
  String getType();

  /**
   * Returns the reference identifier.
   * <p>
   * The identifier will usually refer to the id of the bundle element, should
   * the reference point to an element inside the bundle (see
   * {@link BundleElement#getIdentifier()}).
   * <p>
   * In case of a reference to another bundle, this will reflect the bundle id
   * (see {@link Bundle#getIdentifier()}) or <code>self</code> if it refers to
   * the parent bundle.
   * 
   * @return the reference identifier
   */
  String getIdentifier();

  /**
   * Returns <code>true</code> if this reference matches <code>reference</code>
   * by means of type and identifier.
   * 
   * @param reference
   *          the bundle reference
   * @return <code>true</code> if the reference matches
   */
  boolean matches(BundleReference reference);

}