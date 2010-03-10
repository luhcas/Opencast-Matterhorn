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
package org.opencastproject.composer.impl;


import org.opencastproject.composer.api.Receipt;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Provides a mechanism to transform {@link Receipt}s to and from xml.
 */
public class ReceiptBuilder {

  /** The singleton instance for this factory */
  private static ReceiptBuilder instance = null;

  protected JAXBContext jaxbContext = null;
  
  private ReceiptBuilder() throws JAXBException {
    StringBuilder sb = new StringBuilder();
    sb.append("org.opencastproject.media.mediapackage");
    sb.append(":org.opencastproject.composer.impl.endpoint");
    jaxbContext= JAXBContext.newInstance(sb.toString(), ReceiptBuilder.class.getClassLoader());
  }
  
  /**
   * Returns an instance of the {@link ReceiptBuilder}.
   * 
   * @return a factory
   */
  public static ReceiptBuilder getInstance() {
    if (instance == null) {
      try {
        instance = new ReceiptBuilder();
      } catch (JAXBException e) {
        throw new RuntimeException(e.getLinkedException() != null ? e.getLinkedException() : e);
      }
    }
    return instance;
  }

  /**
   * Loads a receipt from the given input stream.
   * 
   * @param is
   *          the input stream
   * @return the receipt
   * @throws Exception
   *           if creating the receipt fails
   */
  public Receipt parseReceipt(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    return unmarshaller.unmarshal(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in),
            Receipt.class).getValue();
  }
  
  /**
   * Loads a receipt from the xml fragement.
   * 
   * @param definition
   *          xml fragment of the receipt
   * @return the receipt
   * @throws Exception
   *           if creating the receipt fails
   */
  public Receipt parseReceipt(String in) throws Exception {
    InputStream is = null;
    try {
      is = IOUtils.toInputStream(in, "UTF-8");
      return parseReceipt(is);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  /**
   * Serializes a Receipt to xml.
   * 
   * @param receipt the receipt to serialize
   * @return the xml fragment
   * @throws Exception
   */
  public String toXml(Receipt receipt) throws Exception {
    Marshaller marshaller = jaxbContext.createMarshaller();
    Writer writer = new StringWriter();
    marshaller.marshal(receipt, writer);
    return writer.toString();
  }
}
