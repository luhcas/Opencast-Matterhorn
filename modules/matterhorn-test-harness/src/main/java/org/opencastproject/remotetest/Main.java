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

import org.opencastproject.remotetest.util.TrustedHttpClient;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs all of the remote tests
 */
public class Main {

  public static String BASE_URL = "http://localhost:8080";
  public static String USERNAME = "matterhorn_system_account";
  public static String PASSWORD = "CHANGE_ME";

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

  public static void main(String[] args) {
    Options options = new Options();
    options.addOption(new Option("help", false, "print this message"));
    options.addOption(new Option("withperf", false, "run the performance tests"));
    options.addOption(new Option("withserver", false, "run the tests for the server side components"));
    options.addOption(new Option("withcapture", false, "run the tests for the capture agent"));
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
}
