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
package org.opencastproject.integrationtest;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.w3c.dom.Document;

/**
 * Test utilities
 * @author jamiehodge
 *
 */

public class Utils {

  public static Document parseXml(String doc) throws Exception {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(IOUtils.toInputStream(doc));
   }
  
  public static Object xPath(Document document, String path, QName returnType)
    throws XPathExpressionException, TransformerException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    xPath.setNamespaceContext(new UniversalNamespaceResolver(document));
    return xPath.compile(path).evaluate(document, returnType);
  }
  
  public static Boolean xPathExists(Document document, String path) throws Exception {
    return (Boolean) xPath(document, path, XPathConstants.BOOLEAN);
  }
  
  public static JSONObject parseJson(String doc) throws Exception {
    return (JSONObject) JSONValue.parse(doc);
  }
  
  public static String schedulerEvent(Integer duration) throws Exception {
	Long start = System.currentTimeMillis() + 60000;
	Long end = start + duration;
	String event = IOUtils.toString(Utils.class.getClassLoader().getResourceAsStream("scheduler-event.xml"));
	return event
		.replace("@@start@@", start.toString())
		.replace("@@end@@", end.toString())
		.replace("@@duration@@", duration.toString());
	}
  
}
