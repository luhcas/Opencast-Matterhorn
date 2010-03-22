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
import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageMetadata;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.metadata.api.MediaPackageMetadataService;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Workflow operation used to inspect all tracks of a media package.
 */
public class InspectWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ComposeWorkflowOperationHandler.class);

  /** The inspection service */
  private MediaInspectionService inspectionService = null;

  /** The local workspace */
  private Workspace workspace = null;

  /** The dublin core catalog service */
  private DublinCoreCatalogService dcService;

  private SortedSet<MediaPackageMetadataService> metadataServices;

  public InspectWorkflowOperationHandler () {
    metadataServices = new TreeSet<MediaPackageMetadataService>(new Comparator<MediaPackageMetadataService>() {
      @Override
      public int compare(MediaPackageMetadataService o1, MediaPackageMetadataService o2) {
        return o1.getPriority() - o2.getPriority();
      }
    });
  }

  public void setDublincoreService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }
  
  public void addMetadataService(MediaPackageMetadataService service) {
    metadataServices.add(service);
  }

  public void removeMetadataService(MediaPackageMetadataService service) {
    metadataServices.remove(service);
  }
  
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
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance)
          throws WorkflowOperationException {
    MediaPackage mediaPackage = (MediaPackage)workflowInstance.getMediaPackage().clone();
    // Populate the mediapackage with any metadata found in its catalogs
    populateMediaPackageMetadata(mediaPackage);
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
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(mediaPackage, Action.CONTINUE);

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
      DublinCoreCatalog dublinCore = dcService.load(dcCatalogs[0]);
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
      // TODO: this is too complicated! Add update() method to metadata service
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      dublinCore.toXml(out, true);
      InputStream in = new ByteArrayInputStream(out.toByteArray());
      String mpId = mediaPackage.getIdentifier().toString();
      String elementId = dublinCore.getIdentifier();
      workspace.delete(mpId, elementId);
      workspace.put(mpId, elementId, "dublincore.xml", in);
      dcCatalogs[0].setURI(workspace.getURI(mpId, elementId));
    }
  }

  /**
   * Reads the available metadata from the dublin core catalog (if there is
   * one).
   * 
   * @param mp
   *          the media package
   */
  private void populateMediaPackageMetadata(MediaPackage mp) {
    if(metadataServices.size() == 0) {
      logger.debug("No metadata services are registered with the ingest service, so no mediapackage metadata can be extracted from catalogs");
      return;
    }
    for(MediaPackageMetadataService metadataService : metadataServices) {
      MediaPackageMetadata metadata = metadataService.getMetadata(mp);
      if(metadata != null) {
        if(mp.getDate().getTime() == 0) {
          mp.setDate(metadata.getDate());
        }
        if(mp.getLanguage() == null || mp.getLanguage().isEmpty()) {
          mp.setLanguage(metadata.getLanguage());
        }
        if(mp.getLicense() == null || mp.getLicense().isEmpty()) {
          mp.setLicense(metadata.getLicense());
        }
        if(mp.getSeries() == null || mp.getSeries().isEmpty()) {
          mp.setSeries(metadata.getSeriesIdentifier());
        }
        if(mp.getSeriesTitle() == null || mp.getSeriesTitle().isEmpty()) {
          mp.setSeriesTitle(metadata.getSeriesTitle());
        }
        if(mp.getTitle() == null || mp.getTitle().isEmpty()) {
          mp.setTitle(metadata.getTitle());
        }
      }
    }
  }
}
