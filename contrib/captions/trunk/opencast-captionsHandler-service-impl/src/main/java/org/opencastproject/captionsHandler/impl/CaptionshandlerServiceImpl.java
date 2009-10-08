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
package org.opencastproject.captionsHandler.impl;

import org.opencastproject.captionsHandler.api.CaptionshandlerEntity;
import org.opencastproject.captionsHandler.api.CaptionshandlerService;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/**
 * FIXME -- Add javadocs
 */
public class CaptionshandlerServiceImpl implements CaptionshandlerService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(CaptionshandlerServiceImpl.class);
  
  Map<String, CaptionshandlerEntity> map;
  
  public CaptionshandlerServiceImpl() {
    map = new HashMap<String, CaptionshandlerEntity>();
    CaptionshandlerEntityImpl entity = new CaptionshandlerEntityImpl();
    entity.setId("1");
    entity.setTitle("Test Title");
    entity.setDescription("Test Description");
    map.put("1", entity);
  }
  
  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.captionsHandler.api.CaptionshandlerService#getEntity(java.lang.String)
   */
  public CaptionshandlerEntity getCaptionshandlerEntity(String id) {
    CaptionshandlerEntity entity = map.get(id);
    logger.info("returning " + entity + " for id=" + id);
    return entity;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.captionsHandler.api.CaptionshandlerService#setObject(java.lang.String, org.opencastproject.captionsHandler.api.CaptionshandlerEntity)
   */
  public void saveCaptionshandlerEntity(CaptionshandlerEntity entity) {
    String id = entity.getId();
    logger.info("setting id=" + id + " to " + entity);
    map.put(id, entity);
  }

}

