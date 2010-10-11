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
package org.opencastproject.remote.impl;


import org.opencastproject.remote.api.Job;

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
 * Provides a mechanism to transform {@link Job}s to and from xml.
 */
public class JobBuilder {

  /** The singleton instance for this factory */
  private static JobBuilder instance = null;

  protected JAXBContext jaxbContext = null;
  
  private JobBuilder() throws JAXBException {
    StringBuilder sb = new StringBuilder();
    sb.append("org.opencastproject.mediapackage");
    sb.append(":org.opencastproject.mediapackage.attachment");
    sb.append(":org.opencastproject.mediapackage.track");
    sb.append(":org.opencastproject.remote.impl");
    jaxbContext= JAXBContext.newInstance(sb.toString(), JobBuilder.class.getClassLoader());
  }
  
  /**
   * Returns an instance of the {@link JobBuilder}.
   * 
   * @return a factory
   */
  public static JobBuilder getInstance() {
    if (instance == null) {
      try {
        instance = new JobBuilder();
      } catch (JAXBException e) {
        throw new RuntimeException(e.getLinkedException() != null ? e.getLinkedException() : e);
      }
    }
    return instance;
  }

  /**
   * Loads a receipt from the given input stream.
   * 
   * @param in
   *          the input stream
   * @return the receipt
   * @throws Exception
   *           if creating the receipt fails
   */
  public Job parseJob(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    return unmarshaller.unmarshal(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in),
            JobImpl.class).getValue();
  }
  
  /**
   * Loads a receipt from the xml stream.
   * 
   * @param in
   *          xml stream of the receipt
   * @return the receipt
   * @throws Exception
   *           if creating the receipt fails
   */
  public Job parseJob(String in) throws Exception {
    InputStream is = null;
    try {
      is = IOUtils.toInputStream(in, "UTF-8");
      return parseJob(is);
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
  public String toXml(Job receipt) throws Exception {
    Marshaller marshaller = jaxbContext.createMarshaller();
    Writer writer = new StringWriter();
    marshaller.marshal(receipt, writer);
    return writer.toString();
  }
}
