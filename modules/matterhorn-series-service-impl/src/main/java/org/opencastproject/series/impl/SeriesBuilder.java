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
package org.opencastproject.series.impl;

import org.opencastproject.series.api.Series;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;

public final class SeriesBuilder {

  /** Builder singleton */
  private static SeriesBuilder instance = null;

  protected JAXBContext jaxbContext = null;

  /**
   * Set up the JAXBContext.
   * 
   */
  private SeriesBuilder() throws JAXBException {
    jaxbContext = JAXBContext.newInstance("org.opencastproject.series.impl", SeriesBuilder.class.getClassLoader());
  }

  /**
   * Returns an instance of the {@link SeriesBuilder}
   * 
   * @return a factory
   */
  public static SeriesBuilder getInstance() {
    if (instance == null) {
      try {
        instance = new SeriesBuilder();
      } catch (JAXBException e) {
        throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
      }
    }
    return instance;
  }

  public SeriesImpl parseSeriesImpl(String in) throws Exception {
    return parseSeriesImpl(IOUtils.toInputStream(in, "UTF-8"));
  }

  public SeriesImpl parseSeriesImpl(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    return unmarshaller
            .unmarshal(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in), SeriesImpl.class)
            .getValue();
  }

  public SeriesMetadataImpl parseSeriesMetadataImpl(String in) throws Exception {
    return parseSeriesMetadataImpl(IOUtils.toInputStream(in, "UTF-8"));
  }

  public SeriesMetadataImpl parseSeriesMetadataImpl(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    return unmarshaller.unmarshal(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in),
            SeriesMetadataImpl.class).getValue();
  }

  public String marshallSeries(Series s) throws Exception {
    Marshaller marshaller = jaxbContext.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(s, writer);
    return writer.toString();
  }

}
