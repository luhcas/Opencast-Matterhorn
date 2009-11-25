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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
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
    
    // See if a url has been passed
    if (args.length > 0) {
      try {
        URL url = new URL(args[0]);
        host = url.toExternalForm();
      } catch (MalformedURLException e) {
        System.err.println("The argument is not a valid url. Please use http://<hostname>:<portname>");
        System.exit(1);
      }
    }

    HttpClient client = null;
    
    try {
      client = new DefaultHttpClient();
      String loadDemoDataWorkflow = loadWorkflow("/demo-workflow.xml");
      File[] packages = unzipDemoData("/demodata.zip");

      XPath xpath = XPathFactory.newInstance().newXPath();
      
      for (File packageDir : packages) {
        String mediapackageId = packageDir.getName();
        File manifestFile = new File(packageDir, "index.xml");
        Object result = xpath.evaluate("//catalog", new InputSource(new FileInputStream(manifestFile)), XPathConstants.NODESET);
        NodeList nodes = (NodeList)result;
        for (int i=0; i < nodes.getLength(); i++) {
          Node n = nodes.item(i);
          String elementId = n.getAttributes().getNamedItem("id").getNodeValue();
          String url = null;
          String filename = null;
          NodeList childNodes = n.getChildNodes();
          for (int j=0; j < childNodes.getLength(); j++) {
            Node childNode = childNodes.item(j);
            if (childNode.getNodeName().equals("url")) {
              url = packageDir.getAbsolutePath() + File.separatorChar + childNode.getFirstChild().getNodeValue();
              filename = url.substring(url.lastIndexOf('/') + 1);
              break;
            }
          }

          MultipartEntity postEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
          postEntity.addPart("file", new InputStreamBody(new FileInputStream(url), filename));
          HttpPost post = new HttpPost(host + "/files/" + mediapackageId + "/" + elementId);
          post.setEntity(postEntity);
          client.execute(post);
        }        
        
        // Start a workflow instance via the rest endpoint
        HttpPost postStart = new HttpPost(host + "/workflow/rest/start");
        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    
        formParams.add(new BasicNameValuePair("definition", loadDemoDataWorkflow));
        formParams.add(new BasicNameValuePair("mediapackage", loadMediapackage(packageDir)));
        formParams.add(new BasicNameValuePair("properties", "mediapackage=" + packageDir));
        postStart.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
    
        // Grab the new workflow instance from the response
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
   * Loads the manifest from the given media package directory and returns it as a string.
   * 
   * @param packageDir
   *          root directory of the media package
   * @return the manifest
   * @throws Exception
   *           if loading the manifest fails
   */
  protected static String loadMediapackage(File packageDir) throws Exception {
    return IOUtils.toString(new FileInputStream(new File(packageDir, "index.xml")));
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
      File tmpDir = new File(System.getProperty("java.io.tmpdir"));
      File extractDir = new File(tmpDir, "demodata");
      extractDir.mkdirs();

      // Unzip the demo data
      BufferedOutputStream dest = null;
      InputStream is = DemodataLoader.class.getResourceAsStream(zipfile);
      ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
      ZipEntry entry = null;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          String directoryName = extractDir.getAbsolutePath() + File.separatorChar + entry.getName();
          new File(directoryName).mkdirs();
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
          return pathname.isDirectory() && pathname.getName().equals("demodata");
        }
      })[0];
      return dataFolder.listFiles();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

}
