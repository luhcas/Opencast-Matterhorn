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

import org.opencastproject.util.PathSupport;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Default implementation of a {@link MediaPackageSerializer} that is able to deal with relative urls in manifest.
 */
public class DefaultMediaPackageSerializerImpl implements MediaPackageSerializer {

  /** Optional package root file */
  protected URL packageRoot = null;
  
  /**
   * Creates a new package serializer that will work completely transparent, therefore resolving urls by simply
   * returning them as is.
   */
  public DefaultMediaPackageSerializerImpl() { }

  /**
   * Creates a new package serializer that enables the resolution of relative urls from the manifest
   * by taking <code>packageRoot</code> as the root url.
   * 
   * @param packageRoot
   *          the root url
   */
  public DefaultMediaPackageSerializerImpl(URL packageRoot) {
    this.packageRoot = packageRoot;
  }

  /**
   * Creates a new package serializer that enables the resolution of relative 
   * urls from the manifest by taking <code>packageRoot</code> as the root
   * directory.
   * 
   * @param packageRoot
   *          the root url
   * @throws MalformedURLException
   *          if the file cannot be converted to a url 
   */
  public DefaultMediaPackageSerializerImpl(File packageRoot) throws MalformedURLException {
    if (packageRoot != null)
      this.packageRoot = packageRoot.toURI().toURL();
  }

  /**
   * Returns the package root that is used determine and resolve relative
   * paths. Note that the package root may be <code>null</code>.
   * 
   * @return the packageRoot
   */
  public URL getPackageRoot() {
    return packageRoot;
  }

  /**
   * Sets the package root.
   * 
   * @param packageRoot the packageRoot to set
   * @see #getPackageRoot()
   */
  public void setPackageRoot(URL packageRoot) {
    this.packageRoot = packageRoot;
  }

  /**
   * This serializer implementation tries to cope with relative urls. Should the root url be set to any value
   * other than <code>null</code>, the serializer will try to convert element urls to relative paths if possible.
   * .
   * @see org.opencastproject.media.mediapackage.MediaPackageSerializer#encodeURL(java.net.URL)
   */
  public String encodeURL(URL url) {
    if (url == null)
      throw new IllegalArgumentException("Argument url is null");

    String path = url.toExternalForm();
    
    // Has a package root been set? If not, no relative paths!
    if (packageRoot == null)
      return url.toExternalForm();
    
    // A package root has been set
    String rootPath = packageRoot.toExternalForm();
    if (path.startsWith(rootPath)) {
      path = path.substring(rootPath.length());
    }
    
    return path;
  }

  /**
   * This serializer implementation tries to cope with relative urls. Should the path start with neither a protocol nor
   * a path separator, the packageRoot is used to create the url relative to the root url that was passed in the
   * constructor.
   * <p>
   * Note that for absolute paths without a protocol, the <code>file://</code> protocol is assumed.
   *  
   * @see #DefaultMediaPackageSerializerImpl(URL)
   * @see org.opencastproject.media.mediapackage.MediaPackageSerializer#resolvePath(java.lang.String)
   */
  public URL resolvePath(String path) throws MalformedURLException {
    if (path == null)
      throw new IllegalArgumentException("Argument path is null");
    
    // If the path starts with neither a protocol nor a path separator, the packageRoot is used to
    // create the url relative to the root
    URL url = null;
    boolean isRelative = false;
    try {
      url = new URL(path);
      isRelative = !url.getPath().startsWith("/");
      if (!isRelative)
        return url;
    } catch (MalformedURLException e) {
      // this may happen, we're still fine
      isRelative = !path.startsWith("/");
      if (!isRelative) {
        path = "file:" + path;
        url = new URL(path);
        return url;
      }
    }
     
    // This is a relative path
    if (isRelative && packageRoot != null) {
      url = new URL(PathSupport.concat(packageRoot.toExternalForm(), path));
      return url;
    }

     throw new MalformedURLException("Path '" + path + "' cannot be resolved to a URL");
  }

}