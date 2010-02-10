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

import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.identifier.Id;
import org.opencastproject.media.mediapackage.identifier.IdBuilder;
import org.opencastproject.media.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.media.mediapackage.identifier.UUIDIdBuilderImpl;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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
import java.io.OutputStream;
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
      System.setProperty("opencast.idbuilder", UUIDIdBuilderImpl.class.getCanonicalName());
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
    IdBuilder idBuilder = IdBuilderFactory.newInstance().newIdBuilder();
    try {

      mpBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(packageDir));
      File manifestFile = new File(packageDir, "index.xml");
      MediaPackage mediaPackage = mpBuilder.loadFromXml(new FileInputStream(manifestFile));
      addMediaPackageMetadata(mediaPackage);
      Id mediapackageId = idBuilder.createNew();
      mediaPackage.setIdentifier(mediapackageId);
  
      if (verbose) {
        System.out.println("Ingesting media package " + packageDir.getName());
      }
      
      // Copying sample tracks into place
      File trackDir = new File(packageDir, "tracks");
      trackDir.mkdir();
      for (Track track : mediaPackage.getTracks()) {
        String filename = FilenameUtils.getName(track.getURI().toString());
        File outFile = new File(trackDir, filename);
        InputStream is = DemodataLoader.class.getResourceAsStream("/tracks/" + filename);
        OutputStream os = new FileOutputStream(outFile);
        IOUtils.copy(is, os);
        IOUtils.closeQuietly(os);
      }
      
      // Copying sample attachments into place
      File attachmentDir = new File(packageDir, "attachments");
      attachmentDir.mkdir();
      for (Attachment attachment : mediaPackage.getAttachments()) {
        String filename = FilenameUtils.getName(attachment.getURI().toString());
        File outFile = new File(attachmentDir, filename);
        InputStream is = DemodataLoader.class.getResourceAsStream("/attachments/" + filename);
        OutputStream os = new FileOutputStream(outFile);
        IOUtils.copy(is, os);
        IOUtils.closeQuietly(os);
      }

      // Upload media package elements to working file repository
      for (MediaPackageElement element : mediaPackage.getElements()) {
        client = new DefaultHttpClient();
        MultipartEntity postEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        URI elementUrl = element.getURI();
        String filename = FilenameUtils.getName(elementUrl.toString());
        URI uploadedElementUrl = new URI(host + "/files/" + mediapackageId.compact() + "/" + element.getIdentifier());
        postEntity.addPart("file", new InputStreamBody(element.getURI().toURL().openStream(), filename));
        HttpPost post = new HttpPost(uploadedElementUrl);
        post.setEntity(postEntity);
        client.execute(post);
        element.setURI(uploadedElementUrl);
      }
      
      // Start a workflow instance via the rest endpoint
      HttpPost postStart = new HttpPost(host + "/workflow/rest/start");
      List<NameValuePair> formParams = new ArrayList<NameValuePair>();
  
      formParams.add(new BasicNameValuePair("definition", loadDemoDataWorkflow));
      formParams.add(new BasicNameValuePair("mediapackage", mediaPackage.toXml()));
      formParams.add(new BasicNameValuePair("properties", "mediapackage=" + packageDir));
      postStart.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
  
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
        formatter.printHelp("demoloader [url]", cmdOptions);
        System.exit(0);
      }
      return cmd;
    } catch (ParseException e) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("demoloader [url]", cmdOptions);
      throw e;
    }
  }

  /**
   * Reads the available metadata from the dublin core catalog (if there is
   * one).
   * 
   * @param mediapackage
   *          the media package
   */
  private static void addMediaPackageMetadata(MediaPackage mediapackage) {    
    Catalog[] dcs = mediapackage.getCatalogs(DublinCoreCatalog.FLAVOR);
    for (Catalog catalog : dcs) {
      DublinCoreCatalog dc = new DublinCoreCatalogImpl(catalog);
      
      if (dc.getReference() == null) {
  
        // Title
        mediapackage.setTitle(dc.getFirst(DublinCore.PROPERTY_TITLE));
  
        // Created date
        if (dc.hasValue(DublinCore.PROPERTY_CREATED))
          mediapackage.setDate(EncodingSchemeUtils.decodeDate(dc.get(
                  DublinCore.PROPERTY_CREATED).get(0)));
  
        // Series id
        if (dc.hasValue(DublinCore.PROPERTY_IS_PART_OF)) {
          mediapackage.setSeries(dc.get(DublinCore.PROPERTY_IS_PART_OF).get(0).getValue());
        }
  
        // Creator
        if (dc.hasValue(DublinCore.PROPERTY_CREATOR)) {
          for (DublinCoreValue creator : dc.get(DublinCore.PROPERTY_CREATOR)) {
            mediapackage.addCreator(creator.getValue());
          }
        }
  
        // Contributor
        if (dc.hasValue(DublinCore.PROPERTY_CONTRIBUTOR)) {
          for (DublinCoreValue contributor : dc
                  .get(DublinCore.PROPERTY_CONTRIBUTOR)) {
            mediapackage.addContributor(contributor.getValue());
          }
        }
  
        // Subject
        if (dc.hasValue(DublinCore.PROPERTY_SUBJECT)) {
          for (DublinCoreValue subject : dc.get(DublinCore.PROPERTY_SUBJECT)) {
            mediapackage.addSubject(subject.getValue());
          }
        }
  
        // License
        mediapackage.setLicense(dc.getFirst(DublinCore.PROPERTY_LICENSE));
  
        // Language
        mediapackage.setLanguage(dc.getFirst(DublinCore.PROPERTY_LANGUAGE));
  
        break;
      } else {
        // Series Title
        mediapackage.setSeriesTitle(dc.getFirst(DublinCore.PROPERTY_TITLE));
      }
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
