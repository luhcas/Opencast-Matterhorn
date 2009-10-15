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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * Provides a mechanism to build a workflow definition from an xml inputstream.
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
   * @throws Exception
   *           if creating the workflow definition fails
   */
  public WorkflowDefinition parse(InputStream in) throws Exception {
    WorkflowDefinitionImpl impl = new WorkflowDefinitionImpl();
    List<WorkflowOperationDefinition> operations = new ArrayList<WorkflowOperationDefinition>();
    impl.setOperations(operations);

    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    InputSource is = new InputSource();
    is.setCharacterStream(new InputStreamReader(in, "UTF8"));
    Document doc = db.parse(is);
    XPathFactory factory = XPathFactory.newInstance();
    XPath xpath = factory.newXPath();
    impl.setId((String)xpath.compile("/workflow-definition[@id]/text()").evaluate(doc, XPathConstants.STRING));
    impl.setTitle((String)xpath.compile("/workflow-definition/title/text()").evaluate(doc, XPathConstants.STRING));
    impl.setDescription((String)xpath.compile("/workflow-definition/description/text()").evaluate(doc, XPathConstants.STRING));
    XPathExpression operationXpath = xpath.compile("/workflow-definition/operations/operation");
    NodeList operationList = (NodeList)operationXpath.evaluate(doc, XPathConstants.NODESET);
    for(int i=0; i<operationList.getLength(); i++) {
      Element operationNode = (Element)operationList.item(i);
      String name = operationNode.getAttribute("name");
      String description = operationNode.getAttribute("description");
      boolean failOnError = Boolean.TRUE.toString().equals(operationNode.getAttribute("fail-on-error"));
      operations.add(new WorkflowOperationDefinitionImpl(name, description, failOnError));
    }
    return impl;
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
