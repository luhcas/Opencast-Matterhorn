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
package org.opencastproject.media.mediapackage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Provides a {@link MessageBodyReader} and {@link MessageBodyWriter} for RESTful endpoints to produce and consume
 * {@link MediaPackage}s.
 */
public class MediaPackageJaxRsReaderWriter implements MessageBodyReader<MediaPackage>, MessageBodyWriter<MediaPackage> {
  private static final Logger logger = LoggerFactory.getLogger(MediaPackageJaxRsReaderWriter.class);

  /**
   * {@inheritDoc}
   * @see javax.ws.rs.ext.MessageBodyReader#isReadable(java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType)
   */
  public boolean isReadable(Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
    // return MediaType.TEXT_XML_TYPE.equals(mediaType) || MediaType.APPLICATION_XML_TYPE.equals(mediaType);
    logger.debug("inspecting readability of class=" + clazz + ", type=" + type + ", mediaType=" + mediaType);
    return true;
  }

  /**
   * {@inheritDoc}
   * @see javax.ws.rs.ext.MessageBodyReader#readFrom(java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType, javax.ws.rs.core.MultivaluedMap, java.io.InputStream)
   */
  public MediaPackage readFrom(Class<MediaPackage> clazz, Type type, Annotation[] annotations, MediaType mediaType,
          MultivaluedMap<String, String> headers, InputStream in) throws IOException, WebApplicationException {
    MediaPackageBuilderFactory factory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder builder = factory.newMediaPackageBuilder();
    MediaPackageSerializer s = new DefaultMediaPackageSerializerImpl();
    builder.setSerializer(s);
    try {
      return builder.loadFromManifest(in);
    } catch (MediaPackageException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see javax.ws.rs.ext.MessageBodyWriter#getSize(java.lang.Object, java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType)
   */
  public long getSize(MediaPackage mediaPackage, Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   * @see javax.ws.rs.ext.MessageBodyWriter#isWriteable(java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType)
   */
  public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
    logger.debug("inspecting writeability of class=" + clazz + ", type=" + type + ", mediaType=" + mediaType);
    return true;
  }

  /**
   * {@inheritDoc}
   * @see javax.ws.rs.ext.MessageBodyWriter#writeTo(java.lang.Object, java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType, javax.ws.rs.core.MultivaluedMap, java.io.OutputStream)
   */
  public void writeTo(MediaPackage mediaPackage, Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType,
          MultivaluedMap<String, Object> headers, OutputStream out) throws IOException, WebApplicationException {
    Document doc;
    try {
      doc = mediaPackage.toXml();
    } catch (MediaPackageException e) {
      throw new RuntimeException("Unable to parse mediapackage " + mediaPackage, e);
    }
    TransformerFactory tfactory = TransformerFactory.newInstance();
    Transformer serializer;
    try {
        serializer = tfactory.newTransformer();
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        serializer.transform(new DOMSource(doc), new StreamResult(out));
    } catch (TransformerException e) {
        throw new RuntimeException(e);
    }
  }
}
