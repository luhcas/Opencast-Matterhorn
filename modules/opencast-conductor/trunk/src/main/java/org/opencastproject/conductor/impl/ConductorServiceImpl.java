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
package org.opencastproject.conductor.impl;

import org.opencastproject.conductor.api.ConductorService;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowDefinitionList;
import org.opencastproject.workflow.api.WorkflowDefinitionListImpl;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * This is the default implementation of the conductor service.
 */
public class ConductorServiceImpl implements ConductorService {
  private static final Logger logger = LoggerFactory.getLogger(ConductorServiceImpl.class);
  protected HashMap<String, WorkflowDefinition> defs = new HashMap<String, WorkflowDefinition>();
  protected JAXBContext jaxbContext;
  public void activate(ComponentContext context) {
    logger.info("init()");
    try {
      jaxbContext= JAXBContext.newInstance("org.opencastproject.workflow.api", WorkflowDefinition.class.getClassLoader());
      InputStream distOnly = ConductorServiceImpl.class.getClassLoader().getResourceAsStream("/workflows/distribute-only.xml");
      InputStream composeAndDist = ConductorServiceImpl.class.getClassLoader().getResourceAsStream("/workflows/compose-and-distribute.xml");
      loadWorkflowDefinition(distOnly);
      loadWorkflowDefinition(composeAndDist);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected void loadWorkflowDefinition(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(in));
    WorkflowDefinitionImpl def = unmarshaller.unmarshal(doc, WorkflowDefinitionImpl.class).getValue();
    defs.put(def.getTitle(), def);
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.conductor.api.ConductorService#getWorkflowDefinitions()
   */
  public WorkflowDefinitionList getWorkflowDefinitions() {
    WorkflowDefinitionListImpl list = new WorkflowDefinitionListImpl();
    list.getWorkflowDefinition().addAll(defs.values());
    return list;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.conductor.api.ConductorService#getWorkflowDefinitionByTitle(java.lang.String)
   */
  public WorkflowDefinition getWorkflowDefinitionByTitle(String name) {
    return defs.get(name);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.conductor.api.ConductorService#addWorkflowDefinition(org.opencastproject.workflow.api.WorkflowDefinition)
   */
  public void addWorkflowDefinition(WorkflowDefinition def) {
    defs.put(def.getTitle(), def);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.conductor.api.ConductorService#removeWorkflowDefinition(java.lang.String)
   */
  public WorkflowDefinition removeWorkflowDefinition(String title) {
    for(Iterator<Entry<String, WorkflowDefinition>> iter = defs.entrySet().iterator(); iter.hasNext();) {
      Entry<String, WorkflowDefinition> entry = iter.next();
      WorkflowDefinition def = entry.getValue();
      if(title.equals(def.getTitle())) {
        iter.remove();
        return def;
      }
    }
    return null;
  }

}
