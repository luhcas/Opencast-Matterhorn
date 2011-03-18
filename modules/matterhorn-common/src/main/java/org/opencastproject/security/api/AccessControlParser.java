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
package org.opencastproject.security.api;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Marshals and unmarshals {@link AccessControlList}s to/from XML.
 */
public final class AccessControlParser {
  private static final JAXBContext jaxbContext;

  static {
    try {
      jaxbContext = JAXBContext.newInstance("org.opencastproject.security.api",
              AccessControlParser.class.getClassLoader());
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  public static AccessControlList parseAcl(String serializedForm) throws IOException {
    return parseAcl(IOUtils.toInputStream(serializedForm, "UTF-8"));
  }

  /**
   * Unmarshals an ACL from an xml input stream.
   * 
   * @param in
   *          the xml input stream
   * @return the acl
   * @throws IOException
   *           if there is a problem unmarshaling the stream
   */
  public static AccessControlList parseAcl(InputStream in) throws IOException {
    Unmarshaller unmarshaller;
    try {
      unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in),
              AccessControlList.class).getValue();
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Serializes an AccessControlList to its XML form.
   * 
   * @param acl
   *          the access control list
   * @return the xml as a string
   * @throws IOException
   *           if there is a problem marshaling the xml
   */
  public static String toXml(AccessControlList acl) throws IOException {
    try {
      Marshaller marshaller = jaxbContext.createMarshaller();
      Writer writer = new StringWriter();
      marshaller.marshal(acl, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IOException(e);
    }
  }

}
