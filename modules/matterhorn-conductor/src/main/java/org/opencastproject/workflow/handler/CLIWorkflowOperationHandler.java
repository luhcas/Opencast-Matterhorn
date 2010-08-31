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
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.jaxen.SimpleNamespaceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * A command line interpreter workflow handler to execute non-java workflow operations. TODO: Change the throwing of
 * workflowoperationexceptions to throwing some suitable subclass so the caller can switch on errors as appropriate.
 * TODO: Provide variable passing for executables that operate on elements within the workspace/media packages.
 */
public class CLIWorkflowOperationHandler implements WorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CLIWorkflowOperationHandler.class);

  /** The local workspace */
  private Workspace workspace = null;

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("exec", "The full path the executable to run");
    CONFIG_OPTIONS.put("params", "Space separated list of command line parameters to pass to the executable')");
  }

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   * 
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException {

    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();

    // MediaPackage from previous workflow operations
    MediaPackage srcPackage = workflowInstance.getMediaPackage();
    // Modified media package from our external cli operation, if any
    MediaPackage resultPackage = null;
    // Executable attempting to be invoked
    String exec = operation.getConfiguration("exec");
    // Parameters, like argv[]
    String params = operation.getConfiguration("params");

    // Verify that the executable is not null
    if ((exec == null) || (exec == "")) {
      logger.info("Executable parameter from workflow document is either null or empty: " + exec);
      throw new WorkflowOperationException("Invalid exec param: " + exec);
    }

    // Substitute #{xpath_query} with values from the media package
    try {
      params = substituteVariables(params, srcPackage);
    } catch (Exception e) {
      throw new WorkflowOperationException(
              "Failed to create concrete parameters from list, are the xpath variables set correctly?");
    }

    // Start the external process
    List<String> args = new LinkedList<String>();
    args.add(exec);
    args.addAll(splitParameters(params));

    Process p = null;
    try {
      // Debug output of the Command being run
      if (logger.isDebugEnabled()) {
        StringBuilder sb = new StringBuilder();
        for (String str : args) {
          sb.append(str);
          sb.append(" ");
        }
        logger.debug("Starting subprocess: {}", sb.toString());
      }

      logger.info("Starting subprocess: {}", exec);
      ProcessBuilder pb = new ProcessBuilder(args);
      pb.redirectErrorStream(true); // Unfortunately merges but necessary for deadlock prevention
      p = pb.start();
    } catch (IOException e) {
      // Only log the first argument, the executable, as other arguments may contain sensitive values
      // e.g. MySQL password/user, paths, etc. that should not be shown to caller
      logger.error("Could not start subprocess {}", args.get(0));
      throw new WorkflowOperationException("Could not start subprocess: " + args.get(0));
    }
    // Attempt to read the output of the command and parse it as a media package
    try {
      logger.debug("Reading results from subprocess");
      InputStream in = p.getInputStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      StringBuffer sb = new StringBuffer();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
      in.close();
      logger.info("Subprocess return data: {}", sb.toString());

      // If the response is a media package set the response to it
      try {
        resultPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(sb.toString());
      } catch (Exception e) {
        logger.info("Output text was found but was not deserializable to a MediaPackage object");
      }

      // If the response is a file to a media package set the response to the contents of the file
      BufferedReader fil = null;
      try {
        File f = new File(sb.toString());
        if (f.exists()) {
          StringBuffer fb = new StringBuffer();
          fil = new BufferedReader(new FileReader(f));
          line = fil.readLine();
          while (line != null) {
            fb.append(line);
            line = fil.readLine();
          }

          resultPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(fb.toString());
        }
      } catch (Exception e) {
        logger.info("Output file was found but was not deserializable to a MediaPackage object");
      } finally {
        try {
          fil.close();
        } catch (Exception e) {
          // supressed
        }
      }

    } catch (IOException e) {
      logger.debug("Unable to read output from subprocess.", e);
    }

    // On error return code throw to caller
    int returnCode = 0;
    try {
      returnCode = p.waitFor();
      logger.debug("Subprocess returned code {}", String.valueOf(returnCode));
    } catch (InterruptedException e) {
      throw new WorkflowOperationException("Workflow handler thread interrupted before external process ended.");
    }
    if (returnCode != 0) {
      logger.warn("Non-zero return code from external process");
      throw new WorkflowOperationException("Non-zero return code from external process: " + String.valueOf(returnCode));
    }

    // If there is no resultant mediapackage, pass back the one that was provided to us
    if (resultPackage == null) {
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(srcPackage, Action.CONTINUE);
    }
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(resultPackage, Action.CONTINUE);
  }

  /**
   * This code is a hack to split parameters based on white space, but does not handle quoted strings or escaped
   * whitespace/strings.
   */
  ArrayList<String> splitParameters(String params) {
    ArrayList<String> args = new ArrayList<String>();
    String[] paramary = params.split(" ");
    for (String item : paramary) {
      args.add(item);
    }
    return args;
  }

  /*
   * ArrayList<String> splitParameters(String input){ ArrayList<String> parameters = new ArrayList<String>(); //put the
   * list of all quoted double quote indicies. These should be ignored. int nextQuote=input.indexOf("\"");
   * 
   * return parameters; }
   * 
   * private int findNextQuote(int position, String input){ boolean done=false; int nextQuote=-1 while (!done){
   * nextQuote=input.indexOf("\"", position); if (nextQuote>0){ if
   * (input.substring(nextQuote-1,nextQuote).equals("\\")){ continue; } } } return nextQuote; }
   */

  /**
   * Replaces instances of the substring #{x} with values from the media package. Requires that x is a valid xpath
   * expression.
   * 
   * @param params
   *          May contains xpath expressions in #{}, e.g. "/backups/#{//mediapackage[@id]}"
   * @param mp
   *          The media package xpath elements are relative to
   * @return
   */
  String substituteVariables(String params, MediaPackage mp) throws Exception {
    // Consider several different test cases where the query results in string X:
    // Case 0: -r /bob/#44/{123-123} --> -r /bob/#44/{123-123}
    // Case 1: #{//mediapackage[@id]} --> X
    // Case 2: /backups/#{//mediapackage[@id]} --> /backups/X
    // Case 3: /backups/#{//mediapackage[@id]}/data --> /backups/X/data

    // Tokenize on the substring matches
    String[] tokens = params.split("#\\{");

    // Check the trivial case where the whole word is a string replacement
    // Test Case 1 & 2 & 3 skip; Test case 0 return params
    if (tokens.length == 1) {
      return params;
    }

    // For each odd element, find the matching closing } and break it in two
    ArrayList<String> results = new ArrayList<String>();
    int i = 1;
    while (i < tokens.length) {
      // Test case 1: turn "" into ""
      // Test case 2: turn "/backups/#{" into "/backups/"
      // Test case 3: turn "/backups/#{" into "/backups/"
      String lhs = tokens[i - 1].substring(0, tokens[i - 1].length());
      // Test case 1: turn "//mediapackage[@id]}" into "//mediapackage[@id]"
      // Test case 2: turn "//mediapackage[@id]}" into "//mediapackage[@id]"
      // Test case 3: turn "//mediapackage[@id]}" into "//mediapackage[@id]"
      String xpathExpression = tokens[i].substring(0, tokens[i].indexOf("}"));
      // Test case 1: turn "}" into ""
      // Test case 2: turn "}" into ""
      // Test case 3: turn "}/data" into "/data"
      String rhs = "";
      if (!(tokens[i].endsWith("}"))) {
        rhs = tokens[i].substring(tokens[i].indexOf("}") + 1, tokens[i].length());
      }

      // Get the results of the xpathExpression
      DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document xmldoc = db.parse(new ByteArrayInputStream(mp.toXml().getBytes())); // TODO: should use a better
                                                                                   // serializer, to get local files

      XPath xpath = XPathFactory.newInstance().newXPath();

      SimpleNamespaceContext namespace = new SimpleNamespaceContext();
      // xpath.setNamespaceContext( new MediaPackageNamespace() );
      // InputSource inputSource = new InputSource( new StringReader(mp.toXml( )) );
      NodeList nodes = (NodeList) xpath.evaluate(xpathExpression, xmldoc, XPathConstants.NODESET);
      // String nodeString = xpath.evaluate(xpathExpression, inputSource);

      // Comma separate the nodes in the nodeslist
      // TODO: maybe this should be an arbitrary separator
      int j = 0;
      String result = "";
      while (j < nodes.getLength()) {
        // Special first case where we want no comma
        if (j != 0) {
          result += ",";
        }
        result += nodes.item(j).getTextContent();
        j = j + 1;
      }
      // Put this all in the final result array
      if (results.size() > 0) {
        // previous rhs was this lhs
        results.add(result + rhs);
      } else {
        results.add(lhs + result + rhs);
      }
      // Add this result to our array list
      i += 1;
    }

    StringBuilder br = new StringBuilder();
    for (String result : results) {
      br.append(result);
    }
    return br.toString();
  }

  @Override
  public String getId() {
    return "cli";
  }

  @Override
  public String getDescription() {
    return "Executes command line workflow operations";
  }

  @Override
  public void destroy(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    // Do nothing (nothing to clean up, the command line program should do this itself)
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  class MediaPackageNamespace implements NamespaceContext {
    @Override
    public String getNamespaceURI(String prefix) {
      return "http://mediapackage.opencastproject.org";
    }

    @Override
    public String getPrefix(String namespace) {
      return "ns";
    }

    @Override
    public Iterator getPrefixes(String namespace) {
      return null;
    }
  }

}
