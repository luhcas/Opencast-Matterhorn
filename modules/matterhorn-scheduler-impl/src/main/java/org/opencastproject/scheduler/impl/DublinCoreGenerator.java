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
package org.opencastproject.scheduler.impl;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


/**
 * converts the metadata from an scheduler event to Dublin Core metadata   
 *
 */
public class DublinCoreGenerator {
  
  private static final Logger logger = LoggerFactory.getLogger(DublinCoreGenerator.class);
  
  MetadataMapper mapper;

  
  public static String formatW3CDTF(Date date) {
    SimpleDateFormat dateFormater = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    //dateFormater.setTimeZone(Calendar.getInstance().getTimeZone());
    dateFormater.setTimeZone(TimeZone.getTimeZone("GMT"));
    return dateFormater.format(date);
  }
  
  /**
   * Constructor needs the reference to the metadata mapping information. 
   * @param dcMappingFile Properties File with mapping for Dublin Core metadata. This may come from different resources, so that is must be provided.
   * @throws FileNotFoundException
   * @throws IOException
   */
  public DublinCoreGenerator (InputStream dcMappingFile) throws FileNotFoundException, IOException {
    logger.debug("Initialising Dublin Core Generator");
    mapper = new MetadataMapper(dcMappingFile);   
  }
  
  /**
   * Generates a DublinCoreCatalog with the metadata from the provided event
   * @param event The SchedulerEvent from which the metadata should be generated as Dublin Core 
   * @return The DublinCoreCatalog
   */
  public DublinCoreCatalog generate (Event event) {
    logger.debug("creating Dublin Core  information for event {}", event.getEventId());
    DublinCoreCatalog dcCatalog = DublinCoreCatalogImpl.newInstance();
    dcCatalog.add(DublinCore.PROPERTY_IDENTIFIER, new DublinCoreValue(Long.toString(event.getEventId())));
    dcCatalog.add(DublinCore.PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(event.getStartDate(), Precision.Second));
    dcCatalog.add(DublinCore.PROPERTY_TITLE, new DublinCoreValue(event.getTitle()));
    if(event.getContributor() != null) {
      dcCatalog.add(DublinCore.PROPERTY_CONTRIBUTOR, new DublinCoreValue(event.getContributor()));
    }
    if(event.getCreator() != null) {
      dcCatalog.add(DublinCore.PROPERTY_CREATOR, new DublinCoreValue(event.getCreator()));
    }
    if(event.getDescription() != null) {
      dcCatalog.add(DublinCore.PROPERTY_DESCRIPTION, new DublinCoreValue(event.getDescription()));
    }
    if(event.getDevice() != null) {
      dcCatalog.add(DublinCore.PROPERTY_SPATIAL, new DublinCoreValue(event.getDevice())); // TODO Is this right?
    }
    if(event.getLanguage() != null) {
      dcCatalog.add(DublinCore.PROPERTY_LANGUAGE, new DublinCoreValue(event.getLanguage()));
    }
    if(event.getLicense() != null) {
      dcCatalog.add(DublinCore.PROPERTY_LICENSE, new DublinCoreValue(event.getLicense()));
    }
    if(event.getSeriesId() != null) {
      dcCatalog.add(DublinCore.PROPERTY_IS_PART_OF, new DublinCoreValue(event.getSeriesId()));
    }
    if(event.getSubject() != null) {
      dcCatalog.add(DublinCore.PROPERTY_SUBJECT, new DublinCoreValue(event.getSubject()));
    }
//    for (String key : dcMetadata.keySet()) {  
//      if (validDcKey(key)) {
//        DublinCoreValue value = new DublinCoreValue(dcMetadata.get(key));
//        EName property = new EName("http://purl.org/dc/terms/", key);
//        dcCatalog.add(property, value);
//      } else {
//        logger.debug("Key {} is not a valid Dublin Core identifier", key );
//      }
//    }   
    return dcCatalog;

  }
  
  /**
   * Generates a XML with the Dublin Core metadata from the provided event
   * @param event The SchedulerEvent from which the metadata should be generated as Dublin Core 
   * @return A String with a XML representation of the Dublin Core metadata
   */
  public String generateAsString (Event event) {
    try {
      DublinCoreCatalog dccat = generate(event);
      Document doc = dccat.toXml();
      
      Source source = new DOMSource(doc);
      StringWriter stringWriter = new StringWriter();
      Result result = new StreamResult(stringWriter);
      TransformerFactory factory = TransformerFactory.newInstance();
      Transformer transformer = factory.newTransformer();
      transformer.transform(source, result);
      

      
      return stringWriter.getBuffer().toString().trim(); 
    } catch (ParserConfigurationException e) {
      logger.error("Could not parse DublinCoreCatalog: {}", e.getMessage());
    } catch (IOException e) {
      logger.error("Could not open DublinCoreCatalog to parse it: {}", e.getMessage());
    } catch (TransformerException e) {
      logger.error("Could not transform DublinCoreCatalog: {}", e.getMessage());
    }
    return null;
  }

}
