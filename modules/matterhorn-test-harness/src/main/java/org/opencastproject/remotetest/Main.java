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
package org.opencastproject.remotetest;

import org.opencastproject.remotetest.ui.NonExitingSeleniumServer;
import org.opencastproject.remotetest.ui.SeleniumTestSuite;
import org.opencastproject.remotetest.util.TrustedHttpClient;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.openqa.selenium.server.SeleniumServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Runs all of the remote tests
 */
public class Main {

  public static String BASE_URL = "http://localhost:8080";
  public static String USERNAME = "matterhorn_system_account";
  public static String PASSWORD = "CHANGE_ME";
  public static Set<String> BROWSERS = new HashSet<String>();
  public static final String FIREFOX = "*firefox";
  public static final String SAFARI = "*safari";
  public static final String CHROME = "*googlechrome";

  public static final TrustedHttpClient getClient() {
    return new TrustedHttpClient(USERNAME, PASSWORD);
  }

  public static final void returnClient(TrustedHttpClient client) {
    if (client != null) {
      client.shutdown();
    }
  }

  public static final String getBaseUrl() {
    return BASE_URL;
  }

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption(new Option("help", false, "print this message"));
    options.addOption(new Option("withperf", false, "run the performance tests"));
    options.addOption(new Option("withserver", false, "run the tests for the server side components"));
    options.addOption(new Option("withcapture", false, "run the tests for the capture agent"));
    options.addOption(new Option("withff", false, "run the selenium user interface tests with the firefox browser"));
    options.addOption(new Option("withchrome", false, "run the selenium user interface tests with the chrome browser"));
    options.addOption(new Option("withsafari", false, "run the selenium user interface tests with the safari browser"));
    options.addOption(new Option("url", true, "run tests against the Matterhorn installation at this URL"));
    options.addOption(new Option("username", true, "the username to use when accessing the Matterhorn installation"));
    options.addOption(new Option("password", true, "the password to use when accessing the Matterhorn installation"));

    // create the parser
    CommandLineParser parser = new PosixParser();
    List<Class<?>> testClasses = new ArrayList<Class<?>>();
    CommandLine line = null;
    try {
      line = parser.parse(options, args);
    } catch (ParseException e) {
      System.err.println("Parsing commandline failed: " + e.getMessage());
      System.exit(1);
    }

    if (line.hasOption("help")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("java -jar matterhorn-test-harness-<version>-jar-with-dependencies.jar>", options);
      System.exit(0);
    }
    // should we run the server-side tests
    if (line.hasOption("withserver")) {
      System.out.println("Running with the 'server' test suite enabled");
      testClasses.add(ServerTests.class);
      if (line.hasOption("withperf")) {
        System.out.println("Running with the server performance test suite enabled");
        testClasses.add(ServerPerformanceTests.class);
      }
    }
    if (line.hasOption("withcapture")) {
      System.out.println("Running 'capture' test suite");
      testClasses.add(CaptureAgentTests.class);
      if (line.hasOption("withperf")) {
        // TODO: Add capture agent performance tests
        // System.out.println("Running with the 'capture' performance test suite enabled");
      }
    }

    SeleniumServer seleniumServer = null;
    if (line.hasOption("withff") || line.hasOption("withchrome") || line.hasOption("withsafari")) {

      // Add the test suite
      testClasses.add(SeleniumTestSuite.class);

      // Assemble the browsers that the user wants to use in the tests
      if (line.hasOption("withff")) {
        runSeleneseTests(FIREFOX);
        BROWSERS.add(FIREFOX);
      }
      if (line.hasOption("withchrome")) {
        runSeleneseTests(CHROME);
        BROWSERS.add(CHROME);
      }
      if (line.hasOption("withsafari")) {
        runSeleneseTests(SAFARI);
        BROWSERS.add(SAFARI);
      }

      // Start the selenium server
      seleniumServer = new SeleniumServer();
      seleniumServer.start();
    }
    // if we don't have any test classes, add the server (not performance) tests... just so we have *something* to do
    if (testClasses.size() == 0) {
      System.out.println("No test suites specified... running server (not including performance) tests");
      testClasses.add(ServerTests.class);
    }

