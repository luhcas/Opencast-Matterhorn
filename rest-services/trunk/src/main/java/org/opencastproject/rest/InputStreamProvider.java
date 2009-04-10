package org.opencastproject.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

/**
 * Provides a means to send {@link InputStream}s to the servlet response's
 * {@link OutputStream}. RestEasy includes a provider for inputstreams, but its
 * type checking is either broken or so severe that it's unusable.
 */
@Provider
@Produces("*/*")
public class InputStreamProvider implements MessageBodyWriter<InputStream> {
  public boolean isWriteable(Class<?> type, Type genericType,
      Annotation[] annotations, MediaType mediaType) {
    return InputStream.class.isAssignableFrom(type);
  }

  public long getSize(InputStream inputStream, Class<?> type, Type genericType,
      Annotation[] annotations, MediaType mediaType) {
    return -1;
  }

  public void writeTo(InputStream is, Class<?> type, Type genericType,
      Annotation[] annotations, MediaType mediaType,
      MultivaluedMap<String, Object> headers, OutputStream os)
      throws IOException {
    int c;
    while ((c = is.read()) != -1) {
      os.write(c);
    }
  }
}
