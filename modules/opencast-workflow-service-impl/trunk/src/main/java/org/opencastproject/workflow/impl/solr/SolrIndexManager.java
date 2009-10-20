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

package org.opencastproject.workflow.impl.solr;

import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.DublinCoreCatalog;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.dublincore.DublinCore;
import org.opencastproject.media.mediapackage.dublincore.utils.DCMIPeriod;
import org.opencastproject.media.mediapackage.dublincore.utils.EncodingSchemeUtils;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest.ACTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Utility class used to manage the workflow database.
 */
public class SolrIndexManager {

  /** Logging facility */
  static Logger log_ = LoggerFactory.getLogger(SolrIndexManager.class);

  /** Connection to the database */
  private SolrConnection solrConnection = null;

  /**
   * Creates a new management instance for the workflow database.
   * 
   * @param connection
   *          connection to the database
   */
  public SolrIndexManager(SolrConnection connection) {
    if (connection == null)
      throw new IllegalArgumentException("Unable to manage solr with null connection");
    this.solrConnection = connection;
  }

  /**
   * Clears the workflow database. Make sure you know what you are doing.
   * 
   * @throws SolrServerException
   *           if an errors occurs while talking to solr
   */
  public void clear() throws SolrServerException {
    UpdateRequest solrRequest = new UpdateRequest();
    solrRequest.deleteByQuery("*:*");
    solrRequest.setAction(ACTION.COMMIT, true, true);
    try {
      solrConnection.update(solrRequest);
    } catch (Exception e) {
      log_.error("Cannot clear solr index");
    }
  }

  /**
   * Removes the entry with the given <code>id</code> from the database.
   * 
   * @param id
   *          identifier of the series or episode to delete
   * @throws SolrServerException
   *           if an errors occurs while talking to solr
   */
  public boolean delete(String id) throws SolrServerException {
    UpdateRequest solrRequest = new UpdateRequest();
    solrRequest.deleteById(id);
    solrRequest.setAction(ACTION.COMMIT, true, true);
    try {
      solrConnection.update(solrRequest);
      return true;
    } catch (Exception e) {
      log_.error("Cannot clear solr index", e);
      return false;
    }
  }

  /**
   * Posts the media package to solr. Depending on what is referenced in the media package, the method might create one
   * or two entries: one for the episode and one for the series that the episode belongs to.
   * 
   * @param workflow
   *          the workflow instance
   * @throws SolrServerException
   *           if an errors occurs while talking to solr
   */
  public boolean add(WorkflowInstance workflow) throws SolrServerException {
    UpdateRequest solrRequest = new UpdateRequest();
    solrRequest.setAction(ACTION.COMMIT, true, true);
    
    SolrUpdateableInputDocument episodeDocument = createInputDocument(workflow);
    
    // If neither an episode nor a series was contained, there is no point in trying to update
    if (episodeDocument == null)
      return false;

    // Add the episode metadata
    if (episodeDocument != null) {
      solrRequest.add(episodeDocument);
    }

    // Post everything to the workflow database
    try {
      solrConnection.update(solrRequest);
      return true;
    } catch (Exception e) {
      log_.error("Cannot clear solr index");
      return false;
    }
  }

  /**
   * Creates a solr input document for the episode metadata of the media package.
   * 
   * @param workflow
   *          the workflow instance
   * @return an input document ready to be posted to solr
   */
  private SolrUpdateableInputDocument createInputDocument(WorkflowInstance workflow) {
    SolrUpdateableInputDocument solrEpisodeDocument = new SolrUpdateableInputDocument();

    // Use the latest version of the media package available
    List<WorkflowOperationInstance> operations = workflow.getWorkflowOperationInstanceList().getOperationInstance();
    MediaPackage mediaPackage;
    if(operations.size() == 0) {
      mediaPackage = workflow.getSourceMediaPackage();
    } else {
      WorkflowOperationResult result = operations.get(operations.size()-1).getResult();
      if(result == null || result.getResultingMediaPackage() == null) {
        mediaPackage = workflow.getSourceMediaPackage();
      } else {
        mediaPackage = result.getResultingMediaPackage();
      }
    }

    if(mediaPackage != null) {
      Catalog dcCatalogs[] = mediaPackage.getCatalogs(DublinCoreCatalog.FLAVOR,
              MediaPackageReferenceImpl.ANY_MEDIAPACKAGE);
      solrEpisodeDocument.setField(SolrFields.OC_MEDIA_PACKAGE_ID, mediaPackage.getIdentifier().toString());
      DublinCoreCatalog dublinCore = (DublinCoreCatalog) dcCatalogs[0];
      addStandardDublincCoreFields(solrEpisodeDocument, dublinCore);
    }

    // Set common fields
    solrEpisodeDocument.setField(SolrFields.WORKFLOW_ID, workflow.getId());
    solrEpisodeDocument.setField(SolrFields.OC_STATE, workflow.getState().name());

    // TODO: Store properties SolrFields.OC_PROPERTIES
    
    // TODO: Store list of operations SolrFields.OC_OPERATIONS
    
    // TODO: Store current operation SolrFields.OC_CURRENT_OPERATION

    // Add the entire workflow instance
    try {
      String xml = WorkflowBuilder.getInstance().toXml(workflow);
      solrEpisodeDocument.setField(SolrFields.OC_WORKFLOW_INSTANCE, xml);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }    
    return solrEpisodeDocument;
  }

