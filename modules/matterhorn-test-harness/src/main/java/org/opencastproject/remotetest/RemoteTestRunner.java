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

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * Runs all of the remote tests
 */
public class RemoteTestRunner {
  public static String BASE_URL = "http://localhost:8080";
  public static void main(String[] args) {
    if(args.length == 2) {
      BASE_URL = args[1];
    }

    System.out.println("Beginning matterhorn test suite on " + BASE_URL);

    Result result = null;
    if(args.length == 0) {
      result = JUnitCore.runClasses(AllTests.class);
    } else if("all".equals(args[0])) {
      result = JUnitCore.runClasses(AllTests.class);
    } else if("server".equals(args[0])) {
      result = JUnitCore.runClasses(ServerTests.class);
    } else if("capture".equals(args[0])) {
      result = JUnitCore.runClasses(CaptureAgentTests.class);
    } else {
      System.err.println("Usage: java -jar matterhorn-test-harness-<version>.jar [all|server|capture] [url]");
      System.exit(1);
    }
    
    System.out.println(result.getRunCount() + " tests run, " + result.getIgnoreCount() + " tests ignored, "
            + result.getFailureCount() + " tests failed.  Total time = " + result.getRunTime() + "ms");
    if(result.getFailureCount() > 0) {
      for(Failure failure : result.getFailures()) {
        System.out.println(failure.getTrace());
      }
      System.exit(1);
    }
  }
}
