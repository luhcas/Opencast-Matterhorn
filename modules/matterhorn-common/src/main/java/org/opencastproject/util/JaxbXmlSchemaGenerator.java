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
package org.opencastproject.util;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

/**
 * Provides utility methods for transforming a {@link JAXBContext} into an XML schema.
 */
public final class JaxbXmlSchemaGenerator {

  /** Private constructor to disable creation of new instances. */
  private JaxbXmlSchemaGenerator() {
  }

  /**
   * Builds an xml schema from a JAXBContext.
   * 
   * @param jaxbContext
   *          the jaxb context
   * @return the xml as a string
   * @throws IOException
   *           if the JAXBContext can not be transformed into an xml schema
   */
  public static String getXmlSchema(JAXBContext jaxbContext) throws IOException {
    final StringWriter writer = new StringWriter();
    jaxbContext.generateSchema(new SchemaOutputResolver() {
      @Override
      public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
        StreamResult streamResult = new StreamResult(writer);
        streamResult.setSystemId("");
        return streamResult;
      }
    });
    return writer.toString();
  }

}