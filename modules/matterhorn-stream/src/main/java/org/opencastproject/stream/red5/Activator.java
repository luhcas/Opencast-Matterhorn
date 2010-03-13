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
package org.opencastproject.stream.red5;

import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;
import org.red5.server.ClientRegistry;
import org.red5.server.Context;
import org.red5.server.GlobalScope;
import org.red5.server.MappingStrategy;
import org.red5.server.ScopeResolver;
import org.red5.server.Server;
import org.red5.server.WebScope;
import org.red5.server.api.Red5;
import org.red5.server.jmx.JMXAgent;
import org.red5.server.net.rtmpt.RTMPTServlet;
import org.red5.server.net.servlet.AMFGatewayServlet;
import org.red5.server.persistence.FilePersistenceThread;
import org.red5.server.service.ServiceInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextListener;

import java.beans.Introspector;
import java.io.File;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;

/**
 * Configures and starts a red5 streaming server.
 */
public class Activator extends ContextLoaderListener implements BundleActivator {
  private static final Logger logger = LoggerFactory.getLogger(Activator.class);
  private ServiceTracker httpTracker = null;
  private static ArrayList<ServletContext> registeredContexts = new ArrayList<ServletContext>(3);
  private ConfigurableWebApplicationContext applicationContext;
  private DefaultListableBeanFactory parentFactory;
  private static ServletContext servletContext;
  private ContextLoader contextLoader;
  private ClientRegistry clientRegistry;
  private ServiceInvoker globalInvoker;
  private MappingStrategy globalStrategy;
  private ScopeResolver globalResolver;
  private GlobalScope global;
  private Server server;

  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    final ServletContextListener servletContextListener = this;
    final RequestContextListener requestContextListener = new RequestContextListener();
    final HttpServlet gatewayServlet = new AMFGatewayServlet();
    final HttpServlet rtmptServlet = new RTMPTServlet();
    httpTracker = new ServiceTracker(context, WebContainer.class.getName(), null) {
      @Override
      public Object addingService(ServiceReference reference) {
        WebContainer httpService = (WebContainer) context.getService(reference);
        HttpContext httpContext = httpService.createDefaultHttpContext();
        Dictionary<String, String>props = new Hashtable<String, String>();
        props.put("globalScope", "default");
        props.put("parentContextKey", "default.context");
        props.put("webAppRootKey", "/stream");
        props.put("contextConfigLocation", "classpath:/stream-web.xml");

        httpService.setContextParam(props, httpContext);
        // Register listeners
        httpService.registerEventListener(servletContextListener, httpContext);
        httpService.registerEventListener(requestContextListener, httpContext);

        // Register servlets
        try {
          httpService.registerServlet(gatewayServlet, new String[] {"/stream/gateway"}, null, httpContext);
          httpService.registerServlet(rtmptServlet,
                  new String[] {"/stream/fcs/*", "/stream/open/*", "/stream/idle/*", "/stream/send/*", "/stream/close/*"}, null, httpContext);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        
        return super.addingService(reference);
      }
      @Override
      public void removedService(ServiceReference reference, Object service) {
        WebContainer httpService = (WebContainer) context.getService(reference);
        httpService.unregisterEventListener(servletContextListener);
        httpService.unregisterEventListener(requestContextListener);
        httpService.unregisterServlet(gatewayServlet);
        httpService.unregisterServlet(rtmptServlet);
        super.removedService(reference, service);
      }
    };
    httpTracker.open();
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    httpTracker.close();
  }

  /**
   * Main entry point for the Red5 Server
   */
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    System.setProperty("red5.deployment.type", "war");
    servletContext = sce.getServletContext();
    String rootDir = System.getProperty("java.io.tmpdir") + File.separator + "opencast";
    System.setProperty("red5.webapp.root", rootDir);
    
    long time = System.currentTimeMillis();

    logger.info("{} WAR loader", Red5.VERSION);
    logger.debug("Path: {}", rootDir);

