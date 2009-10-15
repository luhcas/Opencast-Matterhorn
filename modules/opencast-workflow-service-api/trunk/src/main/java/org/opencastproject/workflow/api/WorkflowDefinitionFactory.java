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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Factory used to obtain
 */
public class WorkflowDefinitionFactory {

  /** The singleton instance for this factory */
  private static WorkflowDefinitionFactory instance = null;

  /**
   * Returns an instance of the {@link WorkflowDefinitionFactory}.
   * 
   * @return a factory
   */
  public static WorkflowDefinitionFactory getInstance() {
    if (instance == null)
      instance = new WorkflowDefinitionFactory();
    return instance;
  }

  /**
   * Loads a workflow definition from the given input stream.
   * 
   * @param is
   *          the input stream
   * @return the workflow definition
   * @throws IOException
   *           if reading the input stream fails
   * @throws Exception
   *           if creating the workflow definition fails
   */
  public WorkflowDefinition parse(InputStream is) throws IOException, Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();
    String line = null;
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    br.close();
    return parse(sb.toString());
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
  public WorkflowDefinition parse(String definition) throws Exception {
    return WorkflowDefinitionJaxbImpl.valueOf(definition);
  }

}