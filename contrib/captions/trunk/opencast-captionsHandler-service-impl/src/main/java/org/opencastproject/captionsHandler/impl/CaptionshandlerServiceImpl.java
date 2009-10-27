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
package org.opencastproject.captionsHandler.impl;

import org.opencastproject.captionsHandler.api.CaptionshandlerEntity;
import org.opencastproject.captionsHandler.api.CaptionshandlerService;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationDefinitionListImpl;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The captions handler service <br/>
 * Search will provide means of searching for media packages without timed text catalogs <br/>
 * The consumer will present a list of those media packages <br/>
 * The consumer sends back timed text, which will be stored in the working file repository and added to the media package <br/>
 * A new workflow (or the existing one with additional operations) will be kicked off (for redistribution including captions)
 */
public class CaptionshandlerServiceImpl implements CaptionshandlerService, ManagedService {

  private static final Logger logger = LoggerFactory.getLogger(CaptionshandlerServiceImpl.class);

  private Workspace workspace;
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
  public void unsetWorkspace(Workspace workspace) {
    this.workspace = null;
  }

  private SearchService searchService;
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }
  public void unsetSearchService(SearchService searchService) {
    this.searchService = null;
  }

  private WorkflowService workflowService;
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }
  public void unsetWorkflowService(WorkflowService workflowService) {
    this.workflowService = null;
  }

  public CaptionshandlerServiceImpl() {
    loadStuff();
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {
    // Update any configuration properties here
  }

  /**
   * Get the list of all captionable media packages,
   * currently this will simply retrieve media packages by date,
   * this will probably be overridden for local implementations
   * 
   * @param start the start item (for paging), <=0 for first item
   * @param max the maximum items to return (for paging), <=0 for up to 50 items
   * @param sort the sort order string (e.g. 'title asc')
   * @return the list of MediaPackage or empty if none
   */
  public List<MediaPackage> getCaptionableMedia(int start, int max, String sort) {
    List<MediaPackage> l = new ArrayList<MediaPackage>();
    if (start < 0) {
      start = 0;
    }
    if (max < 0 || max > 50) {
      max = 50;
    }
    // TODO make this actually get the captionable items
    WorkflowSet wfs = workflowService.getWorkflowsByDate(start, max);
    WorkflowInstance[] workflows = wfs.getItems();
    for (WorkflowInstance workflow : workflows) {
      MediaPackage mp = workflow.getSourceMediaPackage();
      l.add(mp);
    }
    if (l.isEmpty()) {
      // TODO make this actually get the captionable items
      SearchResult result = searchService.getEpisodesByDate(max, start);
      SearchResultItem[] items = result.getItems();
      for (SearchResultItem searchResultItem : items) {
        MediaPackage mp = searchResultItem.getMediaPackage();
        l.add(mp);
      }
    }
    return l;
  }

  /**
   * Update a media package with some captions data
   * 
   * @param mediaId the media package id
   * @param captionType the type of caption data (could be {@value #CAPTIONS_TYPE_TIMETEXT} or {@value #CAPTIONS_TYPE_DESCAUDIO} for example)
   * @param data the captions data to store with the media package
   * @return the updated media package
   */
  public MediaPackage updateCaptions(String mediaId, String captionType, InputStream data) {
    if (mediaId == null || "".equals(mediaId)) {
      throw new IllegalArgumentException("mediaId must be set");
    }
    if (captionType == null || "".equals(captionType)) {
      throw new IllegalArgumentException("captionType must be set");
    }
    if (data == null) {
      throw new IllegalArgumentException("data must be set");
    }
    // TODO is this the right way to get the media package from the id?
    // find the media package given the id
    WorkflowSet wfs = workflowService.getWorkflowsByMediaPackage(mediaId);
    WorkflowInstance[] workflows = wfs.getItems();
    MediaPackage mp = null;
    if (workflows.length > 0) {
      mp = workflows[0].getSourceMediaPackage();
      // get the MP and update it
      String mediaPackageElementID = CAPTIONS_ELEMENT+captionType;
      URL url = workspace.put(mediaId, mediaPackageElementID, data);

      for (int i = 0; i < workflows.length; i++) {
        WorkflowInstance workflow = workflows[i];
        if (WorkflowInstance.State.SUCCEEDED.equals(workflow.getState())) {
          MediaPackage mediaPackage = workflow.getSourceMediaPackage();
          addCaptionToMediaPackage(mediaPackage, url);
          WorkflowDefinitionImpl workflowDefinition = new WorkflowDefinitionImpl();
          workflowDefinition.setTitle("Captions Added");
          workflowDefinition.setDescription("Captions added workflow for media: " + mediaId);
          // TODO what is this and what do I do with it?
          workflowDefinition.setOperations(new WorkflowOperationDefinitionListImpl());
          workflowService.start(workflowDefinition, mp, null);
        } else if (WorkflowInstance.State.PAUSED.equals(workflow.getState())) {
          MediaPackage mediaPackage = workflow.getSourceMediaPackage();
          addCaptionToMediaPackage(mediaPackage, url);
          workflowService.resume(workflow.getId());
        } else {
          
        }
      }
    } else {
      throw new IllegalArgumentException("No media found with the given id: " + mediaId);
    }
/*
    SearchResult result = searchService.getEpisodeById(mediaId);
    SearchResultItem[] items = result.getItems();
    MediaPackage mp;
    if (items.length > 0) {
      mp = items[0].getMediaPackage();
    } else {
      throw new IllegalArgumentException("No media found with the given id: " + mediaId);
    }
    String mediaPackageElementID = CAPTIONS_ELEMENT+"-"+captionType;
    workspace.put(mediaId, mediaPackageElementID, data);
    // TODO do I need to get it again to find out the URL of the stuff I added?
    // start off a workflow to indicate the captions just changed
    WorkflowDefinitionImpl workflowDefinition = new WorkflowDefinitionImpl();
    workflowDefinition.setTitle("Captions Added");
    workflowDefinition.setDescription("Captions added workflow for media: " + mediaId);
    // TODO what is this and what do I do with it?
    workflowDefinition.setOperations(new WorkflowOperationDefinitionListImpl());
    workflowService.start(workflowDefinition, mp, null);
*/
    return mp;
  }

  private void addCaptionToMediaPackage(MediaPackage mediaPackage, URL url) {
    MediaPackageElementBuilder mpeb = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    try {
      MediaPackageElement element = mpeb.elementFromURL(url, MediaPackageElement.Type.Attachment, MediaPackageElements.INDEFINITE_TRACK);
      mediaPackage.add(element);
    } catch (MediaPackageException e) {
      e.printStackTrace();
      throw new IllegalStateException("Failed while adding caption to media package ("+mediaPackage.getIdentifier()+"):" + e);
    } catch (UnsupportedElementException e) {
      e.printStackTrace();
      throw new IllegalStateException("Failed while adding caption to media package ("+mediaPackage.getIdentifier()+"):" + e);
    }
  }
  
  
  // DEFAULT STUFF

  Map<String, CaptionshandlerEntity> map;
  
  public void loadStuff() {
    map = new HashMap<String, CaptionshandlerEntity>();
    CaptionshandlerEntityImpl entity = new CaptionshandlerEntityImpl();
    entity.setId("1");
    entity.setTitle("Test Title");
    entity.setDescription("Test Description");
    map.put("1", entity);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.captionsHandler.api.CaptionshandlerService#getEntity(java.lang.String)
   */
  public CaptionshandlerEntity getCaptionshandlerEntity(String id) {
    CaptionshandlerEntity entity = map.get(id);
    logger.info("returning " + entity + " for id=" + id);
    return entity;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.captionsHandler.api.CaptionshandlerService#setObject(java.lang.String, org.opencastproject.captionsHandler.api.CaptionshandlerEntity)
   */
  public void saveCaptionshandlerEntity(CaptionshandlerEntity entity) {
    String id = entity.getId();
    logger.info("setting id=" + id + " to " + entity);
    map.put(id, entity);
  }

}

