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
package org.opencastproject.conductor.impl;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.DublinCoreCatalog;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.dublincore.DublinCore;
import org.opencastproject.media.mediapackage.dublincore.DublinCoreValue;
import org.opencastproject.media.mediapackage.dublincore.utils.EncodingSchemeUtils;
import org.opencastproject.media.mediapackage.dublincore.utils.Precision;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Workflow operation used to inspect all tracks of a media package.
 */
public class InspectWorkflowOperationHandler implements
        WorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ComposeWorkflowOperationHandler.class);

  /** The inspection service */
  private MediaInspectionService inspectionService = null;

  /** The local workspace */
  private Workspace workspace = null;

  /**
   * Callback for the OSGi declarative services configuration.
   * 
   * @param inspectionService
   *          the inspection service
   */
  protected void setInspectionService(MediaInspectionService inspectionService) {
    this.inspectionService = inspectionService;
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
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#run(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult run(WorkflowInstance workflowInstance)
          throws WorkflowOperationException {
    MediaPackage mediaPackage = MediaPackageUtil.clone(workflowInstance.getCurrentMediaPackage());

    // Inspect the tracks
    for (Track track : mediaPackage.getTracks()) {
      
      logger.info("Inspecting track '{}' of {}", track.getIdentifier(), mediaPackage);
      Track inspectedTrack = inspectionService.enrich(track, false);
      if (inspectedTrack == null)
        throw new WorkflowOperationException("Track " + track + " could not be inspected");
      
      // Replace the original track with the inspected one
      try {
        mediaPackage.remove(track);
        mediaPackage.add(inspectedTrack);
      } catch (UnsupportedElementException e) {
        logger.error("Error adding {} to media package", inspectedTrack, e);
      }
      
      // Update dublin core with metadata
      try {
        updateDublinCore(mediaPackage);
      } catch (Exception e) {
        logger.warn("Unable to update dublin core data: {}", e.getMessage(), e);
      }
      
    }
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(mediaPackage, null, false);

  }
  
  /**
   * Updates those dublin core fields that can be gathered from the technical metadata.
   * 
   * @param mediaPackage 
   *            the media package
   */
  private void updateDublinCore(MediaPackage mediaPackage) throws Exception {
    // Complete dublin core (if available)
    Catalog dcCatalogs[] = mediaPackage.getCatalogs(DublinCoreCatalog.FLAVOR, MediaPackageReferenceImpl.ANY_MEDIAPACKAGE);
    if (dcCatalogs.length > 0) {
      DublinCoreCatalog dublinCore = (DublinCoreCatalog) dcCatalogs[0];
      Date today = new Date();
      
      // Extent
      if (!dublinCore.hasValue(DublinCore.PROPERTY_EXTENT)) {
        DublinCoreValue extent = EncodingSchemeUtils.encodeDuration(mediaPackage.getDuration());
        dublinCore.set(DublinCore.PROPERTY_EXTENT, extent);
        logger.debug("Setting dc:extent to '{}'", extent.getValue());
      }
      
      // Date created
      if (!dublinCore.hasValue(DublinCore.PROPERTY_CREATED)) {
        DublinCoreValue date = EncodingSchemeUtils.encodeDate(today, Precision.Day);
        dublinCore.set(DublinCore.PROPERTY_CREATED, date);
        logger.debug("Setting dc:date to '{}'", date.getValue());
      }
      
      // Serialize changed dublin core
      // TODO: this is too complicated!
      DOMSource source = new DOMSource(dublinCore.toXml());
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      StreamResult result = new StreamResult(os);
      transformer.transform(source, result);
      InputStream is = new ByteArrayInputStream(os.toByteArray());
      workspace.delete(mediaPackage.getIdentifier().toString(), dublinCore.getIdentifier());
      workspace.put(mediaPackage.getIdentifier().toString(), dublinCore.getIdentifier(), is);
    }
  }

}
