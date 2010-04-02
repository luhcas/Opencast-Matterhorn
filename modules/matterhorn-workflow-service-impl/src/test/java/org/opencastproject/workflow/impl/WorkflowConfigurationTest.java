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
package org.opencastproject.workflow.impl;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowConfiguration;
import org.opencastproject.workflow.api.WorkflowConfigurationImpl;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowSelectionStrategy;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorkflowConfigurationTest {
  @Test
  public void testConfigurationSerialization() throws Exception {
    WorkflowOperationInstanceImpl op = new WorkflowOperationInstanceImpl();
    Set<WorkflowConfiguration> config = new HashSet<WorkflowConfiguration>();
    config.add(new WorkflowConfigurationImpl("this", "that"));
    op.setConfiguration(config);
    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
    ops.add(op);
    instance.setOperations(ops);
    String xml = WorkflowBuilder.getInstance().toXml(instance);
    System.out.println(xml);
    Assert.assertTrue(xml.contains("<configurations><configuration key=\"this\">that</configuration></configurations>"));
  }
  
  @Test
  public void testSelectWorkflowDefinition() throws Exception {
    WorkflowServiceImpl service = new WorkflowServiceImpl();
    
    WorkflowDefinitionImpl def1 = new WorkflowDefinitionImpl();
    def1.setId("from-config");
    service.registerWorkflowDefinition(def1);

    WorkflowDefinitionImpl def2 = new WorkflowDefinitionImpl();
    def2.setId("from-strategy");
    service.registerWorkflowDefinition(def2);
        
    ServiceReference ref = EasyMock.createNiceMock(ServiceReference.class);
    EasyMock.replay(ref);
    
    BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getServiceReference(WorkflowSelectionStrategy.class.getName())).andReturn(null);
    EasyMock.expect(bundleContext.getServiceReference(WorkflowSelectionStrategy.class.getName())).andReturn(ref);
    EasyMock.expect(bundleContext.getService(ref)).andReturn(new SimpleStrategy(def2));
    EasyMock.expect(bundleContext.getProperty(WorkflowServiceImpl.WORKFLOW_DEFINITION_DEFAULT)).andReturn(def1.getId());
    EasyMock.replay(bundleContext);
    
    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bundleContext).anyTimes();
    EasyMock.replay(cc);
    
    service.componentContext = cc;
    
    // The first test doesn't have any WorkflowSelectionStrategy's registered
    Assert.assertEquals(def1, service.getWorkflowDefinition(null, null));
    
    // TODO The next test returns a WorkflowSelectionStrategy that always returns the "from-strategy" workflow
    Assert.assertEquals(def2, service.getWorkflowDefinition(null, null));

    
  }
  
  class SimpleStrategy implements WorkflowSelectionStrategy {
    WorkflowDefinition def;

    SimpleStrategy(WorkflowDefinition def) {
      this.def = def;
    }
    
    public WorkflowDefinition getWorkflowDefinition(MediaPackage mediaPackage, Map<String, String> properties) {
      return def;
    }
  }
}
