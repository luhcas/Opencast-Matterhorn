/**
 *  Copyright 2009, 2010 The Regents of the University of California
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

package org.opencastproject.scheduler.impl;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opencastproject.scheduler.api.Metadata;
import org.opencastproject.scheduler.impl.IncompleteDataException;

public abstract class AbstractEvent {
  private static final Logger logger = LoggerFactory.getLogger(AbstractEvent.class);
  
  public Hashtable<String, String> metadataTable;
  
  protected EntityManagerFactory emf = null;
  
  public String generateId() {
    return UUID.randomUUID().toString();
  }
  
  
  public void buildMetadataTable (List<Metadata> metadata) {
    if (metadataTable == null) metadataTable = new Hashtable<String, String>(); // Buffer metadata in Hashtable for quick
    if (metadata == null) return;
    for (Metadata data : metadata) {
      if (data.getKey() != null && data.getValue() != null){
        metadataTable.put(data.getKey(), data.getValue()); // Overwrite with event specific data
      }
    }
  }
  
  public String getValue (String key) throws IncompleteDataException {
   if (metadataTable == null) throw new IncompleteDataException ("Metadata table not generated");
   return metadataTable.get(key);
   
  }
  
  public Set<String> getKeySet () throws IncompleteDataException {
    if (metadataTable == null) throw new IncompleteDataException ("Metadata table not generated");
    return metadataTable.keySet();
  }
  
  public Date getValueAsDate (String key) throws IncompleteDataException {
    if (metadataTable == null) throw new IncompleteDataException ("Metadata table not generated");
    if (! containsKey(key)) {
      logger.warn("Could not convert to date, because of missing entry for {}.", key);
      return null;
    }
    try {
      return new Date(Long.parseLong(metadataTable.get(key)));
    } catch (Exception e) {
      logger.warn("Could not parse value of {}: {}", key, metadataTable.get(key));
      return null;
    }
  }
  
  public boolean containsKey (String key)  throws IncompleteDataException{
    if (metadataTable == null) throw new IncompleteDataException ("Metadata table not generated");
    return metadataTable.containsKey(key);
  }

  public void setEntityManagerFactory (EntityManagerFactory emf) {
    this.emf = emf;
  }
  
  public void deleteMetadataTable() {
    metadataTable = null;
  }
  
  public abstract void addMetadata(Metadata m);
  
  public abstract void removeMetadata(Metadata m);
}
