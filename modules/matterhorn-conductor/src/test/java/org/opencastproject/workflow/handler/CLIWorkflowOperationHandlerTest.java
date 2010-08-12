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

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageImpl;
import org.opencastproject.mediapackage.MediaPackageMetadata;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.remote.api.Receipt;
import org.opencastproject.remote.api.Receipt.Status;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CLIWorkflowOperationHandlerTest {
  /** the logging facility provided by log4j */
  private final static Logger logger = LoggerFactory.getLogger(CLIWorkflowOperationHandlerTest.class.getName());

  /** Represents a tuple of handler and instance, useful for return types */
  protected class InstanceAndHandler{
    public WorkflowInstanceImpl workflowInstance;
    public WorkflowOperationHandler workflowHandler;
    
    InstanceAndHandler(WorkflowInstanceImpl i, WorkflowOperationHandler h){
      this.workflowInstance = i;
      this.workflowHandler = h;
    }
    
  }  
  
  @Before
  public void setup() throws Exception {
  }

  //most of these tests just work on linux atm
  private boolean isLinux(){
    return System.getProperty("os.name").toLowerCase().equals("linux");
  }
  
  /**
   * Creates a new CLI workflow and readies the engine for processing
   */
  private InstanceAndHandler createCLIWorkflow( String exec , String params ){
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
    return new InstanceAndHandler(workflowInstance,cliHandler);
  }
  
  //test that a filename containing media package is returned correctly
  @Test
  public void testMediaPackageReturnFromSubprocessAsFilename() throws Exception{
    if ( isLinux() ){   
      InstanceAndHandler tuple = createCLIWorkflow("/bin/echo","/tmp/mp.xml");

      //create a dummy mediapackage
      MediaPackageBuilderFactory factory = MediaPackageBuilderFactory.newInstance();
      MediaPackageBuilder builder = factory.newMediaPackageBuilder();
      MediaPackage mp = builder.createNew();
      
      try{        
        File file = new File("/tmp/mp.xml");
        BufferedWriter output = new BufferedWriter(new FileWriter(file));
        output.write( mp.toXml() );
        output.close();
        
        MediaPackage returned_mp = tuple.workflowHandler.start(tuple.workflowInstance).getMediaPackage();
        if (returned_mp == null ){
          Assert.fail("A media package was not returned from external process");
        }
        if (!returned_mp.getIdentifier().toString().equals(mp.getIdentifier().toString())){
          Assert.fail("A valid (identical) media package was not returned");
        }
      }
      finally{
        try{
          File f = new File("/tmp/mp.xml");
          f.delete();
        }
        catch(Exception e){
          //Suppressed
        }
      }
    }
  }

  
  //test that a media package is returned correctly
  @Test
  public void testMediaPackageReturnFromSubprocess() throws Exception{
    if ( isLinux() ){
      try{
        InstanceAndHandler tuple = createCLIWorkflow("/bin/cat","/tmp/mp.xml");
        
        //create a dummy mediapackage
        MediaPackageBuilderFactory factory = MediaPackageBuilderFactory.newInstance();
        MediaPackageBuilder builder = factory.newMediaPackageBuilder();
        MediaPackage mp = builder.createNew();
        
        File file = new File("/tmp/mp.xml");
        BufferedWriter output = new BufferedWriter(new FileWriter(file));
        output.write( mp.toXml() );
        output.close();
        
        MediaPackage returned_mp = tuple.workflowHandler.start(tuple.workflowInstance).getMediaPackage();
        if (returned_mp == null ){
          Assert.fail("A media package was not returned from external process");
        }
        if (!returned_mp.getIdentifier().toString().equals(mp.getIdentifier().toString())){
          Assert.fail("A valid (identical) media package was not returned");
        }
      }
      finally{
        try{
          File f = new File("/tmp/mp.xml");
          f.delete();
        }
        catch(Exception e){
          //Suppressed
        }
      }
    }
  }

  @Test
  public void testErrorReturnFailed() throws Exception{
    if ( isLinux() ){
      try{
        InstanceAndHandler tuple = createCLIWorkflow("/usr/bin/touch","");
        
        //start the flow
        tuple.workflowHandler.start(tuple.workflowInstance).getMediaPackage();
        Assert.fail("Exception should have been thrown but wasn't.");       
      }
      catch (Exception e){
        Assert.assertTrue( e.getMessage().startsWith("Non-zero"));        
      }
    }
  }

  @Test
  public void testNoMediaPackageOperation() throws Exception{
    if ( isLinux() ){
      try{
        InstanceAndHandler tuple = createCLIWorkflow("/usr/bin/touch","/tmp/me");
        
        //start the flow
        tuple.workflowHandler.start(tuple.workflowInstance).getMediaPackage();
        File f = new File("/tmp/me");
        Assert.assertTrue( f.exists() );
      }
      finally{
        try{
          File f = new File("/tmp/me");
          f.delete();
        }
        catch(Exception e){
          //Suppressed
        }
      }
    }
  }
}