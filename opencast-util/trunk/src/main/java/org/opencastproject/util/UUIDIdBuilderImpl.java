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

import java.util.UUID;

/**
 * Default implementation of an id builder. This implementation yields for a
 * distributed id generator that will create unique ids for the system, although
 * created on various nodes.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: UUIDIdBuilderImpl.java 2500 2009-02-22 14:31:25Z wunden $
 */
public class UUIDIdBuilderImpl implements IdBuilder {

  /**
   * Creates a new handle builder.
   */
  public UUIDIdBuilderImpl() {
  }

  /**
   * @see org.opencastproject.util.IdBuilder#createNew()
   */
  public String createNew() {
    return UUID.randomUUID().toString();
  }

  /**
   * @see org.opencastproject.util.IdBuilder#fromString(String)
   */
  public String fromString(String id) throws IllegalArgumentException {
    if (id == null)
      throw new IllegalArgumentException("Argument 'id' is null");
    try {
      UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      throw e;
    }
    return id;
  }

}