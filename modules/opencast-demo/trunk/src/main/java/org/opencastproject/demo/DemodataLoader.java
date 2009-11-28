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
package org.opencastproject.demo;

import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.identifier.Id;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Loads a given set of media packages into the working file repository, then registers a distribution-only workflow and
 * executes it on each of those media packages.
 */
public class DemodataLoader {

  /** Default hostname and port */
  public static final String DEFAULT_HOST = "http://localhost:8080";

  /**
   * Main method that will populate the matterhorn installation running at either localhost or the ip passed in as the
   * first argument.
   * 
   * @param args
   *          program arguments
   */
  public static void main(String[] args) {

    String host = DEFAULT_HOST;
    boolean verbose = true;
    MediaPackageBuilder mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    if (args.length > 2) {
      System.err.println("Wrong number of arguments");
      System.err.println("Usage: demoloader [-q] <host>");
      System.exit(1);
    }

    for (String arg : args) {
      if ("-q".equals(arg)) {
        verbose = false;
      } else {
        try {
          URL url = new URL(args[0]);
          host = url.toExternalForm();
        } catch (MalformedURLException e) {
          System.err.println("Invalid host. Please use http://<hostname>:<portname>");
          System.exit(1);
        }
      }
    }

    HttpClient client = null;

    try {
      client = new DefaultHttpClient();
      String loadDemoDataWorkflow = loadWorkflow("/demo-workflow.xml");
      File[] packages = unzipDemoData("/demo-data.zip");

      for (File packageDir : packages) {
        mpBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(packageDir));
        File manifestFile = new File(packageDir, "index.xml");
        MediaPackage mediaPackage = mpBuilder.loadFromManifest(new FileInputStream(manifestFile));
        Id mediapackageId = mediaPackage.getIdentifier();

        // Upload metadata catalogs to working file repository
        for (Catalog catalog : mediaPackage.getCatalogs()) {
          client = new DefaultHttpClient();
          MultipartEntity postEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
          URI catalogUrl = catalog.getURI();
          String filename = catalogUrl.getPath().substring(catalogUrl.getPath().lastIndexOf('/') + 1);
          URI uploadedCatalogUrl = new URI(host + "/files/" + mediapackageId.compact() + "/" + catalog.getIdentifier());
          postEntity.addPart("file", new InputStreamBody(catalog.getURI().toURL().openStream(), filename));
          HttpPost post = new HttpPost(uploadedCatalogUrl);
          post.setEntity(postEntity);
          client.execute(post);
          catalog.setURI(uploadedCatalogUrl);
        }

        // Serialize the modified media package into a string
        String serializedMediaPackage = null;
        try {
          Writer out = new StringWriter();
          XMLSerializer serializer = new XMLSerializer(out, new OutputFormat(mediaPackage.toXml()));
          serializer.serialize(mediaPackage.toXml());
          serializedMediaPackage = out.toString();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        // Start a workflow instance via the rest endpoint
        HttpPost postStart = new HttpPost(host + "/workflow/rest/start");
        List<NameValuePair> formParams = new ArrayList<NameValuePair>();

        formParams.add(new BasicNameValuePair("definition", loadDemoDataWorkflow));
        formParams.add(new BasicNameValuePair("mediapackage", serializedMediaPackage));
        formParams.add(new BasicNameValuePair("properties", "mediapackage=" + packageDir));
        postStart.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

        if (verbose) {
          System.out.println("Ingesting media package " + mediapackageId);
        }

        // Grab the new workflow instance from the response
        client = new DefaultHttpClient();
        client.execute(postStart);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.getConnectionManager().shutdown();
    }
  }

  protected String getWorkflowInstanceId(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml));
    return ((Element) XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE))
            .getAttribute("id");
  }

  /**
   * Loads the demo workflow from <code>/demo-workflow.xml</code>.
   * 
   * @param workflowName
   *          the workflow file name
   * @return the workflow as a string
   * @throws Exception
   *           if the workflow cannot be loaded
   */
  protected static String loadWorkflow(String workflowName) throws Exception {
    return IOUtils.toString(DemodataLoader.class.getResourceAsStream(workflowName));
  }

  /**
   * Unzips the demo data and returns an array containing the media package folders.
   * 
   * @param zipfile
   *          the zipped demo data
   * @return the media package directories
   */
  protected static File[] unzipDemoData(String zipfile) {
    int BUFFER = 2048;
    try {

      // Create the temporary directories
      File extractDir = new File(System.getProperty("java.io.tmpdir"));
      extractDir.mkdirs();

      // Unzip the demo data
      BufferedOutputStream dest = null;
      InputStream is = DemodataLoader.class.getResourceAsStream(zipfile);
      ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
      ZipEntry entry = null;
      final StringBuffer rootFolder = new StringBuffer();
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          String directoryName = extractDir.getAbsolutePath() + File.separatorChar + entry.getName();
          File f = new File(directoryName);
          if (rootFolder.length() == 0) {
            FileUtils.deleteDirectory(f);
            rootFolder.append(f.getName());
          }
          f.mkdirs();
        } else {
          int count = 0;
          byte data[] = new byte[BUFFER];
          String filename = extractDir.getAbsolutePath() + File.separatorChar + entry.getName();
          FileOutputStream fos = new FileOutputStream(filename);
          dest = new BufferedOutputStream(fos, BUFFER);
          while ((count = zis.read(data, 0, BUFFER)) != -1) {
            dest.write(data, 0, count);
          }
          dest.flush();
          dest.close();
        }
      }
      zis.close();

      File dataFolder = extractDir.listFiles(new FileFilter() {
        public boolean accept(File pathname) {
          return pathname.isDirectory() && pathname.getName().equals(rootFolder.toString());
        }
      })[0];
      return dataFolder.listFiles(new FileFilter() {
        public boolean accept(File pathname) {
          return pathname.isDirectory();
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

}