  /**
   * Adds the standard dublin core fields to the solr document.
   * 
   * @param solrInput
   *          the input document
   * @param dc
   *          the dublin core catalog
   */
  private void addStandardDublincCoreFields(SolrUpdateableInputDocument solrInput, DublinCoreCatalog dc) {
    if (!dc.hasValue(DublinCore.PROPERTY_TITLE))
      throw new IllegalStateException("Found dublin core catalog withouth title");

    solrInput.addField(SolrFields.DC_TITLE, dc.getFirst(DublinCore.PROPERTY_TITLE));

    // dc:identifier
    if (dc.hasValue(DublinCore.PROPERTY_IDENTIFIER)) {
      solrInput.addField(SolrFields.DC_IDENTIFIER, dc.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    }

    // dc:subject
    if (dc.hasValue(DublinCore.PROPERTY_SUBJECT)) {
      solrInput.addField(SolrFields.DC_SUBJECT, dc.getFirst(DublinCore.PROPERTY_SUBJECT));
    }

    // dc:creator
    if (dc.hasValue(DublinCore.PROPERTY_CREATOR)) {
      solrInput.addField(SolrFields.DC_CREATOR, dc.getFirst(DublinCore.PROPERTY_CREATOR));
    }

    // dc:publisher
    if (dc.hasValue(DublinCore.PROPERTY_PUBLISHER)) {
      solrInput.addField(SolrFields.DC_PUBLISHER, dc.getFirst(DublinCore.PROPERTY_PUBLISHER));
    }

    // dc:contributor
    if (dc.hasValue(DublinCore.PROPERTY_CONTRIBUTOR)) {
      solrInput.addField(SolrFields.DC_CONTRIBUTOR, dc.getFirst(DublinCore.PROPERTY_CONTRIBUTOR));
    }

    // dc:abstract
    if (dc.hasValue(DublinCore.PROPERTY_ABSTRACT)) {
      solrInput.addField(SolrFields.DC_ABSTRACT, dc.getFirst(DublinCore.PROPERTY_ABSTRACT));
    }

    // dc:created
    if (dc.hasValue(DublinCore.PROPERTY_CREATED)) {
      Date date = EncodingSchemeUtils.decodeMandatoryDate(dc.get(DublinCore.PROPERTY_CREATED).get(0));
      solrInput.addField(SolrFields.DC_CREATED, date);
    }

    // dc:language
    if (dc.hasValue(DublinCore.PROPERTY_LANGUAGE)) {
      solrInput.addField(SolrFields.DC_LANGUAGE, dc.getFirst(DublinCore.PROPERTY_LANGUAGE));
    }

    // dc:rightsholder
    if (dc.hasValue(DublinCore.PROPERTY_RIGHTS_HOLDER)) {
      solrInput.addField(SolrFields.DC_RIGHTS_HOLDER, dc.getFirst(DublinCore.PROPERTY_RIGHTS_HOLDER));
    }

    // dc:spatial
    if (dc.hasValue(DublinCore.PROPERTY_SPATIAL)) {
      solrInput.addField(SolrFields.DC_SPATIAL, dc.getFirst(DublinCore.PROPERTY_SPATIAL));
    }

    // dc:temporal
    if (dc.hasValue(DublinCore.PROPERTY_TEMPORAL)) {
      solrInput.addField(SolrFields.DC_TEMPORAL, dc.getFirst(DublinCore.PROPERTY_TEMPORAL));
    }

    // dc:replaces
    if (dc.hasValue(DublinCore.PROPERTY_REPLACES)) {
      solrInput.addField(SolrFields.DC_REPLACES, dc.getFirst(DublinCore.PROPERTY_REPLACES));
    }

    // dc:type
    if (dc.hasValue(DublinCore.PROPERTY_TYPE)) {
      solrInput.addField(SolrFields.DC_TYPE, dc.getFirst(DublinCore.PROPERTY_TYPE));
    }

    // dc: accessrights
    if (dc.hasValue(DublinCore.PROPERTY_ACCESS_RIGHTS)) {
      solrInput.addField(SolrFields.DC_ACCESS_RIGHTS, dc.getFirst(DublinCore.PROPERTY_ACCESS_RIGHTS));
    }

    // dc:license
    if (dc.hasValue(DublinCore.PROPERTY_LICENSE)) {
      solrInput.addField(SolrFields.DC_LICENSE, dc.getFirst(DublinCore.PROPERTY_LICENSE));
    }

    // dc:available
    if (dc.hasValue(DublinCore.PROPERTY_AVAILABLE)) {
      Object temporal = EncodingSchemeUtils.decodeTemporal(dc.get(DublinCore.PROPERTY_AVAILABLE).get(0));
      if (temporal instanceof Date) {
        solrInput.addField(SolrFields.DC_AVAILABLE_FROM, temporal);
      }
      if (temporal instanceof DCMIPeriod) {
        DCMIPeriod period = ((DCMIPeriod) temporal);
        if (period.hasStart()) {
          solrInput.addField(SolrFields.DC_AVAILABLE_FROM, period.getStart());
        }
        if (period.hasEnd()) {
          solrInput.addField(SolrFields.DC_AVAILABLE_TO, period.getEnd());
        }
      }
    }
  }

}