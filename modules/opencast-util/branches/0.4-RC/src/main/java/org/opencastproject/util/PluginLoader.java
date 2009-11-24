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

package org.opencastproject.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Loads all possible Java classes that match the specified criteria.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: PluginLoader.java 679 2008-08-05 15:00:37Z wunden $
 */

public class PluginLoader {
  private static final Logger logger = LoggerFactory.getLogger(PluginLoader.class);

  private PluginLoader() {
  }

  /**
   * Search all loadable classes that match the given criteria (package name and
   * subclass relations).
   * 
   * @param pkg
   *          The package name, the classes must belong to. Subpackages of the
   *          specified package are not searched.
   * @param ext
   *          The class name of any superclass of the classes found.
   *          Superclasses are searched recursively.
   * @exception IllegalArgumentException
   *              signals a missing package name.
   * @see #findPlugins(String, String, String[])
   * @see #findPlugins(String, String[])
   */
  public static Class<?>[] findPlugins(String pkg, String ext) {
    return findPlugins(pkg, ext, null, null);
  }

  /**
   * Search all loadable classes that match the given criteria (package name and
   * multiple implements relations).
   * 
   * @param pkg
   *          The package name, the classes must belong to. Subpackages of the
   *          specified package are not searched.
   * @param impl
   *          The name of interfaces that must be implemented by the class.
   *          Specify <code>null</code> if the class doesn't have to implement
   *          any specific interface.
   * @exception IllegalArgumentException
   *              signals a missing package name.
   * @see #findPlugins(String, String, String[])
   * @see #findPlugins(String, String)
   */
  public static Class<?>[] findPlugins(String pkg, String[] impl) {
    return findPlugins(pkg, null, impl, null);
  }

  /**
   * Search all loadable classes that match the given criteria (package name,
   * subclass relation and multiple implements relations). Currently only the
   * following classes from the classpath can be found:<br>
   * <ul>
   * <li>classes in the local file system
   * <li>classes in a JAR archive, that resides in the local file system
   * <li>classes in a ZIP archive, that resides in the local file system
   * </ul>
   * 
   * @param pkg
   *          The package name, the classes must belong to. Subpackages of the
   *          specified package are not searched.
   * @param ext
   *          The class name of any superclass of the classes found.
   *          Superclasses are searched recursively.
   * @param impl
   *          The name of interfaces that must be implemented by the class.
   *          Specify <code>null</code> if the class doesn't have to implement
   *          any specific interface.
   */
  public static Class<?>[] findPlugins(String pkg, String ext, String impl[]) {
    return findPlugins(pkg, ext, impl, null);
  }

  /**
   * Search all loadable classes that match the given criteria (package name,
   * subclass relation and multiple implements relations). Currently only the
   * following classes from the classpath can be found:<br>
   * <ul>
   * <li>classes in the local file system
   * <li>classes in a JAR archive, that resides in the local file system
   * <li>classes in a ZIP archive, that resides in the local file system
   * </ul>
   * 
   * @param pkg
   *          The package name, the classes must belong to. Subpackages of the
   *          specified package are not searched.
   * @param ext
   *          The class name of any superclass of the classes found.
   *          Superclasses are searched recursively.
   * @param impl
   *          The name of interfaces that must be implemented by the class.
   *          Specify <code>null</code> if the class doesn't have to implement
   *          any specific interface.
   * @param cl
   *          a classloader responsible for locating and loading the classes.
   *          Specify <code>null</code> to use the default classloader.
   * @exception IllegalArgumentException
   *              signals a missing package name.
   */
  public static Class<?>[] findPlugins(String pkg, String ext, String impl[],
      ClassLoader cl) throws IllegalArgumentException {
    logger.info("Looking for plugins in package " + pkg);

    if (pkg == null)
      throw new IllegalArgumentException("Package name must not be null!");

    // search classpath for possible locations of the package
    String packagePath = pkg.replace('.', '/').concat("/");

    logger.info("package path = " + packagePath);

    Enumeration<URL> e;
    try {
      if (cl != null) {
        logger.debug("using classloader " + cl);
        e = cl.getResources(packagePath);
      } else {
        logger.debug("Using current classloader ");
        e = ClassLoader.getSystemResources(packagePath);
      }
    } catch (IOException ex) {
      return new Class[0];
    }

    // get classnames, eliminate duplicates
    Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
    while (e.hasMoreElements()) {
      String[] res = findClassNames(e.nextElement());
      for (int i = 0; i < res.length; i++) {
        // add only new classes to the HashTable
        if (!classes.containsKey(res[i]))
          try {
            if (cl != null)
              classes.put(res[i], Class.forName(pkg.concat(".").concat(res[i]),
                  true, cl));
            else
              classes
                  .put(res[i], Class.forName(pkg.concat(".").concat(res[i])));
          } catch (ClassNotFoundException ex) {
            // ???? this should never happen!
            System.err.println(ex);
          } catch (LinkageError ex) {
            // a class file is at a wrong place
            // (i.e. package name <> directory name)
            System.err.println(ex);
          }
      }
    }

    // check whether the classes found match all the criteria
    Iterator<Map.Entry<String, Class<?>>> iter = classes.entrySet().iterator();
    outer: while (iter.hasNext()) {
      Class<?> c = iter.next().getValue();

      // check for real class
      if (c.isInterface() || c.isArray() || c.isPrimitive()) {
        iter.remove();
        continue;
      }

      // check for abstract classes
      if (Modifier.isAbstract(c.getModifiers())) {
        iter.remove();
        continue;
      }

      // check for correct package
      // if (!c.getPackage().getName().equals(pkg)) {
      // iter.remove();
      // continue;
      // }

      // check implements relation
      if (impl != null) {
        for (int i = 0; i < impl.length; i++) {
          try {
            Class<?> implCl = Class.forName(impl[i], true,
                cl == null ? PluginLoader.class.getClassLoader() : cl);
            if (!implCl.isInterface() || !implCl.isAssignableFrom(c)) {
              iter.remove();
              continue outer;
            }
          } catch (ClassNotFoundException ex) {
            iter.remove();
            continue outer;
          }
        }
      }

      // check subclass relation
      if (ext != null) {
        try {
          Class<?> extCl = Class.forName(ext, true,
              cl == null ? PluginLoader.class.getClassLoader() : cl);
          if (extCl.isArray() || extCl.isInterface() || extCl.isPrimitive()
              || !extCl.isAssignableFrom(c)) {
            iter.remove();
            continue;
          }
        } catch (ClassNotFoundException ex) {
          iter.remove();
          continue;
        }
      }
    }

    // return an array of classes
    Class<?>[] objs = classes.values().toArray(new Class<?>[classes.size()]);
    Class<?>[] ret = new Class[objs.length];
    for (int i = 0; i < objs.length; i++) {
      logger.debug("Found plugin " + objs[i]);
      ret[i] = objs[i];
    }
    return ret;
  }

