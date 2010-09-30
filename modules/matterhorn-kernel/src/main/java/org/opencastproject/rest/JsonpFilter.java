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
package org.opencastproject.rest;

import org.apache.cxf.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Adds padding to json responses when the 'jsonp' parameter is specified.
 */
public class JsonpFilter implements Filter {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JsonpFilter.class);

  /** The querystring parameter that indicates the response should be padded */
  public static final String CALLBACK_PARAM = "jsonp";

  /** The regular expression to ensure that the callback is safe for display to a browser */
  public static final Pattern SAFE_PATTERN = Pattern.compile("[a-zA-Z0-9\\.]+");

  /** The default padding to use if the specified padding contains invalid characters */
  public static final String DEFAULT_CALLBACK = "handleMatterhornData";

  /** The content type for jsonp is "application/javascript", not "application/json". */
  public static final String JS_CONTENT_TYPE = "application/javascript;charset=UTF-8";

  /** The post padding, which is always ');' no matter what the pre-padding looks like */
  protected byte[] postPadding;

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  @Override
  public void init(FilterConfig config) throws ServletException {
    try {
      postPadding = ");".getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new ServletException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   *      javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
          ServletException {

    // Cast the request and response to HTTP versions
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;

    // Determine whether the response must be wrapped
    String callbackValue = request.getParameter(CALLBACK_PARAM);
    if (StringUtils.isEmpty(callbackValue)) {
      logger.debug("No json padding requested from {}", request);
      chain.doFilter(request, response);
    } else {
      logger.debug("Json padding '{}' requested from {}", callbackValue, request);
      
      // Ensure the callback value contains only safe characters
      if(!SAFE_PATTERN.matcher(callbackValue).matches()) {
        callbackValue = DEFAULT_CALLBACK;
      }
      
      // Write the padded response
      byte[] prePadding = (callbackValue + "(").getBytes();
      OutputStream out = response.getOutputStream();
      GenericResponseWrapper wrapper = new GenericResponseWrapper((HttpServletResponse) response);
      chain.doFilter(request, wrapper);
      out.write(prePadding);
      out.write(wrapper.getData());
      out.write(postPadding);
      wrapper.setContentType("text/javascript;charset=UTF-8");
      out.close();
    }
  }

  /**
   * A response wrapper that allows for json padding.
   */
  static class GenericResponseWrapper extends HttpServletResponseWrapper {

    /** The output stream */
    private ByteArrayOutputStream output;
    
    /** The content length */
    private int contentLength;
    
    /** The content type */
    private String contentType;

    /** Wrap the original response */
    public GenericResponseWrapper(HttpServletResponse response) {
      super(response);
      output = new ByteArrayOutputStream();
    }

    /** Get the data from the original response */
    public byte[] getData() {
      return output.toByteArray();
    }

    @Override
    public ServletOutputStream getOutputStream() {
      return new FilterServletOutputStream(output);
    }

    @Override
    public PrintWriter getWriter() {
      return new PrintWriter(getOutputStream(), true);
    }

    @Override
    public void setContentLength(int length) {
      this.contentLength = length;
      super.setContentLength(length);
    }

    public int getContentLength() {
      return contentLength;
    }

    public void setContentType(String type) {
      this.contentType = type;
      super.setContentType(type);
    }

    public String getContentType() {
      return contentType;
    }
  }

  static class FilterServletOutputStream extends ServletOutputStream {

    private DataOutputStream stream;

    public FilterServletOutputStream(OutputStream output) {
      stream = new DataOutputStream(output);
    }

    @Override
    public void write(int b) throws IOException {
      stream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
      stream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      stream.write(b, off, len);
    }
  }

}
