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
package org.opencastproject.workflow.api;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Provides a mechanism to build a workflow definition from an xml inputstream.
 */
public class WorkflowDefinitionFactory {

  /** The singleton instance for this factory */
  private static WorkflowDefinitionFactory instance = null;

  protected JAXBContext jaxbContext = null;
  
  private WorkflowDefinitionFactory() throws JAXBException {
    jaxbContext= JAXBContext.newInstance("org.opencastproject.workflow.api", WorkflowDefinitionFactory.class.getClassLoader());
  }
  
  /**
   * Returns an instance of the {@link WorkflowDefinitionFactory}.
   * 
   * @return a factory
   */
  public static WorkflowDefinitionFactory getInstance() {
    if (instance == null) {
      try {
        instance = new WorkflowDefinitionFactory();
      } catch (JAXBException e) {
        throw new RuntimeException(e);
      }
    }
    return instance;
  }

  /**
   * Loads a workflow definition from the given input stream.
   * 
   * @param is
   *          the input stream
   * @return the workflow definition
   * @throws Exception
   *           if creating the workflow definition fails
   */
  public WorkflowDefinition parse(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    return unmarshaller.unmarshal(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in),
            WorkflowDefinitionImpl.class).getValue();
  }
  
  /**
   * Loads a workflow definition from the xml fragement.
   * 
   * @param definition
   *          xml fragment of the workflow definition
   * @return the workflow definition
   * @throws Exception
   *           if creating the workflow definition fails
   */
  public WorkflowDefinition parse(String in) throws Exception {
    return parse(IOUtils.toInputStream(in, "UTF8"));
  }
  
}
