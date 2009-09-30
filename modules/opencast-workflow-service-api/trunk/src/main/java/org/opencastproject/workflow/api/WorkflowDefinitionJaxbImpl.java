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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * FIXME -- Add javadocs
 */
@XmlType(name="workflow-definition", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="workflow-definition", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowDefinitionJaxbImpl implements WorkflowDefinition {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionJaxbImpl.class);

  public WorkflowDefinitionJaxbImpl() {}

  @XmlID
  @XmlAttribute()
  private String id;

  @XmlElement(name="title")
  private String title;

  @XmlElement(name="description")
  private String description;

  @XmlElementWrapper(name="operations")
  private List<WorkflowOperation> operations;
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }
  
  public void setTitle(String title) {
    this.title = title;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  public List<WorkflowOperation> getOperations() {
    return operations;
  }

  public void setOperations(List<WorkflowOperation> operations) {
    this.operations = operations;
  }

  /**
   * Since we are posting workflow definitions as one post parameter in a multi-parameter post, we can not rely on
   * "automatic" JAXB deserialization.  We therefore need to provide a static valueOf(String) method to transform an
   * XML string into a WorkflowDefinition.
   * 
   * @param xmlString The xml describing the workflow
   * @return A {@link WorkflowDefinitionJaxbImpl} instance based on xmlString
   * @throws Exception If there is a problem marshalling the {@link WorkflowDefinitionJaxbImpl} from XML.
   */
  public static WorkflowDefinitionJaxbImpl valueOf(String xmlString) throws Exception {
    WorkflowDefinitionJaxbImpl impl = new WorkflowDefinitionJaxbImpl();
    List<WorkflowOperation> operations = new ArrayList<WorkflowOperation>();
    impl.setOperations(operations);

    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    InputSource is = new InputSource();
    is.setCharacterStream(new StringReader(xmlString));
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
      operations.add(new WorkflowOperationImpl(name, description, failOnError));
    }
    return impl;
  }
}

