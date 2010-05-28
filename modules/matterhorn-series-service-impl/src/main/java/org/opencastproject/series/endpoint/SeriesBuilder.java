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
package org.opencastproject.series.endpoint;

import org.apache.commons.io.IOUtils;
import org.opencastproject.series.api.Series;

import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;

public class SeriesBuilder {

  /** Builder singleton */
  private static SeriesBuilder instance = null;
  
  protected JAXBContext jaxbContext = null;
  
  /**
   *  Set up the JAXBContext.
   *
   */
  private SeriesBuilder() throws JAXBException {
    jaxbContext = JAXBContext.newInstance("org.opencastproject.series.endpoint", SeriesBuilder.class.getClassLoader());
  }
  
  /**
   * Returns an instance of the {@link SeriesBuilder}
   *
   * @return a factory
   */
  public static SeriesBuilder getInstance() {
    if(instance == null){
      try {
        instance = new SeriesBuilder();
      }
      catch (JAXBException e) {
        throw new RuntimeException(e.getLinkedException() != null ? e.getLinkedException() : e);
      }
    }
    return instance;
  }
  
  public SeriesJaxbImpl parseSeriesJaxbImpl(String in) throws Exception {
    return parseSeriesJaxbImpl(IOUtils.toInputStream(in, "UTF8"));
  }
  
  public SeriesJaxbImpl parseSeriesJaxbImpl(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    return unmarshaller.unmarshal(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in),
                                  SeriesJaxbImpl.class).getValue();
  }
      
  
  public SeriesMetadataJaxbImpl parseSeriesMetadataJaxbImpl(String in) throws Exception {
    return parseSeriesMetadataJaxbImpl(IOUtils.toInputStream(in, "UTF8"));
  }
  
  public SeriesMetadataJaxbImpl parseSeriesMetadataJaxbImpl(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    return unmarshaller.unmarshal(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in),
                                  SeriesMetadataJaxbImpl.class).getValue();
  }   
  
  public String marshallSeries (Series s) throws Exception {
    SeriesJaxbImpl jaxbSeries = new SeriesJaxbImpl(s);
    Marshaller marshaller = jaxbContext.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(jaxbSeries, writer);
    return writer.toString();
  }   
}
