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
package org.opencastproject.scheduler.endpoint;

import org.apache.commons.io.IOUtils;
import org.opencastproject.scheduler.impl.EventImpl;
import org.opencastproject.scheduler.impl.MetadataImpl;

import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;

public final class SchedulerBuilder {

  /** Builder singleton */
  private static SchedulerBuilder instance = null;

  protected JAXBContext jaxbContext = null;

  /**
   * Set up the JAXBContext.
   * 
   */
  private SchedulerBuilder() throws JAXBException {
    jaxbContext = JAXBContext
            .newInstance("org.opencastproject.scheduler.impl", SchedulerBuilder.class.getClassLoader());
  }

  /**
   * Returns an instance of the {@link SchedulerBuilder}
   * 
   * @return a factory
   */
  public static SchedulerBuilder getInstance() {
    if (instance == null) {
      try {
        instance = new SchedulerBuilder();
      } catch (JAXBException e) {
        throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
      }
    }
    return instance;
  }

  public EventImpl parseEvent(String in) throws Exception {
    return parseEvent(IOUtils.toInputStream(in, "UTF8"));
  }

  public EventImpl parseEvent(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    return unmarshaller.unmarshal(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in), EventImpl.class)
            .getValue();
  }

  public String marshallEvent(EventImpl e) throws Exception {
    Marshaller marshaller = jaxbContext.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(e, writer);
    return writer.toString();
  }

  public MetadataImpl parseMetadata(String in) throws Exception {
    return parseMetadata(IOUtils.toInputStream(in, "UTF8"));
  }

  public MetadataImpl parseMetadata(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    return unmarshaller.unmarshal(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in),
            MetadataImpl.class).getValue();
  }
}
