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
package org.opencastproject.feed.scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Installs feeds matching "*-feed.xml" in any of felix fileinstall's watch directories.
 */
public class FeedRegistrationScanner implements ArtifactUrlTransformer {
  private static final Logger logger = LoggerFactory.getLogger(FeedRegistrationScanner.class);
  
  protected File tempJarDirectory = new File(System.getProperty("java.io.tmpdir"), "dynamicbundles");
  
  /**
   * {@inheritDoc}
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  @Override
  public boolean canHandle(File artifact) {
    return artifact.getName().endsWith("feed.xml");
  }
  
  /**
   * {@inheritDoc}
   * @see org.apache.felix.fileinstall.ArtifactUrlTransformer#transform(java.net.URL)
   */
  @Override
  public URL transform(URL url) throws Exception {
    File artifact = FileUtils.toFile(url);
    logger.info("Installing feed {}", artifact.getName());
    
    // make the temp directory
    if( ! tempJarDirectory.exists()) {
      tempJarDirectory.mkdirs();
    }
    // construct the osgi bundle jar
    InputStream manifestIn = getClass().getResourceAsStream("/OSGI-INF/component-manifest.txt");
    String manifestString = IOUtils.toString(manifestIn).replaceAll("filename", artifact.getName());
    logger.debug("Using manifest:\n{}\n", manifestString);
    Manifest manifest = new Manifest(IOUtils.toInputStream(manifestString));

    FileOutputStream fileOut = null;
    JarOutputStream jarOut = null;
    FileInputStream fileIn = null;
    File outFile = new File(tempJarDirectory, artifact.getName() + ".jar");
    try {
      fileOut = new FileOutputStream(outFile);
      jarOut = new JarOutputStream(fileOut, manifest);
      fileIn = new FileInputStream(artifact);
      JarEntry dirEntry = new JarEntry("OSGI-INF/");
      dirEntry.setTime(artifact.lastModified());
      jarOut.putNextEntry(dirEntry);
      jarOut.closeEntry();

      JarEntry componentRegistrationEntry = new JarEntry("OSGI-INF/component.xml");
      componentRegistrationEntry.setTime(artifact.lastModified());
      jarOut.putNextEntry(componentRegistrationEntry);
      IOUtils.copy(fileIn, jarOut);
      jarOut.closeEntry();
      
    } finally {
      IOUtils.closeQuietly(fileIn);
      IOUtils.closeQuietly(jarOut);
      IOUtils.closeQuietly(fileOut);
    }
    return outFile.toURI().toURL();
  }
}
