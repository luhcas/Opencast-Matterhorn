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
package org.opencastproject.series.impl.solr;

import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.InstantTemporal;
import org.opencastproject.metadata.dublincore.PeriodTemporal;
import org.opencastproject.metadata.dublincore.Temporal;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesResult;
import org.opencastproject.series.api.SeriesResultImpl;
import org.opencastproject.series.api.SeriesResultItem;
import org.opencastproject.series.api.SeriesResultItemImpl;
import org.opencastproject.series.impl.SeriesServiceDatabaseException;
import org.opencastproject.series.impl.SeriesServiceIndex;
import org.opencastproject.solr.SolrServerFactory;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.SolrUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Implements {@link SeriesServiceIndex}.
 */
public class SeriesServiceSolrIndex implements SeriesServiceIndex {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SeriesServiceSolrIndex.class);

  /** Configuration key for a remote solr server */
  public static final String CONFIG_SOLR_URL = "org.opencastproject.series.solr.url";

  /** Configuration key for an embedded solr configuration and data directory */
  public static final String CONFIG_SOLR_ROOT = "org.opencastproject.series.solr.dir";

  /** Delimeter used for concatenating multivalued fields for sorting fields in solr */
  public static final String SOLR_MULTIVALUED_DELIMETER = "; ";

  /** Date format supported by solr */
  public static final String SOLR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z";

  /** Connection to the solr server. Solr is used to search for workflows. The workflow data are stored as xml files. */
  private SolrServer solrServer = null;

  /** The root directory to use for solr config and data files */
  protected String solrRoot = null;

  /** The URL to connect to a remote solr server */
  protected URL solrServerUrl = null;

  /** Dublin core service */
  protected DublinCoreCatalogService dcService;

  /**
   * No-argument constructor for OSGi declarative services.
   */
  public SeriesServiceSolrIndex() {
  }

  /**
   * No-argument constructor for OSGi declarative services.
   */
  public SeriesServiceSolrIndex(String storageDirectory) {
    solrRoot = storageDirectory;
  }

  /**
   * OSGi callback for setting Dublin core service.
   * 
   * @param dcService
   *          {@link DublinCoreCatalogService} object
   */
  public void setDublinCoreService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  /**
   * Callback from the OSGi environment on component registration. Retrieves location of the solr index.
   * 
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    if (cc == null) {
      if (solrRoot == null)
        throw new IllegalStateException("Storage dir must be set");
    } else {
      String solrServerUrlConfig = StringUtils.trimToNull(cc.getBundleContext().getProperty(CONFIG_SOLR_URL));
      if (solrServerUrlConfig != null) {
        try {
          solrServerUrl = new URL(solrServerUrlConfig);
        } catch (MalformedURLException e) {
          throw new IllegalStateException("Unable to connect to solr at " + solrServerUrlConfig, e);
        }
      } else if (cc.getBundleContext().getProperty(CONFIG_SOLR_ROOT) != null) {
        solrRoot = cc.getBundleContext().getProperty(CONFIG_SOLR_ROOT);
      } else {
        String storageDir = cc.getBundleContext().getProperty("org.opencastproject.storage.dir");
        if (storageDir == null)
          throw new IllegalStateException("Storage dir must be set (org.opencastproject.storage.dir)");
        solrRoot = PathSupport.concat(storageDir, "series");
      }
    }
    activate();
  }

  /**
   * OSGi callback for deactivation.
   * 
   * @param cc
   *          the component context
   */
  public void deactivate(ComponentContext cc) {
    deactivate();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.impl.WorkflowServiceIndex#activate()
   */
  @Override
  public void activate() {
    // Set up the solr server
    if (solrServerUrl != null) {
      solrServer = SolrServerFactory.newRemoteInstance(solrServerUrl);
    } else {
      try {
        setupSolr(new File(solrRoot));
      } catch (IOException e) {
        throw new IllegalStateException("Unable to connect to solr at " + solrRoot, e);
      } catch (SolrServerException e) {
        throw new IllegalStateException("Unable to connect to solr at " + solrRoot, e);
      }
    }
  }

  /**
   * Prepares the embedded solr environment.
   * 
   * @param solrRoot
   *          the solr root directory
   */
  public void setupSolr(File solrRoot) throws IOException, SolrServerException {
    logger.debug("Setting up solr search index at {}", solrRoot);
    File solrConfigDir = new File(solrRoot, "conf");

    // Create the config directory
    if (solrConfigDir.exists()) {
      logger.debug("solr search index found at {}", solrConfigDir);
    } else {
      logger.debug("solr config directory doesn't exist.  Creating {}", solrConfigDir);
      FileUtils.forceMkdir(solrConfigDir);
    }

    // Make sure there is a configuration in place
    copyClasspathResourceToFile("/solr/conf/protwords.txt", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/schema.xml", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/scripts.conf", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/solrconfig.xml", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/stopwords.txt", solrConfigDir);
    copyClasspathResourceToFile("/solr/conf/synonyms.txt", solrConfigDir);

    // Test for the existence of a data directory
    File solrDataDir = new File(solrRoot, "data");
    if (!solrDataDir.exists()) {
      FileUtils.forceMkdir(solrDataDir);
    }

    // Test for the existence of the index. Note that an empty index directory will prevent solr from
    // completing normal setup.
    File solrIndexDir = new File(solrDataDir, "index");
    if (solrIndexDir.exists() && solrIndexDir.list().length == 0) {
      FileUtils.deleteDirectory(solrIndexDir);
    }

    solrServer = SolrServerFactory.newEmbeddedInstance(solrRoot, solrDataDir);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.SeriesServiceIndex.WorkflowServiceIndex#deactivate()
   */
  @Override
  public void deactivate() {
    SolrServerFactory.shutdown(solrServer);
  }

  // TODO: generalize this method
  private void copyClasspathResourceToFile(String classpath, File dir) {
    InputStream in = null;
    FileOutputStream fos = null;
    try {
      in = SeriesServiceSolrIndex.class.getResourceAsStream(classpath);
      File file = new File(dir, FilenameUtils.getName(classpath));
      logger.debug("copying " + classpath + " to " + file);
      fos = new FileOutputStream(file);
      IOUtils.copy(in, fos);
    } catch (IOException e) {
      throw new RuntimeException("Error copying solr classpath resource to the filesystem", e);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(fos);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.opencastproject.series.impl.SeriesServiceIndex#index(org.opencastproject.metadata.dublincore.DublinCoreCatalog)
   */
  @Override
  public void index(DublinCoreCatalog dc) throws SeriesServiceDatabaseException {
    try {
      SolrInputDocument doc = createDocument(dc);
      synchronized (solrServer) {
        solrServer.add(doc);
        solrServer.commit();
      }
    } catch (Exception e) {
      throw new SeriesServiceDatabaseException("Unable to index series", e);
    }
  }

  /**
   * Creates solr document for inserting into solr index.
   * 
   * @param dc
   *          {@link DublinCoreCatalog} to be stored in index
   * @return {@link SolrInputDocument} created out of Dublin core
   */
  protected SolrInputDocument createDocument(DublinCoreCatalog dc) {
    SolrInputDocument doc = new SolrInputDocument();

    doc.addField(SolrFields.ID_KEY, dc.getFirst(DublinCore.PROPERTY_IDENTIFIER));
    try {
      doc.addField(SolrFields.XML_KEY, serializeDublinCore(dc));
    } catch (IOException e1) {
      throw new IllegalArgumentException(e1);
    }

    // single valued fields
    if (dc.hasValue(DublinCore.PROPERTY_TITLE)) {
      doc.addField(SolrFields.TITLE_KEY, dc.getFirst(DublinCore.PROPERTY_TITLE));
    }
    if (dc.hasValue(DublinCore.PROPERTY_CREATED)) {
      Temporal<?> temporal = EncodingSchemeUtils.decodeTemporal(dc.get(DublinCore.PROPERTY_CREATED).get(0));
      if (temporal instanceof InstantTemporal) {
        doc.addField(SolrFields.CREATED_KEY, ((InstantTemporal) temporal).getTemporal());
      } else if (temporal instanceof PeriodTemporal) {
        doc.addField(SolrFields.CREATED_KEY, ((PeriodTemporal) temporal).getTemporal().getStart());
      } else {
        throw new IllegalArgumentException("Dublin core dc:created is neither a date nor a period");
      }
    }
    if (dc.hasValue(DublinCore.PROPERTY_AVAILABLE)) {
      Temporal<?> temporal = EncodingSchemeUtils.decodeTemporal(dc.get(DublinCore.PROPERTY_AVAILABLE).get(0));
      if (temporal instanceof InstantTemporal) {
        doc.addField(SolrFields.AVAILABLE_FROM_KEY, ((InstantTemporal) temporal).getTemporal());
      } else if (temporal instanceof PeriodTemporal) {
        DCMIPeriod period = ((PeriodTemporal) temporal).getTemporal();
        if (period.hasStart()) {
          doc.addField(SolrFields.AVAILABLE_FROM_KEY, period.getStart());
        }
        if (period.hasEnd()) {
          doc.addField(SolrFields.AVAILABLE_TO_KEY, period.getEnd());
        }
      } else {
        throw new IllegalArgumentException("Dublin core field dc:available is neither a date nor a period");
      }
    }

    // multivalued fields
    addMultiValuedFieldToSolrDocument(doc, SolrFields.SUBJECT_KEY, dc.get(DublinCore.PROPERTY_SUBJECT));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.CREATOR_KEY, dc.get(DublinCore.PROPERTY_CREATOR));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.PUBLISHER_KEY, dc.get(DublinCore.PROPERTY_PUBLISHER));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.CONTRIBUTOR_KEY, dc.get(DublinCore.PROPERTY_CONTRIBUTOR));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.ABSTRACT_KEY, dc.get(DublinCore.PROPERTY_ABSTRACT));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.DESCRIPTION_KEY, dc.get(DublinCore.PROPERTY_DESCRIPTION));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.LANGUAGE_KEY, dc.get(DublinCore.PROPERTY_LANGUAGE));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.RIGHTS_HOLDER_KEY, dc.get(DublinCore.PROPERTY_RIGHTS_HOLDER));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.SPATIAL_KEY, dc.get(DublinCore.PROPERTY_SPATIAL));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.TEMPORAL_KEY, dc.get(DublinCore.PROPERTY_TEMPORAL));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.IS_PART_OF_KEY, dc.get(DublinCore.PROPERTY_IS_PART_OF));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.REPLACES_KEY, dc.get(DublinCore.PROPERTY_REPLACES));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.TYPE_KEY, dc.get(DublinCore.PROPERTY_TYPE));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.ACCESS_RIGHTS_KEY, dc.get(DublinCore.PROPERTY_ACCESS_RIGHTS));
    addMultiValuedFieldToSolrDocument(doc, SolrFields.LICENSE_KEY, dc.get(DublinCore.PROPERTY_LICENSE));

    return doc;
  }

  /**
   * Add field to solr document that can contain multiple values. For sorting field, those values are concatenated and
   * multivalued field delimiter is used.
   * 
   * @param doc
   *          {@link SolrInputDocument} for fields to be added to
   * @param solrField
   *          name of the solr field to add value. For sorting field "_sort" is appended
   * @param dcValues
   *          List of Dublin core values to be added to solr document
   */
  private void addMultiValuedFieldToSolrDocument(SolrInputDocument doc, String solrField, List<DublinCoreValue> dcValues) {
    if (!dcValues.isEmpty()) {
      List<String> values = new LinkedList<String>();
      StringBuilder builder = new StringBuilder();
      values.add(dcValues.get(0).getValue());
      builder.append(dcValues.get(0).getValue());
      for (int i = 1; i < dcValues.size(); i++) {
        values.add(dcValues.get(i).getValue());
        builder.append(SOLR_MULTIVALUED_DELIMETER);
        builder.append(dcValues.get(i).getValue());
      }
      doc.addField(solrField, values);
      doc.addField(solrField + "_sort", builder.toString());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.impl.SeriesServiceIndex#count()
   */
  @Override
  public long count() throws SeriesServiceDatabaseException {
    try {
      QueryResponse response = solrServer.query(new SolrQuery("*:*"));
      return response.getResults().getNumFound();
    } catch (SolrServerException e) {
      throw new SeriesServiceDatabaseException(e);
    }
  }

  /**
   * Appends query parameters to a solr query
   * 
   * @param sb
   *          The {@link StringBuilder} containing the query
   * @param key
   *          the key for this search parameter
   * @param value
   *          the value for this search parameter
   * @return the appended {@link StringBuilder}
   */
  private StringBuilder append(StringBuilder sb, String key, String value) {
    if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
      return sb;
    }
    if (sb.length() > 0) {
      sb.append(" AND ");
    }
    sb.append(key);
    sb.append(":");
    sb.append(ClientUtils.escapeQueryChars(value));
    return sb;
  }

  /**
   * Appends query parameters to a solr query in a way that they are found even though they are not treated as a full
   * word in solr.
   * 
   * @param sb
   *          The {@link StringBuilder} containing the query
   * @param key
   *          the key for this search parameter
   * @param value
   *          the value for this search parameter
   * @return the appended {@link StringBuilder}
   */
  private StringBuilder appendFuzzy(StringBuilder sb, String key, String value) {
    if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
      return sb;
    }
    if (sb.length() > 0) {
      sb.append(" AND ");
    }
    sb.append("(");
    sb.append(key).append(":").append(ClientUtils.escapeQueryChars(value));
    sb.append(" OR ");
    sb.append(key).append(":*").append(ClientUtils.escapeQueryChars(value)).append("*");
    sb.append(")");
    return sb;
  }

  /**
   * Appends query parameters to a solr query
   * 
   * @param sb
   *          The {@link StringBuilder} containing the query
   * @param key
   *          the key for this search parameter
   * @param value
   *          the value for this search parameter
   * @return the appended {@link StringBuilder}
   */
  private StringBuilder append(StringBuilder sb, String key, Date startDate, Date endDate) {
    if (StringUtils.isBlank(key) || (startDate == null && endDate == null)) {
      return sb;
    }
    if (sb.length() > 0) {
      sb.append(" AND ");
    }
    if (startDate == null)
      startDate = new Date(0);
    if (endDate == null)
      endDate = new Date(Long.MAX_VALUE);
    sb.append(key);
    sb.append(":");
    sb.append(SolrUtils.serializeDateRange(startDate, endDate));
    return sb;
  }

  /**
   * Builds a solr search query from a {@link SeriesQuery}.
   * 
   * @param query
   *          the series query
   * @return the solr query string
   */
  protected String buildSolrQueryString(SeriesQuery query) {
    StringBuilder sb = new StringBuilder();
    append(sb, SolrFields.ID_KEY, query.getSeriesId());
    appendFuzzy(sb, SolrFields.TITLE_KEY, query.getSeriesTitle());
    appendFuzzy(sb, SolrFields.FULLTEXT_KEY, query.getText());
    appendFuzzy(sb, SolrFields.CREATOR_KEY, query.getCreator());
    appendFuzzy(sb, SolrFields.CONTRIBUTOR_KEY, query.getContributor());
    append(sb, SolrFields.LANGUAGE_KEY, query.getLanguage());
    append(sb, SolrFields.LICENSE_KEY, query.getLicense());
    appendFuzzy(sb, SolrFields.SUBJECT_KEY, query.getSubject());
    appendFuzzy(sb, SolrFields.ABSTRACT_KEY, query.getAbstract());
    appendFuzzy(sb, SolrFields.DESCRIPTION_KEY, query.getDescription());
    appendFuzzy(sb, SolrFields.PUBLISHER_KEY, query.getPublisher());
    appendFuzzy(sb, SolrFields.RIGHTS_HOLDER_KEY, query.getRightsHolder());
    appendFuzzy(sb, SolrFields.SUBJECT_KEY, query.getSubject());
    append(sb, SolrFields.CREATED_KEY, query.getCreatedFrom(), query.getCreatedTo());

    // If we're looking for anything, set the query to a wildcard search
    if (sb.length() == 0) {
      sb.append("*:*");
    }

    return sb.toString();
  }

  /**
   * Returns the search index' field name that corresponds to the sort field.
   * 
   * @param sort
   *          the sort field
   * @return the field name in the search index
   */
  protected String getSortField(SeriesQuery.Sort sort) {
    switch (sort) {
      case ABSTRACT:
        return SolrFields.ABSTRACT_KEY;
      case ACCESS:
        return SolrFields.ACCESS_RIGHTS_KEY;
      case AVAILABLE_FROM:
        return SolrFields.AVAILABLE_FROM_KEY;
      case AVAILABLE_TO:
        return SolrFields.AVAILABLE_TO_KEY;
      case CONTRIBUTOR:
        return SolrFields.CONTRIBUTOR_KEY;
      case CREATED:
        return SolrFields.CREATED_KEY;
      case CREATOR:
        return SolrFields.CREATOR_KEY;
      case DESCRIPTION:
        return SolrFields.DESCRIPTION_KEY;
      case IS_PART_OF:
        return SolrFields.IS_PART_OF_KEY;
      case LANGUAGE:
        return SolrFields.LANGUAGE_KEY;
      case LICENCE:
        return SolrFields.LICENSE_KEY;
      case PUBLISHER:
        return SolrFields.PUBLISHER_KEY;
      case REPLACES:
        return SolrFields.REPLACES_KEY;
      case RIGHTS_HOLDER:
        return SolrFields.RIGHTS_HOLDER_KEY;
      case SPATIAL:
        return SolrFields.SPATIAL_KEY;
      case SUBJECT:
        return SolrFields.SUBJECT_KEY;
      case TEMPORAL:
        return SolrFields.TEMPORAL_KEY;
      case TITLE:
        return SolrFields.TITLE_KEY;
      case TYPE:
        return SolrFields.TYPE_KEY;
      default:
        throw new IllegalArgumentException("No mapping found between sort field and index");
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.SeriesServiceIndex.WorkflowServiceIndex#getWorkflowInstances(org.opencastproject.workflow.api.WorkflowQuery)
   */
  @Override
  public SeriesResult search(SeriesQuery query) throws SeriesServiceDatabaseException {
    int count = query.getCount() > 0 ? (int) query.getCount() : 20; // default to 20 items if not specified
    int startPage = query.getStartPage() > 0 ? (int) query.getStartPage() : 0; // default to page zero

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setRows(count);
    solrQuery.setStart(startPage * count);

    String solrQueryString = buildSolrQueryString(query);
    solrQuery.setQuery(solrQueryString);

    if (query.getSort() != null) {
      SolrQuery.ORDER order = query.isSortAscending() ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc;
      solrQuery.addSortField(getSortField(query.getSort()) + "_sort", order);
    }

    if (!SeriesQuery.Sort.CREATED.equals(query.getSort())) {
      solrQuery.addSortField(getSortField(SeriesQuery.Sort.CREATED) + "_sort", SolrQuery.ORDER.desc);
    }

    SeriesResultImpl result = null;

    try {
      long time = System.currentTimeMillis();
      QueryResponse response = solrServer.query(solrQuery);
      SolrDocumentList items = response.getResults();
      time = System.currentTimeMillis() - time;

      result = new SeriesResultImpl();
      result.setSearchTime(time);
      result.setStartPage(startPage);
      result.setPageSize(count);
      result.setNumberOfItems(items.getNumFound());

      // Iterate through the results
      for (SolrDocument doc : items) {
        SeriesResultItem item = createResultItem(doc);
        result.addItem(item);
      }
    } catch (Exception e) {
      throw new SeriesServiceDatabaseException(e);
    }
    return result;
  }

  /**
   * Creates series result item out of solr document.
   * 
   * @param doc
   *          {@link SolrDocument} to be parsed
   * @return created {@link SeriesResultItem}
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected SeriesResultItem createResultItem(SolrDocument doc) {
    SeriesResultItemImpl item = new SeriesResultItemImpl();
    item.setAbstract((Collection) doc.getFieldValues(SolrFields.ABSTRACT_KEY));
    item.setAccessRights((Collection) doc.getFieldValues(SolrFields.ACCESS_RIGHTS_KEY));
    item.setAvailableFrom((Date) doc.get(SolrFields.AVAILABLE_FROM_KEY));
    item.setAvailableTo((Date) doc.get(SolrFields.AVAILABLE_TO_KEY));
    item.setContributor((List) doc.getFieldValues(SolrFields.CONTRIBUTOR_KEY));
    item.setCreated((Date) doc.get(SolrFields.CREATED_KEY));
    item.setCreator((Collection) doc.getFieldValues(SolrFields.CREATOR_KEY));
    item.setDescription((Collection) doc.getFieldValues(SolrFields.DESCRIPTION_KEY));
    item.setId((String) doc.get(SolrFields.ID_KEY));
    item.setIsPartOf((Collection) doc.getFieldValues(SolrFields.IS_PART_OF_KEY));
    item.setLanguage((Collection) doc.getFieldValues(SolrFields.LANGUAGE_KEY));
    item.setLicense((Collection) doc.getFieldValues(SolrFields.LICENSE_KEY));
    item.setPublisher((Collection) doc.getFieldValues(SolrFields.PUBLISHER_KEY));
    item.setReplaces((Collection) doc.getFieldValues(SolrFields.REPLACES_KEY));
    item.setRightsHolder((Collection) doc.getFieldValues(SolrFields.RIGHTS_HOLDER_KEY));
    item.setSpatial((Collection) doc.getFieldValues(SolrFields.SPATIAL_KEY));
    item.setSubject((Collection) doc.getFieldValues(SolrFields.SUBJECT_KEY));
    item.setTemporal((Collection) doc.getFieldValues(SolrFields.TEMPORAL_KEY));
    item.setTitle((String) doc.get(SolrFields.TITLE_KEY));
    item.setType((Collection) doc.getFieldValues(SolrFields.TYPE_KEY));
    return item;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.SeriesServiceIndex.WorkflowServiceIndex#remove(long)
   */
  @Override
  public void delete(String id) throws SeriesServiceDatabaseException {
    try {
      synchronized (solrServer) {
        solrServer.deleteById(id);
        solrServer.commit();
      }
    } catch (Exception e) {
      throw new SeriesServiceDatabaseException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.series.impl.SeriesServiceIndex#get(java.lang.String)
   */
  @Override
  public DublinCoreCatalog get(String seriesId) throws SeriesServiceDatabaseException, NotFoundException {
    String solrQueryString = SolrFields.ID_KEY + ":" + ClientUtils.escapeQueryChars(seriesId);
    SolrQuery q = new SolrQuery(solrQueryString);
    QueryResponse response;
    try {
      response = solrServer.query(q);
    } catch (SolrServerException e) {
      logger.error("Could not perform series retrieval: {}", e);
      throw new SeriesServiceDatabaseException(e);
    }
    if (response.getResults().isEmpty()) {
      logger.info("No series exists with ID {}", seriesId);
      throw new NotFoundException("Series with ID " + seriesId + " does not exist");
    } else {
      String dcXML = (String) response.getResults().get(0).get(SolrFields.XML_KEY);
      DublinCoreCatalog dc;
      try {
        dc = parseDublinCore(dcXML);
      } catch (IOException e) {
        logger.error("Could not parse Dublin core: {}", e);
        throw new SeriesServiceDatabaseException(e);
      }
      return dc;
    }
  }

  /**
   * Clears the index of all series instances.
   */
  public void clear() throws SeriesException {
    try {
      synchronized (solrServer) {
        solrServer.deleteByQuery("*:*");
        solrServer.commit();
      }
    } catch (Exception e) {
      throw new SeriesException(e);
    }
  }

  /**
   * Serializes Dublin core and returns serialized string.
   * 
   * @param dc
   *          {@link DublinCoreCatalog} to be serialized
   * @return String representation of serialized Dublin core
   * @throws IOException
   *           if serialization fails
   */
  private String serializeDublinCore(DublinCoreCatalog dc) throws IOException {
    InputStream in = dcService.serialize(dc);

    StringWriter writer = new StringWriter();
    IOUtils.copy(in, writer, "UTF-8");

    return writer.toString();
  }

  /**
   * Parses Dublin core stored as string.
   * 
   * @param dcXML
   *          string representation of Dublin core
   * @return parsed {@link DublinCoreCatalog}
   * @throws IOException
   *           if parsing fails
   */
  private DublinCoreCatalog parseDublinCore(String dcXML) throws IOException {
    DublinCoreCatalog dc = dcService.load(new ByteArrayInputStream(dcXML.getBytes("UTF-8")));
    return dc;
  }
}