  /**
   * Finds class names at a specified URL.
   * 
   * @param url
   *          the URL where all available classes should be retrieved.
   * @return an array with the names of the classes found at the given URL.
   **/
  private static String[] findClassNames(URL url) {
    // check protocol
    if (url.getProtocol().equals("file")) {
      // handle regular files
      // 1.3 File dir = new File(url.getPath());

      // extract the name of the file by decoding
      // the file-part of the url.
      String fileName = null;
      try {
        fileName = java.net.URLDecoder.decode(url.getFile(), "UTF-8");
      } catch (java.io.UnsupportedEncodingException e) {
        System.err.println(e);
        fileName = url.getFile();
      }
      File dir = new File(fileName);

      // check whether the directory is valid
      if (!dir.exists() || !dir.isDirectory() || !dir.canRead())
        return new String[0];

      // now, find all class files
      String[] files = dir.list(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.endsWith(".class");
        }
      });

      // truncate the file extension
      if (files != null) {
        for (int i = 0; i < files.length; i++)
          files[i] = files[i].substring(0, files[i].lastIndexOf('.'));
        return files;
      }
    } else if (url.getProtocol().equals("jar")) {
      // handle ZIP and JAR archives
      String match = url.getFile().substring(url.getFile().indexOf('!') + 1);
      if (match.startsWith("/"))
        match = match.substring(1);

      // extract the archive URL and check whether the archive is
      // located somewhere in the file system.
      URL archiveFile;
      try {
        archiveFile = new URL(url.getFile().substring(0,
            url.getFile().indexOf('!')));
      } catch (MalformedURLException e) {
        System.err.println(e);
        return new String[0];
      }
      if (!archiveFile.getProtocol().equals("file")) {
        return new String[0];
      }

      // load the JAR or ZIP archive
      JarFile archive;
      // extract the name of the archive by decoding
      // the file-part of the url.
      String archiveName = null;
      try {
        archiveName = java.net.URLDecoder
            .decode(archiveFile.getFile(), "UTF-8");
      } catch (java.io.UnsupportedEncodingException e) {
        System.err.println(e);
        archiveName = archiveFile.getFile();
      }
      try {
        // archive = new JarFile(archiveFile.getFile());
        archive = new JarFile(archiveName);
      } catch (IOException e) {
        System.err.println(e);
        return new String[0];
      }

      // find classes in the archive
      Enumeration<JarEntry> e = archive.entries();
      List<String> v = new ArrayList<String>();
      while (e.hasMoreElements()) {
        ZipEntry entry = e.nextElement();
        if (entry.isDirectory())
          continue;
        String s = entry.getName();
        if (!s.endsWith(".class") || !s.startsWith(match))
          continue;
        s = s.substring(match.length(), s.length() - 6);
        // prevent subpackages
        if (s.indexOf('/') != -1)
          continue;
        v.add(s);
      }

      // close the archive
      try {
        archive.close();
      } catch (IOException ex) {
        System.err.println(ex);
      }

      // create the return value
      String[] ret = new String[v.size()];
      for (int i = 0; i < ret.length; i++)
        ret[i] = v.get(i);
      return ret;
    }

    // unknown protocol
    return new String[0];
  }

}
