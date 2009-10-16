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

import org.opencastproject.conductor.api.ConductorService;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionFactory;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Loads, unloads, and reloads {@link WorkflowDefinition}s from "*workflow.xml" files in any of fileinstall's watch
 * directories.
 */
public class WorkflowDefinitionScanner implements ArtifactInstaller {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionScanner.class);
  
  protected ConductorService conductorService;
  public void setConductorService(ConductorService conductorService) {
    this.conductorService = conductorService;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
  public void install(File artifact) throws Exception {
    logger.info("Installing workflow from file " + artifact.getAbsolutePath());
    InputStream stream = null;
    try {
      stream = new FileInputStream(artifact);
      WorkflowDefinition def = WorkflowDefinitionFactory.getInstance().parse(stream);
      conductorService.addWorkflowDefinition(def);
    } finally {
      stream.close();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  public void uninstall(File artifact) throws Exception {
    logger.info("Uninstalling workflow from file " + artifact.getAbsolutePath());
    InputStream stream = null;
    try {
      stream = new FileInputStream(artifact);
      WorkflowDefinition def = WorkflowDefinitionFactory.getInstance().parse(stream);
      conductorService.removeWorkflowDefinition(def.getTitle());
    } finally {
      stream.close();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.apache.felix.fileinstall.ArtifactInstaller#update(java.io.File)
   */
  public void update(File artifact) throws Exception {
    uninstall(artifact);
    install(artifact);
  }

  /**
   * {@inheritDoc}
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  public boolean canHandle(File artifact) {
    boolean canHandle =  artifact.getName().endsWith("workflow.xml");
    if(canHandle) {
      logger.info(WorkflowDefinitionScanner.class.getName() + " can handle file " + artifact.getAbsolutePath());
    }
    return canHandle;
  }
}
