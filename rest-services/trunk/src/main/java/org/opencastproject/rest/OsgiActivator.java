/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.jboss.resteasy.spi.Registry;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Hashtable;

public class OsgiActivator implements BundleActivator {
  public static final String urlPattern = "/rest/*";

  protected Registry registry;

  public void start(BundleContext context) throws Exception {
    // Create a servlet to handle /rest URLs
    HttpServletDispatcher restServlet = new HttpServletDispatcher();
    ResteasyBootstrap restServletContextListener = new ResteasyBootstrap();

    // FIXME: This is a hack -- it assumes the existence of a pax web
    // container at the time the bundle starts
    WebContainer pax = (WebContainer) context.getService(context
        .getServiceReference(WebContainer.class.getName()));
    HttpContext httpContext = pax.createDefaultHttpContext();
    pax.registerEventListener(restServletContextListener, httpContext);
    pax.registerServlet(restServlet, "RestEasy", new String[] { urlPattern },
        new Hashtable<String, String>(), null);

    // ResteasyProviderFactory defaultInstance =
    // ResteasyProviderFactory.getInstance();
    // ResteasyProviderFactory.setInstance(new
    // ThreadLocalResteasyProviderFactory(defaultInstance));
    //
    // event.getServletContext().setAttribute(ResteasyProviderFactory.class.getName(),
    // factory);
    // dispatcher = new SynchronousDispatcher(factory);
    // registry = dispatcher.getRegistry();
    // event.getServletContext().setAttribute(Dispatcher.class.getName(),
    // dispatcher);
    // event.getServletContext().setAttribute(Registry.class.getName(),
    // registry);
    // String applicationConfig =
    // event.getServletContext().getInitParameter(Application.class.getName());
    // if (applicationConfig == null)
    // {
    // // stupid spec doesn't use FQN of Application class name
    // applicationConfig =
    // event.getServletContext().getInitParameter("javax.ws.rs.Application");
    // }
    // else
    // {
    // logger.warn("The use of " + Application.class.getName() +
    // " is deprecated, please use javax.ws.rs.Application as a context-param instead");
    // }
    //
    // String providers =
    // event.getServletContext().getInitParameter(ResteasyContextParameters.RESTEASY_PROVIDERS);
    //
    // if (providers != null) setProviders(providers);
    //
    // String resourceMethodInterceptors =
    // event.getServletContext().getInitParameter(ResteasyContextParameters.RESTEASY_RESOURCE_METHOD_INTERCEPTORS);
    //
    // if (resourceMethodInterceptors != null)
    // setProviders(resourceMethodInterceptors);
    //
    // String builtin =
    // event.getServletContext().getInitParameter(ResteasyContextParameters.RESTEASY_USE_BUILTIN_PROVIDERS);
    // if (builtin == null || Boolean.valueOf(builtin.trim()))
    // RegisterBuiltin.register(factory);
    //
    // boolean scanProviders = false;
    // boolean scanResources = false;
    //
    // String sProviders =
    // event.getServletContext().getInitParameter(ResteasyContextParameters.RESTEASY_SCAN_PROVIDERS);
    // if (sProviders != null)
    // {
    // scanProviders = Boolean.valueOf(sProviders.trim());
    // }
    // String scanAll =
    // event.getServletContext().getInitParameter(ResteasyContextParameters.RESTEASY_SCAN);
    // if (scanAll != null)
    // {
    // boolean tmp = Boolean.valueOf(scanAll.trim());
    // scanProviders = tmp || scanProviders;
    // scanResources = tmp || scanResources;
    // }
    // String sResources =
    // event.getServletContext().getInitParameter(ResteasyContextParameters.RESTEASY_SCAN_RESOURCES);
    // if (sResources != null)
    // {
    // scanResources = Boolean.valueOf(sResources.trim());
    // }
    //
    // if (scanProviders || scanResources)
    // {
    // if (applicationConfig != null)
    // throw new
    // RuntimeException("You cannot deploy a javax.ws.rs.core.Application and have scanning on as this may create errors");
    //
    // URL[] urls = WarUrlFinder.findWebInfLibClasspaths(event);
    // URL url = WarUrlFinder.findWebInfClassesPath(event);
    // AnnotationDB db = new AnnotationDB();
    // String[] ignoredPackages = {"org.jboss.resteasy.plugins",
    // "org.jboss.resteasy.annotations", "org.jboss.resteasy.client",
    // "org.jboss.resteasy.specimpl", "org.jboss.resteasy.core",
    // "org.jboss.resteasy.spi", "org.jboss.resteasy.util",
    // "org.jboss.resteasy.mock", "javax.ws.rs"};
    // db.setIgnoredPackages(ignoredPackages);
    // try
    // {
    // if (url != null) db.scanArchives(url);
    // db.scanArchives(urls);
    // try
    // {
    // db.crossReferenceImplementedInterfaces();
    // db.crossReferenceMetaAnnotations();
    // }
    // catch (AnnotationDB.CrossReferenceException ignored)
    // {
    //
    // }
    //
    // }
    // catch (IOException e)
    // {
    // throw new
    // RuntimeException("Unable to scan WEB-INF for JAX-RS annotations, you must manually register your classes/resources",
    // e);
    // }
    //
    // if (scanProviders) processProviders(db);
    // if (scanResources) processResources(db);
    // }
    //
    // String jndiResources =
    // event.getServletContext().getInitParameter(ResteasyContextParameters.RESTEASY_JNDI_RESOURCES);
    // if (jndiResources != null)
    // {
    // processJndiResources(jndiResources);
    // }
    //
    // String resources =
    // event.getServletContext().getInitParameter(ResteasyContextParameters.RESTEASY_RESOURCES);
    // if (resources != null)
    // {
    // processResources(resources);
    // }
    //
    // // Mappings don't work anymore, but leaving the code in just in case
    // users demand to put it back in
    // String mimeExtentions =
    // event.getServletContext().getInitParameter(ResteasyContextParameters.RESTEASY_MEDIA_TYPE_MAPPINGS);
    // if (mimeExtentions != null)
    // {
    // Map<String, String> map = parseMap(mimeExtentions);
    // Map<String, MediaType> extMap = new HashMap<String, MediaType>();
    // for (String ext : map.keySet())
    // {
    // String value = map.get(ext);
    // extMap.put(ext, MediaType.valueOf(value));
    // }
    // if (dispatcher.getMediaTypeMappings() != null)
    // dispatcher.getMediaTypeMappings().putAll(extMap);
    // else dispatcher.setMediaTypeMappings(extMap);
    // }
    //
    // // Mappings don't work anymore, but leaving the code in just in case
    // users demand to put it back in
    // String languageExtensions =
    // event.getServletContext().getInitParameter(ResteasyContextParameters.RESTEASY_LANGUAGE_MAPPINGS);
    // if (languageExtensions != null)
    // {
    // Map<String, String> map = parseMap(languageExtensions);
    // if (dispatcher.getLanguageMappings() != null)
    // dispatcher.getLanguageMappings().putAll(map);
    // else dispatcher.setLanguageMappings(map);
    // }
    //
    // if (applicationConfig != null)
    // {
    // try
    // {
    // //System.out.println("application config: " + applicationConfig.trim());
    // Class configClass =
    // Thread.currentThread().getContextClassLoader().loadClass(applicationConfig.trim());
    // Application config = (Application) configClass.newInstance();
    // processApplication(config, registry, factory);
    // }
    // catch (ClassNotFoundException e)
    // {
    // throw new RuntimeException(e);
    // }
    // catch (InstantiationException e)
    // {
    // throw new RuntimeException(e);
    // }
    // catch (IllegalAccessException e)
    // {
    // throw new RuntimeException(e);
    // }
    // }

    // Add the existing JAX-RS resources
    registry = (Registry) restServlet.getServletContext().getAttribute(
        Registry.class.getName());
    for (ServiceReference jaxRsRef : context.getAllServiceReferences(
        OpencastRestService.class.getName(), null)) {
      registry.addSingletonResource(context.getService(jaxRsRef));
    }

    // Track JAX-RS Resources that are added and removed
    ServiceTracker tracker = new ServiceTracker(context,
        OpencastRestService.class.getName(), null) {
      @Override
      public Object addingService(ServiceReference reference) {
        registry.addSingletonResource(context.getService(reference));
        return super.addingService(reference);
      }

      @Override
      public void removedService(ServiceReference reference, Object service) {
        registry.removeRegistrations(context.getService(reference).getClass());
        super.removedService(reference, service);
      }
    };
    tracker.open();
  }

  public void stop(BundleContext arg0) throws Exception {
  }

}
