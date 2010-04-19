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
package org.opencastproject.http;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A static resource for registration with the http service.
 */
public class StaticResource extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(StaticResource.class);
  private final MimetypesFileTypeMap mimeMap = new MimetypesFileTypeMap(getClass().getClassLoader().getResourceAsStream("mimetypes"));
  String classpath;
  String alias;
  String welcomeFile;
  String testClasspath;
  String testSuite;
  URL defaultUrl;
  
  HttpService httpService;
  HttpContext httpContext;
  ComponentContext componentContext;

  public StaticResource() {}

  public StaticResource(String classpath, String alias, String welcomeFile) {
    this(classpath, alias, welcomeFile, null, null, null, null);
  }

  public StaticResource(String classpath, String alias, String welcomeFile, String testClasspath, String testSuite) {
    this(classpath, alias, welcomeFile, testClasspath, testSuite, null, null);
  }

  public StaticResource(String classpath, String alias, String welcomeFile, HttpService httpService, HttpContext httpContext) {
    this(classpath, alias, welcomeFile, null, null, httpService, httpContext);
  }

  public StaticResource(String classpath, String alias, String welcomeFile, String testClasspath, String testSuite,
          HttpService httpService, HttpContext httpContext) {
    this.classpath = classpath;
    this.alias = alias;
    this.welcomeFile = welcomeFile;
    this.testClasspath = testClasspath;
    this.testSuite = testSuite;
    this.httpService = httpService;
    this.httpContext = httpContext;
  }


  public void setHttpService(HttpService service) {
    this.httpService = service;
  }
  
  public void setHttpContext(HttpContext httpContext) {
    this.httpContext = httpContext;
  }

  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
    if(welcomeFile == null) welcomeFile = (String)componentContext.getProperties().get("welcome.file");
    boolean welcomeFileSpecified = true;
    if(welcomeFile == null) {
      welcomeFileSpecified = false;
      welcomeFile = "index.html";
    }
    if(alias == null) alias = (String)componentContext.getProperties().get("alias");
    if(classpath == null) classpath = (String)componentContext.getProperties().get("classpath");
    if(testSuite == null) testSuite = (String)componentContext.getProperties().get("test.suite");
    if(testClasspath == null) testClasspath = (String)componentContext.getProperties().get("test.classpath");
    logger.info("registering classpath:{} at {} with welcome file {} {}, test suite: {} from classpath {}",
            new Object[] {classpath, alias, welcomeFile, welcomeFileSpecified ? "" : "(via default)", testSuite, testClasspath});
    try {
      httpService.registerServlet(alias, this, null, httpContext);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    String serverUrl = componentContext.getBundleContext().getProperty("org.opencastproject.server.url");
    try {
      defaultUrl = new URL(serverUrl + alias);
    } catch (MalformedURLException e) {
      throw new IllegalStateException("unable to construct URL " + serverUrl + "/" + alias, e);
    }
  }
  
  public void deactivate(ComponentContext context) {
    try {
      httpService.unregister(alias);
    } catch(Exception e) {
      logger.debug("unable to unregister alias " + alias, e);
    }
  }

  public URL getDefaultUrl() {
    return defaultUrl;
  }
  
  @Override
  public String toString() {
    return "StaticResource [alias=" + alias + ", classpath=" + classpath + ", welcome file=" + welcomeFile + "]";
  }
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) {
    String pathInfo = req.getPathInfo();
    String servletPath = req.getServletPath();
    String path = pathInfo == null ? servletPath : servletPath + pathInfo;
    Boolean testMode = "true".equalsIgnoreCase(componentContext.getBundleContext().getProperty("testMode"));
    logger.debug("handling path {}, pathInfo={}, servletPath={}, testMode={}", new Object[] {path, pathInfo, servletPath, testMode});
    
    // If the URL points to a "directory", redirect to the welcome file
    if("/".equals(path) || alias.equals(path) || (alias + "/").equals(path)) {
      try {
        String redirectPath;
        if("/".equals(alias)) {
          redirectPath = "/" + welcomeFile;
        } else {
          redirectPath = alias + "/" + welcomeFile;
        }
        logger.debug("redirecting {} to {}", new String[] {path, redirectPath});
        resp.sendRedirect(redirectPath);
        return;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    
    // Find and deliver the resource
    String classpathToResource;
    if(pathInfo == null) {
      if( ! servletPath.equals(alias)) {
        classpathToResource = classpath + servletPath;
      } else {
        classpathToResource = classpath + "/" + welcomeFile;
      }
    } else {
      classpathToResource = classpath + pathInfo;
    }

    // Make sure we are using an absolute path
    if (!classpathToResource.startsWith("/"))
      classpathToResource = "/" + classpathToResource;

    // Try to load the resource from the regular resources section
    URL url = componentContext.getBundleContext().getBundle().getResource(classpathToResource);
    
    // No luck? Maybe it's part of the test class path?
    if(url == null && testMode && testClasspath != null) {
      if(pathInfo == null) {
        if( ! servletPath.equals(alias)) {
          classpathToResource = testClasspath + servletPath;
        } else {
          classpathToResource = testClasspath + "/" + testSuite;
        }
      } else {
        classpathToResource = testClasspath + pathInfo;
      }
      if (!classpathToResource.startsWith("/"))
        classpathToResource = "/" + classpathToResource;
      url = componentContext.getBundleContext().getBundle().getResource(classpathToResource);
    }
      
    if(url == null) {
      try {
        resp.sendError(404);
        return;
      } catch (IOException e) {
        logger.warn(e.getMessage(), e);
        return;
      }
    }
    logger.debug("opening url {} {}", new Object[] {classpathToResource, url});
    InputStream in = null;
    try {
      in = url.openStream();
      String md5 = DigestUtils.md5Hex(in);
      if(md5.equals(req.getHeader("If-None-Match"))) {
        resp.setStatus(304);
        return;
      }
      resp.setHeader("ETag", md5);
    } catch (IOException e) {
      logger.warn("This system can not generate md5 hashes.");
    } finally {
      IOUtils.closeQuietly(in);
    }
    String contentType = mimeMap.getContentType(url.getFile());
    if( ! "application/octet-stream".equals(contentType)){
      resp.setHeader("Content-Type", contentType);
    }
    try {
      in = url.openStream();
      IOUtils.copy(in, resp.getOutputStream());
    } catch (IOException e) {
      logger.warn("could not open or copy streams", e);
      try {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
      } catch (IOException e1) {
        logger.warn("unable to send http 500 error: {}", e1);
      }
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

}