    if (line.hasOption("url")) {
      BASE_URL = line.getOptionValue("url");
    }
    if (line.hasOption("username")) {
      USERNAME = line.getOptionValue("username");
    }
    if (line.hasOption("password")) {
      PASSWORD = line.getOptionValue("password");
    }

    // run the tests
    System.out.println("Beginning matterhorn test suite on " + BASE_URL);
    Result result = JUnitCore.runClasses(testClasses.toArray(new Class<?>[testClasses.size()]));

    if (seleniumServer != null) {
      seleniumServer.stop();
    }

    // print the results
    System.out.println(result.getRunCount() + " tests run, " + result.getIgnoreCount() + " tests ignored, "
            + result.getFailureCount() + " tests failed.  Total time = " + result.getRunTime() + "ms");
    if (result.getFailureCount() > 0) {
      for (Failure failure : result.getFailures()) {
        System.out.println(failure.getTrace());
      }
      System.exit(1);
    }
  }

  /**
   * Runs the selenese test suite in src/main/resources/selenium/suite.html
   * 
   * @throws Exception
   *           if the selenese tests fail to run
   */
  private static void runSeleneseTests(String browser) throws Exception {
    // Copy the test suite and its associated files to the temp directory
    String time = Long.toString(System.currentTimeMillis());
    File temp = new File(System.getProperty("java.io.tmpdir"), "selenium" + time);
    FileUtils.forceMkdir(temp);
    URL[] urls = getResourceListing("selenium/");
    File testSuite = null;
    for (int i = 0; i < urls.length; i++) {
      URL url = urls[i];
      String filename = FilenameUtils.getName(url.toString());
      if (filename == null || filename.equals("")) {
        continue;
      }
      File outFile = new File(temp, filename);
      InputStream is = url.openStream();
      OutputStream os = new FileOutputStream(outFile);
      IOUtils.copy(is, os);
      IOUtils.closeQuietly(is);
      IOUtils.closeQuietly(os);
      if ("suite.html".equals(filename)) {
        testSuite = outFile;
      }
    }

    File target = new File("target");
    if (!target.isDirectory()) {
      FileUtils.forceMkdir(target);
    }
    File report = new File(target, "selenium_results_" + browser.replaceAll("\\W", "") + "_" + time + ".html");

    // Run the selenese tests
    System.out.println("Beginning selenese tests for the '" + browser + "' browser on " + Main.BASE_URL);
    SeleniumServer seleniumServer = new NonExitingSeleniumServer(testSuite, report, browser);
    seleniumServer.boot();
    seleniumServer.stop();
    System.out.println("Finished selenese tests for the '" + browser + "'.  Results are available at " + report);

    // TODO: interpret whether there are problems in the test report
    FileUtils.forceDelete(temp);
  }

  /**
   * List directory contents for a resource folder. Not recursive. This is basically a brute-force implementation. Works
   * for regular files and also JARs.
   * 
   * @param path
   *          Should end with "/", but not start with one.
   * @return URLs for each item
   * @throws URISyntaxException
   * @throws IOException
   */
  private static URL[] getResourceListing(String path) throws Exception {
    URL dirURL = Main.class.getClassLoader().getResource(path);

    String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); // strip out only the JAR file
    JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
    Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
    Set<URL> result = new HashSet<URL>(); // avoid duplicates in case it is a subdirectory
    while (entries.hasMoreElements()) {
      String name = entries.nextElement().getName();
      if (name.startsWith(path)) { // filter according to the path
        String entry = name.substring(path.length());
        int checkSubdir = entry.indexOf("/");
        if (checkSubdir >= 0) {
          // if it is a subdirectory, we just return the directory name
          entry = entry.substring(0, checkSubdir);
        }
        result.add(new URL(dirURL.toString() + entry));
      }
    }
    return result.toArray(new URL[result.size()]);
  }
}
