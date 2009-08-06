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

import org.apache.commons.io.FileUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.maven.plugin.AbstractMojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Generates service api and impl projects
 * @goal generate
 */
public class GenerateServiceMojo extends AbstractMojo {
  /**
   * @parameter expression="${serviceName}" default-value="my"
   */
  protected String serviceName = null;
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }
  
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
    
    // Fix the pom.xml files
    File apiPomFile = new File(apiProject, "pom.xml");
    File implPomFile = new File(implProject, "pom.xml");
    String apiPom = FileUtils.readFileToString(apiPomFile);
    String implPom = FileUtils.readFileToString(implPomFile);
    apiPom = apiPom.replaceAll("template", serviceName);
    implPom = implPom.replaceAll("template", serviceName);
    FileUtils.writeStringToFile(apiPomFile, apiPom, "UTF-8");
    FileUtils.writeStringToFile(implPomFile, implPom, "UTF-8");
    
    } catch (Exception e) {
      throw new MojoExecutionException("failed to generate service " + serviceName, e);
    }
    
    // Rename the Java packages
    File apiPackage = new File(apiArtifactId + "/src/main/java/org/opencastproject/template");
    apiPackage.renameTo(new File(apiArtifactId + "/src/main/java/org/opencastproject/" + serviceName));
    File implPackage = new File(implArtifactId + "/src/main/java/org/opencastproject/template");
    implPackage.renameTo(new File(implArtifactId + "/src/main/java/org/opencastproject/" + serviceName));
    File testPackage = new File(implArtifactId + "/src/test/java/org/opencastproject/template");
    testPackage.renameTo(new File(implArtifactId + "/src/test/java/org/opencastproject/" + serviceName));

    // Rename the Java files
    File serviceApi = new File(apiArtifactId + "/src/main/java/org/opencastproject/" + serviceName + "/api/TemplateService.java");
    serviceApi.renameTo(new File(apiArtifactId + "/src/main/java/org/opencastproject/" + serviceName + "/api/" + WordUtils.capitalizeFully(serviceName) +"Service.java"));
    File serviceImpl = new File(implArtifactId + "/src/main/java/org/opencastproject/" + serviceName + "/impl/TemplateServiceImpl.java");
    serviceImpl.renameTo(new File(implArtifactId + "/src/main/java/org/opencastproject/" + serviceName + "/impl/" + WordUtils.capitalizeFully(serviceName) +"ServiceImpl.java"));
    File serviceRestImpl = new File(implArtifactId + "/src/main/java/org/opencastproject/" + serviceName + "/impl/TemplateServiceRestImpl.java");
    serviceRestImpl.renameTo(new File(implArtifactId + "/src/main/java/org/opencastproject/" + serviceName + "/impl/" + WordUtils.capitalizeFully(serviceName) +"ServiceRestImpl.java"));
    File serviceTest = new File(implArtifactId + "/src/test/java/org/opencastproject/" + serviceName + "/impl/TemplateServiceImplTest.java");
    serviceTest.renameTo(new File(implArtifactId + "/src/test/java/org/opencastproject/" + serviceName + "/impl/" + WordUtils.capitalizeFully(serviceName) +"ServiceImplTest.java"));

    // Rename the service component files
    File serviceComponent = new File(implArtifactId + "/src/main/resources/OSGI-INF/template-service.xml");
    serviceComponent.renameTo(new File(implArtifactId + "/src/main/resources/OSGI-INF/" + serviceName + "-service.xml"));
    File serviceRestComponent = new File(implArtifactId + "/src/main/resources/OSGI-INF/template-service-rest.xml");
    serviceRestComponent.renameTo(new File(implArtifactId + "/src/main/resources/OSGI-INF/" + serviceName + "-service-rest.xml"));

    try {
      // Fix the Java files
      File javaApiFile = new File(apiArtifactId + "/src/main/java/org/opencastproject/" + serviceName + "/api/" + WordUtils.capitalizeFully(serviceName) +"Service.java");
      String javaApiString = FileUtils.readFileToString(javaApiFile, "UTF-8");
      javaApiString = javaApiString.replaceAll("template", serviceName);
      javaApiString = javaApiString.replaceAll("Template", WordUtils.capitalizeFully(serviceName));
      FileUtils.writeStringToFile(javaApiFile, javaApiString, "UTF-8");
      
      File javaImplFile = new File(implArtifactId + "/src/main/java/org/opencastproject/" + serviceName + "/impl/" + WordUtils.capitalizeFully(serviceName) +"ServiceImpl.java");
      String javaImplString = FileUtils.readFileToString(javaImplFile, "UTF-8");
      javaImplString = javaImplString.replaceAll("template", serviceName);
      javaImplString = javaImplString.replaceAll("Template", WordUtils.capitalizeFully(serviceName));
      FileUtils.writeStringToFile(javaImplFile, javaImplString, "UTF-8");

      File javaRestFile = new File(implArtifactId + "/src/main/java/org/opencastproject/" + serviceName + "/impl/" + WordUtils.capitalizeFully(serviceName) +"ServiceRestImpl.java");
      String javaRestString = FileUtils.readFileToString(javaRestFile, "UTF-8");
      javaRestString = javaRestString.replaceAll("template", serviceName);
      javaRestString = javaRestString.replaceAll("Template", WordUtils.capitalizeFully(serviceName));
      FileUtils.writeStringToFile(javaRestFile, javaRestString, "UTF-8");

      File javaTestFile = new File(implArtifactId + "/src/test/java/org/opencastproject/" + serviceName + "/impl/" + WordUtils.capitalizeFully(serviceName) +"ServiceImplTest.java");
      String javaTestString = FileUtils.readFileToString(javaTestFile, "UTF-8");
      javaTestString = javaTestString.replaceAll("template", serviceName);
      javaTestString = javaTestString.replaceAll("Template", WordUtils.capitalizeFully(serviceName));
      FileUtils.writeStringToFile(javaTestFile, javaTestString, "UTF-8");

      // Fix the service component files
      File serviceComponentFile = new File(implArtifactId + "/src/main/resources/OSGI-INF/" + serviceName + "-service.xml");
      String serviceComponentString = FileUtils.readFileToString(serviceComponentFile, "UTF-8");
      serviceComponentString = serviceComponentString.replaceAll("template", serviceName);
      serviceComponentString = serviceComponentString.replaceAll("Template", WordUtils.capitalizeFully(serviceName));
      FileUtils.writeStringToFile(serviceComponentFile, serviceComponentString, "UTF-8");
      
      File serviceComponentRestFile = new File(implArtifactId + "/src/main/resources/OSGI-INF/" + serviceName + "-service-rest.xml");
      String serviceComponentRestString = FileUtils.readFileToString(serviceComponentRestFile, "UTF-8");
      serviceComponentRestString = serviceComponentRestString.replaceAll("template", serviceName);
      serviceComponentRestString = serviceComponentRestString.replaceAll("Template", WordUtils.capitalizeFully(serviceName));
      FileUtils.writeStringToFile(serviceComponentRestFile, serviceComponentRestString, "UTF-8");
    } catch (Exception e) {
      throw new MojoExecutionException("failed to generate service " + serviceName, e);
    }
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