    try {
      contextLoader = createContextLoader();
      applicationContext = (ConfigurableWebApplicationContext) contextLoader.initWebApplicationContext(servletContext);
      // this is servlet 2.5 only and oddly we seem to be using a mix of 2.4 and 2.5, commenting this out until all 2.4 references are gone -AZ
      //logger.debug("Root context path: {}", applicationContext.getServletContext().getContextPath());
      ConfigurableBeanFactory factory = applicationContext.getBeanFactory();
      factory.registerSingleton("default.context", applicationContext);
      parentFactory = (DefaultListableBeanFactory) factory.getParentBeanFactory();
    } catch (Throwable t) {
      t.printStackTrace();
      logger.error("", t);
    }
    long startupIn = System.currentTimeMillis() - time;
    logger.info("Startup done in: {} ms", startupIn);
  }

  /*
   * Registers a subcontext with red5
   */
  public void registerSubContext(String webAppKey) {
    // get the sub contexts - servlet context
    ServletContext ctx = servletContext.getContext(webAppKey);
    if (ctx == null) {
      ctx = servletContext;
    }
    ContextLoader loader = new ContextLoader();
    ConfigurableWebApplicationContext appCtx = (ConfigurableWebApplicationContext) loader.initWebApplicationContext(ctx);
    appCtx.setParent(applicationContext);
    appCtx.refresh();

    ctx.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx);
    ConfigurableBeanFactory appFactory = appCtx.getBeanFactory();

    logger.debug("About to grab Webcontext bean for {}", webAppKey);
    Context webContext = (Context) appCtx.getBean("web.context");
    webContext.setCoreBeanFactory(parentFactory);
    webContext.setClientRegistry(clientRegistry);
    webContext.setServiceInvoker(globalInvoker);
    webContext.setScopeResolver(globalResolver);
    webContext.setMappingStrategy(globalStrategy);

    WebScope scope = (WebScope) appFactory.getBean("web.scope");
    scope.setServer(server);
    scope.setParent(global);
    scope.register();
    scope.start();

    // register the context so we dont try to reinitialize it
    registeredContexts.add(ctx);

  }

  @Override
  public ContextLoader getContextLoader() {
    return this.contextLoader;
  }

  /**
   * Clearing the in-memory configuration parameters, we will receive
   * notification that the servlet context is about to be shut down
   */
  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    synchronized (servletContext) {
      logger.info("Webapp shutdown");
      try {
        ServletContext ctx = sce.getServletContext();
        // prepare spring for shutdown
        Introspector.flushCaches();
        // dereg any drivers
        for (Enumeration<?> e = DriverManager.getDrivers(); e
            .hasMoreElements();) {
          Driver driver = (Driver) e.nextElement();
          if (driver.getClass().getClassLoader() == getClass()
              .getClassLoader()) {
            DriverManager.deregisterDriver(driver);
          }
        }
        // shutdown jmx
        JMXAgent.shutdown();
        // shutdown the persistence thread
        FilePersistenceThread persistenceThread = FilePersistenceThread.getInstance();
        if (persistenceThread != null) {
          persistenceThread.shutdown();
        }
        // shutdown spring
        Object attr = ctx.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        if (attr != null) {
          // get web application context from the servlet context
          ConfigurableWebApplicationContext applicationContext = (ConfigurableWebApplicationContext) attr;
          ConfigurableBeanFactory factory = applicationContext.getBeanFactory();
          // for (String scope : factory.getRegisteredScopeNames()) {
          // logger.debug("Registered scope: " + scope);
          // }
          try {
            for (String singleton : factory.getSingletonNames()) {
              logger.debug("Registered singleton: {}", singleton);
              factory.destroyScopedBean(singleton);
            }
          } catch (RuntimeException e) {
          }
          factory.destroySingletons();
          ctx.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
          applicationContext.close();
        }
        getContextLoader().closeWebApplicationContext(ctx);
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }
}
