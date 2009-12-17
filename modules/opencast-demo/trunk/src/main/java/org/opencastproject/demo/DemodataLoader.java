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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
    boolean random = false;
    int samples = Integer.MAX_VALUE;

    // Parse commandline
    try {
      CommandLine cmd = setupCommandline(args);

      // Samples
      if (cmd.hasOption('n')) {
        try {
          samples = Integer.parseInt(cmd.getOptionValue('n'));
        } catch (NumberFormatException e) {
          System.err.println("Error parsing number of samples");
          System.exit(1);
        }
      }

      // Verbose?
      verbose = !cmd.hasOption('q');

      // Random selection?
      random = cmd.hasOption('r');

      // Host
      try {
        if (cmd.getArgs().length > 0) {
          URL url = new URL(cmd.getArgs()[0]);
          host = url.toExternalForm();
        } else {
          host = "http://localhost:8080";
        }
      } catch (MalformedURLException e) {
        System.err.println("Invalid host. Please use http://<hostname>:<portname>");
        System.exit(1);
      }

    } catch (ParseException e) {
      System.exit(1);
    }
    
    // Load the data
    try {
      File[] packages = unzipDemoData("/demo-data.zip");
      if (!random) {
        for (File packageDir : packages) {
          if (samples <= 0)
            break;
          loadSample(packageDir, host, verbose);
          samples--;
        }
      } else {
        samples = Math.min(samples, packages.length);
        Random rdm = new Random();
        while (samples > 0) {
          loadSample(packages[rdm.nextInt(packages.length)], host, verbose);
          samples--;
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Loads the given media package into the matterhorn installation.
   * 
   * @param packageDir
   *          the mediapackage root directory
   */
  protected static void loadSample(File packageDir, String host, boolean verbose) throws Exception {
    HttpClient client = new DefaultHttpClient();
    String loadDemoDataWorkflow = loadWorkflow("/demo-workflow.xml");
    MediaPackageBuilder mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    try {

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
  
      // Do the xml serialization
      String serializedMediaPackage = null;
      Writer out = new StringWriter();
      DOMSource domSource = new DOMSource(mediaPackage.toXml());
      StreamResult streamResult = new StreamResult(out);
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer serializer = tf.newTransformer();
      serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      serializer.setOutputProperty(OutputKeys.INDENT, "yes");
      serializer.transform(domSource, streamResult);
      serializedMediaPackage = out.toString();
  
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
    } finally {
      client.getConnectionManager().shutdown();
    }
  }

  /**
   * Creates the commandline options.
   * 
   * @param args
   *          the commandline arguments
   * @throws ParseException
   *           If there are errors parsing the commandline
   */
  @SuppressWarnings("static-access")
  protected static CommandLine setupCommandline(String[] args) throws ParseException {
    Options cmdOptions = new Options();
    cmdOptions.addOption("h", "help", false, "display this help screen");
    cmdOptions.addOption("n", true, "number of datasets to load");
    cmdOptions.addOption("q", "quiet", false, "be quiet, don't add verbose output");
    cmdOptions.addOption("r", "random", false, "choose samples randomly");
    cmdOptions.addOption(OptionBuilder.hasArgs().withArgName("number").withDescription("number of samples to load")
            .withLongOpt("number").create("n"));
    CommandLineParser cmdParser = new GnuParser();
    CommandLine cmd = null;
    try {
      cmd = cmdParser.parse(cmdOptions, args);
      if (cmd.hasOption('h')) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("demoloader", cmdOptions);
        System.exit(0);
      }
      return cmd;
    } catch (ParseException e) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("demoloader", cmdOptions);
      throw e;
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
