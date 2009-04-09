package org.opencastproject.rest;

import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class ResteasyServlet extends HttpServletDispatcher {
  private static final long serialVersionUID = 1L;

  public static final String SERVLET_PATH = "/rest";

  ServletConfig servletConfig;
  ServletContext servletContext;
  
  public void init(ServletConfig servletConfig) throws ServletException {
    this.servletConfig = new ServletConfigWrapper(servletConfig);
    this.servletContext = this.servletConfig.getServletContext();
    super.init(this.servletConfig);
    RegisterBuiltin.register(this.providerFactory); // TODO Why do we need to do this twice?
  }
  
  public ServletConfig getServletConfig() {
    return servletConfig;
  }
  
  public ServletContext getServletContext() {
    return servletContext;
  }
}

class ServletConfigWrapper implements ServletConfig {
  private ServletConfig delegate;
  public String getInitParameter(String arg0) {
    return delegate.getInitParameter(arg0);
  }
  public Enumeration getInitParameterNames() {
    return delegate.getInitParameterNames();
  }
  public ServletContext getServletContext() {
    return new ServletContextWrapper(delegate.getServletContext());
  }
  public String getServletName() {
    return delegate.getServletName();
  }
  ServletConfigWrapper(ServletConfig delegate) {
    this.delegate = delegate;
  }
}

class ServletContextWrapper implements ServletContext {
  private ServletContext delegate;
  public ServletContextWrapper(ServletContext delegate) {
    this.delegate = delegate;
  }
  public Object getAttribute(String arg0) {
    return delegate.getAttribute(arg0);
  }
  public Enumeration getAttributeNames() {
    return delegate.getAttributeNames();
  }
  public ServletContext getContext(String arg0) {
    return delegate.getContext(arg0);
  }
  public String getContextPath() {
    return delegate.getContextPath();
  }
  public String getInitParameter(String key) {
    if ("resteasy.servlet.mapping.prefix".equalsIgnoreCase(key)) {
      return ResteasyServlet.SERVLET_PATH;
    } else {
      return delegate.getInitParameter(key);
    }
  }
  public Enumeration getInitParameterNames() {
    return delegate.getInitParameterNames();
  }
  public int getMajorVersion() {
    return delegate.getMajorVersion();
  }
  public String getMimeType(String arg0) {
    return delegate.getMimeType(arg0);
  }
  public int getMinorVersion() {
    return delegate.getMinorVersion();
  }
  public RequestDispatcher getNamedDispatcher(String arg0) {
    return delegate.getNamedDispatcher(arg0);
  }
  public String getRealPath(String arg0) {
    return delegate.getRealPath(arg0);
  }
  public RequestDispatcher getRequestDispatcher(String arg0) {
    return delegate.getRequestDispatcher(arg0);
  }
  public URL getResource(String arg0) throws MalformedURLException {
    return delegate.getResource(arg0);
  }
  public InputStream getResourceAsStream(String arg0) {
    return delegate.getResourceAsStream(arg0);
  }
  public Set getResourcePaths(String arg0) {
    return delegate.getResourcePaths(arg0);
  }
  public String getServerInfo() {
    return delegate.getServerInfo();
  }
  public Servlet getServlet(String arg0) throws ServletException {
    return delegate.getServlet(arg0);
  }
  public String getServletContextName() {
    return delegate.getServletContextName();
  }
  public Enumeration getServletNames() {
    return delegate.getServletNames();
  }
  public Enumeration getServlets() {
    return delegate.getServlets();
  }
  public void log(Exception arg0, String arg1) {
    delegate.log(arg0, arg1);
  }
  public void log(String arg0, Throwable arg1) {
    delegate.log(arg0, arg1);
  }
  public void log(String arg0) {
    delegate.log(arg0);
  }
  public void removeAttribute(String arg0) {
    delegate.removeAttribute(arg0);
  }
  public void setAttribute(String arg0, Object arg1) {
    delegate.setAttribute(arg0, arg1);
  }
}