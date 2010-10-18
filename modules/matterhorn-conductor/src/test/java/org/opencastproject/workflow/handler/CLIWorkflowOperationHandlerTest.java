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
package org.opencastproject.workflow.handler;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CLIWorkflowOperationHandlerTest {

  /** the logging facility provided by log4j */
  private final static Logger logger = LoggerFactory.getLogger(CLIWorkflowOperationHandlerTest.class.getName());

  /** True if the environment provides the tools needed for the test suite */
  private static boolean isSane = true;

  /** Represents a tuple of handler and instance, useful for return types */
  protected class InstanceAndHandler {

    public WorkflowInstanceImpl workflowInstance;
    public WorkflowOperationHandler workflowHandler;

    InstanceAndHandler(WorkflowInstanceImpl i, WorkflowOperationHandler h) {
      this.workflowInstance = i;
      this.workflowHandler = h;
    }

  }

  /**
   * Make sure that all of the binaries used by the tests are there.
   */
  @BeforeClass
  public static void textEnvironment() throws IOException {
    File tmp = File.createTempFile("test", "txt");
    Map<String, String> commands = new HashMap<String, String>();
    commands.put("touch", tmp.getAbsolutePath());
    commands.put("echo", "hello");
    commands.put("cat", tmp.getAbsolutePath());

    for (Map.Entry<String, String> command : commands.entrySet()) {
      try {
        Process p = new ProcessBuilder(command.getKey(), command.getValue()).start();
        if (p.waitFor() != 0)
          throw new IllegalStateException("Command '" + command.getKey() + "' failed with status " + p.exitValue());
      } catch (Throwable t) {
        logger.warn("Skipping cli workflow tests due to incomplete environment");
        isSane = false;
      }
    }
  }

  /**
   * Creates a new CLI workflow and readies the engine for processing
   */
  private InstanceAndHandler createCLIWorkflow(String exec, String params) {
    WorkflowOperationHandler cliHandler = new CLIWorkflowOperationHandler();

    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId("workflow-cli-test");
    workflowInstance.setState(WorkflowState.RUNNING);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl();
    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    operation.setConfiguration("exec", exec);
    operation.setConfiguration("params", params);
    workflowInstance.next(); // Simulate starting the workflow
    return new InstanceAndHandler(workflowInstance, cliHandler);
  }

  /**
   * Tests the xpath replacement in the CLI handler
   * 
   * @throws Exception
   */

  @Test
  public void testVariableSubstitution() throws Exception {
    // create a dummy mediapackage
    MediaPackageBuilderFactory factory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder builder = factory.newMediaPackageBuilder();
    MediaPackage mp = builder.createNew(new IdImpl("blah"));
    mp.addContributor("chris");
    mp.addContributor("greg");

    // test the trivial
    InstanceAndHandler tuple = createCLIWorkflow("", "");
    CLIWorkflowOperationHandler handler = (CLIWorkflowOperationHandler) tuple.workflowHandler;

    // test the case where the whole work is a string replacement
    try {
      String s = handler.substituteVariables("#{//mediapackage/@id}", mp);
      Assert.assertTrue(s.equals("blah"));
    } catch (Exception e) {
      Assert.fail("Simple parameter that is a single replacement values failed");
    }

    // test the case where the replacement string has the right characters but is not formed (should just return params
    try {
      String s = handler.substituteVariables("-r /bob/#44/{123-123}", mp);
      Assert.assertTrue(s.equals("-r /bob/#44/{123-123}"));
    } catch (Exception e) {
      Assert.fail("String without replacement variables failed ");
    }

    // test a single substitution
    // this should not throw and exception
    try {
      String result = handler.substituteVariables("/backups/#{//mediapackage/@id}", mp);
      Assert.assertTrue(result.equals("/backups/blah"));
    } catch (Exception e) {
      Assert.fail("String with replacement variables failed ");
    }

    // test a double substitution
    // this should not throw and exception
    try {
      String result = handler.substituteVariables("/backups/#{//mediapackage/@id}/1 /backups/#{//mediapackage/@id}/2",
              mp);
      Assert.assertTrue(result.equals("/backups/blah/1 /backups/blah/2"));
    } catch (Exception e) {
      Assert.fail("String with 2 replacement variables failed ");
    }
    // test a triple substitution
    // this should not throw and exception
    try {
      String result = handler.substituteVariables(
              "/backups/#{//mediapackage/@id}/1 /backups/#{//mediapackage/@id}/2 /backups/#{//mediapackage/@id}/3", mp);
      Assert.assertTrue(result.equals("/backups/blah/1 /backups/blah/2 /backups/blah/3"));
    } catch (Exception e) {
      Assert.fail("String with 3 replacement variables failed ");
    }
    // test substitution with more than one node returned
    try {
      String result = handler.substituteVariables("#{//contributor}", mp);
      Assert.assertTrue(result.equals("chris,greg"));
    } catch (Exception e) {
      Assert.fail("String with multiple nodes in nodeset failed");
    }

  }

  // test that a filename containing media package is returned correctly
  @Test
  public void testMediaPackageReturnFromSubprocessAsFilename() throws Exception {
    if (!isSane)
      return;

    InstanceAndHandler tuple = createCLIWorkflow("echo", "/tmp/mp.xml");

    // create a dummy mediapackage
    MediaPackageBuilderFactory factory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder builder = factory.newMediaPackageBuilder();
    MediaPackage mp = builder.createNew();

    try {
      File file = new File("/tmp/mp.xml");
      BufferedWriter output = new BufferedWriter(new FileWriter(file));
      output.write(mp.toXml());
      output.close();

      MediaPackage returned_mp = tuple.workflowHandler.start(tuple.workflowInstance).getMediaPackage();
      if (returned_mp == null) {
        Assert.fail("A media package was not returned from external process");
      }
      if (!returned_mp.getIdentifier().toString().equals(mp.getIdentifier().toString())) {
        Assert.fail("A valid (identical) media package was not returned");
      }
    } finally {
      try {
        File f = new File("/tmp/mp.xml");
        f.delete();
      } catch (Exception e) {
        // Suppressed
      }
    }
  }

  // test that a media package is returned correctly
  @Test
  public void testMediaPackageReturnFromSubprocess() throws Exception {
    if (!isSane)
      return;

    try {
      InstanceAndHandler tuple = createCLIWorkflow("cat", "/tmp/mp.xml");

      // create a dummy mediapackage
      MediaPackageBuilderFactory factory = MediaPackageBuilderFactory.newInstance();
      MediaPackageBuilder builder = factory.newMediaPackageBuilder();
      MediaPackage mp = builder.createNew();

      File file = new File("/tmp/mp.xml");
      BufferedWriter output = new BufferedWriter(new FileWriter(file));
      output.write(mp.toXml());
      output.close();

      MediaPackage returned_mp = tuple.workflowHandler.start(tuple.workflowInstance).getMediaPackage();
      if (returned_mp == null) {
        Assert.fail("A media package was not returned from external process");
      }
      if (!returned_mp.getIdentifier().toString().equals(mp.getIdentifier().toString())) {
        Assert.fail("A valid (identical) media package was not returned");
      }
    } finally {
      try {
        File f = new File("/tmp/mp.xml");
        f.delete();
      } catch (Exception e) {
        // Suppressed
      }
    }
  }

  @Test
  public void testErrorReturnFailed() throws Exception {
    if (!isSane)
      return;
    try {
      InstanceAndHandler tuple = createCLIWorkflow("touch", "");

      // start the flow
      tuple.workflowHandler.start(tuple.workflowInstance).getMediaPackage();
      Assert.fail("Exception should have been thrown but wasn't.");
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().startsWith("Non-zero"));
    }
  }

  @Test
  public void testNoMediaPackageOperation() throws Exception {
    if (!isSane)
      return;
    try {
      InstanceAndHandler tuple = createCLIWorkflow("touch", "/tmp/me");

      // start the flow
      tuple.workflowHandler.start(tuple.workflowInstance).getMediaPackage();
      File f = new File("/tmp/me");
      Assert.assertTrue(f.exists());
    } finally {
      try {
        File f = new File("/tmp/me");
        f.delete();
      } catch (Exception e) {
        // Suppressed
      }
    }
  }

  @Test
  public void testNoMediaPackageOperationMultipleParameters() throws Exception {
    if (!isSane)
      return;
    try {
      InstanceAndHandler tuple = createCLIWorkflow("touch", "/tmp/me /tmp/and /tmp/you");

      // start the flow
      tuple.workflowHandler.start(tuple.workflowInstance).getMediaPackage();
      File f1 = new File("/tmp/me");
      Assert.assertTrue(f1.exists());
      File f2 = new File("/tmp/and");
      Assert.assertTrue(f2.exists());
      File f3 = new File("/tmp/you");
      Assert.assertTrue(f3.exists());
    } finally {
      try {
        File f = new File("/tmp/me");
        f.delete();

        File f1 = new File("/tmp/and");
        f1.delete();

        File f2 = new File("/tmp/you");
        f2.delete();
      } catch (Exception e) {
        // Suppressed
      }
    }
  }

  @Test
  public void testParametersString() throws Exception {
    InstanceAndHandler tuple = createCLIWorkflow("touch", "/tmp/me /tmp/and /tmp/you");
    CLIWorkflowOperationHandler handler = (CLIWorkflowOperationHandler) tuple.workflowHandler;
    Assert.assertTrue(handler.splitParameters("one two three").size() == 3);
    Assert.assertTrue(handler.splitParameters("\"one \'two\' three\"").get(0).equals("one \'two\' three"));
    Assert.assertTrue(handler.splitParameters("one\\ two three").size() == 2);
  }

}
