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
package org.opencastproject.maven;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.maven.plugin.AbstractMojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Generates service api and impl projects
 * @goal generate
 */
public class GenerateServiceMojo extends AbstractMojo {
  private static final Logger logger = LoggerFactory.getLogger(GenerateServiceMojo.class);

  /**
   * @parameter expression="${serviceName}" default-value="my"
   */
  protected String serviceName = null;
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }
  
  @SuppressWarnings("unchecked")
  public void execute() throws MojoExecutionException, MojoFailureException {
    String apiArtifactId = "opencast-" + serviceName + "-service-api";
    File apiProject = new File(apiArtifactId);
    
    String implArtifactId = "opencast-" + serviceName + "-service-impl";
    File implProject = new File(implArtifactId);
    
    // Expand each of the zip archives
    expand(apiArtifactId, "/opencast-template-service-api.zip");
    expand(implArtifactId, "/opencast-template-service-impl.zip");
    
    // Fix the directory names
    try {
      FileUtils.moveDirectory(new File("opencast-template-service-api"), apiProject);
      FileUtils.moveDirectory(new File("opencast-template-service-impl"), implProject);
    } catch (Exception e) {
      throw new MojoExecutionException("failed to generate service " + serviceName, e);
    }
    new DirectoryMover(serviceName, apiArtifactId);
    new DirectoryMover(serviceName, implArtifactId);
    new FileMover(serviceName, apiArtifactId);
    new FileMover(serviceName, implArtifactId);
    new FileFixer(serviceName, apiArtifactId);
    new FileFixer(serviceName, implArtifactId);
  }
  
  public void expand(String artifactId, String template) throws MojoExecutionException, MojoFailureException {
    getLog().info("Opencast Generator Expanding - " + artifactId + " from " + template);
    InputStream in = getClass().getResourceAsStream(template);
    try {
      File zipFile = copyStreamToFile(in, artifactId + ".zip");
      unzip(zipFile);
      zipFile.delete();
    } catch (IOException e) {
      throw new MojoExecutionException("failed to unzip the template files", e);
    }
  }

  public File copyStreamToFile(InputStream in, String fileName) throws IOException {
    File file = new File(fileName);
    FileOutputStream out = new FileOutputStream(file);
    IOUtils.copy(in, out);
    IOUtils.closeQuietly(in);
    IOUtils.closeQuietly(out);
    return file;
  }
  
  public void unzip(File file) throws IOException {
    ZipFile zip = new ZipFile(file);
    Enumeration<? extends ZipEntry> entries = zip.entries();
    while(entries.hasMoreElements()) {
      ZipEntry entry = (ZipEntry)entries.nextElement();
      if(entry.isDirectory()) {
        (new File(entry.getName())).mkdir();
        continue;
      }
      FileOutputStream fileOut = new FileOutputStream(entry.getName());
      IOUtils.copy(zip.getInputStream(entry), fileOut);
      IOUtils.closeQuietly(fileOut);
    }
  }
}

class DirectoryMover extends DirectoryWalker {
  
  String serviceName = null;
  String artifactName = null;
  @SuppressWarnings("unchecked")
  List results = new ArrayList();
  @SuppressWarnings("unchecked")
  public DirectoryMover(String serviceName, String artifactName) {
    super();
    this.serviceName = serviceName;
    this.artifactName = artifactName;
    try {
      super.walk(new File(artifactName), results);
      for(Iterator iter = results.iterator(); iter.hasNext();) {
        System.out.println("### result=" + iter.next());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @SuppressWarnings("unchecked")
  protected boolean handleDirectory(File directory, int depth, Collection results) {
    if("template".equals(directory.getName())) {
      directory.renameTo(new File(directory.getParent(), serviceName));
    }
    return true;
  }
}

class FileMover extends DirectoryWalker {
  String serviceName = null;
  String artifactName = null;
  @SuppressWarnings("unchecked")
  Collection results = new ArrayList();
  public FileMover(String serviceName, String artifactName) {
    super();
    this.serviceName = serviceName;
    this.artifactName = artifactName;
    try {
      super.walk(new File(artifactName), results);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
 
  @SuppressWarnings("unchecked")
  protected void handleFile(File file, int depth, Collection results) {
    // Rename the file
    String originalFileName = file.getName();
    String newFileName = originalFileName.replaceAll("template", serviceName).
      replaceAll("Template", WordUtils.capitalizeFully(serviceName));
    file.renameTo(new File(file.getParent(), newFileName));
  }
}

class FileFixer extends DirectoryWalker {
  String serviceName = null;
  String artifactName = null;
  @SuppressWarnings("unchecked")
  Collection results = new ArrayList();
  public FileFixer(String serviceName, String artifactName) {
    super();
    this.serviceName = serviceName;
    this.artifactName = artifactName;
    try {
      super.walk(new File(artifactName), results);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
 
  @SuppressWarnings("unchecked")
  protected void handleFile(File file, int depth, Collection results) {
    // Fix the file's contents
    try {
      String originalContents = FileUtils.readFileToString(file, "UTF-8");
      String newContents = originalContents.replaceAll("template", serviceName)
      .replaceAll("Template", WordUtils.capitalizeFully(serviceName));
    FileUtils.writeStringToFile(file, newContents, "UTF-8");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
