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
package org.opencastproject.workflow.impl;

import org.opencastproject.media.mediapackage.jaxb.AttachmentType;
import org.opencastproject.media.mediapackage.jaxb.AttachmentsType;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstanceListImpl;
import org.opencastproject.workflow.api.WorkflowOperationResultImpl;

import junit.framework.Assert;

import org.junit.Test;

public class WorkflowInstanceTest {
  @Test
  public void testWorkflowWithoutOperations() throws Exception {
    WorkflowInstanceImpl workflow = new WorkflowInstanceImpl();
    MediapackageType mpt = new MediapackageType();
    mpt.setId("10.0000/1");
    workflow.setSourceMediaPackageType(mpt);
    Assert.assertEquals("10.0000/1", workflow.getCurrentMediaPackage().getIdentifier().toString());
  }

  @Test
  public void testWorkflowWithOperationsWithResults() throws Exception {
    WorkflowInstanceImpl workflow = new WorkflowInstanceImpl();
    MediapackageType src = new MediapackageType();
    src.setId("10.0000/1");
    workflow.setSourceMediaPackageType(src);

    AttachmentType attachment = new AttachmentType();
    attachment.setUrl("http://test/attachment.txt");
    
    MediapackageType mpt = new MediapackageType();
    mpt.setId("10.0000/1");
    AttachmentsType at = new AttachmentsType();
    at.getAttachment().add(attachment);
    mpt.setAttachments(at);
    
    WorkflowOperationInstanceImpl opInstance = new WorkflowOperationInstanceImpl();
    WorkflowOperationResultImpl result = new WorkflowOperationResultImpl();
    result.setResultingMediaPackage(mpt);
    opInstance.setResult(result);
    WorkflowOperationInstanceListImpl ops = new WorkflowOperationInstanceListImpl();
    ops.add(opInstance);

    workflow.setWorkflowOperationInstanceList(ops);
    
    Assert.assertEquals(1, workflow.getCurrentMediaPackage().getAttachments().length);
  }
}
