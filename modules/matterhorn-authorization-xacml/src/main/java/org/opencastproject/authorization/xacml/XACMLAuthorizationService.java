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
package org.opencastproject.authorization.xacml;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.jboss.security.xacml.core.JBossPDP;
import org.jboss.security.xacml.core.model.context.AttributeType;
import org.jboss.security.xacml.core.model.context.RequestType;
import org.jboss.security.xacml.core.model.context.SubjectType;
import org.jboss.security.xacml.core.model.policy.ActionType;
import org.jboss.security.xacml.core.model.policy.ApplyType;
import org.jboss.security.xacml.core.model.policy.AttributeValueType;
import org.jboss.security.xacml.core.model.policy.EffectType;
import org.jboss.security.xacml.core.model.policy.PolicyType;
import org.jboss.security.xacml.core.model.policy.RuleType;
import org.jboss.security.xacml.factories.RequestAttributeFactory;
import org.jboss.security.xacml.factories.RequestResponseContextFactory;
import org.jboss.security.xacml.interfaces.PolicyDecisionPoint;
import org.jboss.security.xacml.interfaces.RequestContext;
import org.jboss.security.xacml.interfaces.XACMLConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

/**
 * A XACML implementation of the {@link AuthorizationService}.
 */
public class XACMLAuthorizationService implements AuthorizationService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(XACMLAuthorizationService.class);

  /** The workspace */
  protected Workspace workspace;

  /** The security service */
  protected SecurityService securityService;

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.AuthorizationService#getAccessControlList(org.opencastproject.mediapackage.MediaPackage)
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<AccessControlEntry> getAccessControlList(MediaPackage mediapackage) {
    List<AccessControlEntry> acl = new ArrayList<AccessControlEntry>();
    Attachment[] xacmlAttachments = mediapackage.getAttachments(MediaPackageElements.XACML_POLICY);
    if (xacmlAttachments.length == 0) {
      logger.warn("No XACML attachment found in {}", mediapackage);
      return acl;
    } else if (xacmlAttachments.length > 1) {
      logger.warn("More than one XACML policy is attached to {}", mediapackage);
      return acl;
    }
    File xacmlPolicyFile = null;
    try {
      xacmlPolicyFile = workspace.get(xacmlAttachments[0].getURI());
    } catch (NotFoundException e) {
      logger.warn("XACML policy file not found", e);
    } catch (IOException e) {
      logger.warn("Unable to access XACML policy file {}", xacmlPolicyFile, e);
    }

    FileInputStream in;
    try {
      in = new FileInputStream(xacmlPolicyFile);
    } catch (FileNotFoundException e) {
      throw new IllegalStateException("Unable to find file in the workspace: " + xacmlPolicyFile);
    }

    PolicyType policy = null;
    try {
      policy = ((JAXBElement<PolicyType>) XACMLUtils.jBossXacmlJaxbContext.createUnmarshaller().unmarshal(in))
              .getValue();
    } catch (JAXBException e) {
      throw new IllegalStateException("Unable to unmarshall xacml document" + xacmlPolicyFile);
    }
    for (Object object : policy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition()) {
      if (object instanceof RuleType) {
        RuleType rule = (RuleType) object;
        if (rule.getTarget() == null) {
          continue;
        }
        ActionType action = rule.getTarget().getActions().getAction().get(0);
        String actionForAce = (String) action.getActionMatch().get(0).getAttributeValue().getContent().get(0);
        String role = null;
        JAXBElement<ApplyType> apply = (JAXBElement<ApplyType>) rule.getCondition().getExpression();
        for (JAXBElement<?> element : apply.getValue().getExpression()) {
          if (element.getValue() instanceof AttributeValueType) {
            role = (String) ((AttributeValueType) element.getValue()).getContent().get(0);
            break;
          }
        }
        if (role == null) {
          logger.warn("Unable to find a role in rule {}", rule);
          continue;
        }
        AccessControlEntry ace = new AccessControlEntry(role, actionForAce, rule.getEffect().equals(EffectType.PERMIT));
        acl.add(ace);
      } else {
        logger.debug("Skipping {}", object);
      }
    }
    return acl;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.AuthorizationService#hasPermission(org.opencastproject.mediapackage.MediaPackage,
   *      java.lang.String)
   */
  @Override
  public boolean hasPermission(MediaPackage mediapackage, String action) {
    Attachment[] xacmlAttachments = mediapackage.getAttachments(MediaPackageElements.XACML_POLICY);
    if (xacmlAttachments.length == 0) {
      logger.warn("No XACML attachment found in {}", mediapackage);
      return false;
    } else if (xacmlAttachments.length > 1) {
      logger.warn("More than one XACML policy is attached to {}", mediapackage);
      return false;
    }
    File xacmlPolicyFile = null;
    try {
      xacmlPolicyFile = workspace.get(xacmlAttachments[0].getURI());
    } catch (NotFoundException e) {
      logger.warn("XACML policy file not found", e);
    } catch (IOException e) {
      logger.warn("Unable to access XACML policy file {}", xacmlPolicyFile, e);
    }

    RequestContext requestCtx = RequestResponseContextFactory.createRequestCtx();

    User user = securityService.getUser();

    // Create a subject type
    SubjectType subject = new SubjectType();
    subject.getAttribute().add(
            RequestAttributeFactory.createStringAttributeType(XACMLUtils.SUBJECT_IDENTIFIER, XACMLUtils.ISSUER,
                    user.getUserName()));
    for (String role : user.getRoles()) {
      AttributeType attSubjectID = RequestAttributeFactory.createStringAttributeType(
              XACMLUtils.SUBJECT_ROLE_IDENTIFIER, XACMLUtils.ISSUER, role);
      subject.getAttribute().add(attSubjectID);
    }

    // Create a resource type
    URI uri = null;
    try {
      uri = new URI(mediapackage.getIdentifier().toString());
    } catch (URISyntaxException e) {
      logger.warn("Unable to represent mediapackage identifier '{}' as a URI", mediapackage.getIdentifier().toString());
    }
    org.jboss.security.xacml.core.model.context.ResourceType resourceType = new org.jboss.security.xacml.core.model.context.ResourceType();
    resourceType.getAttribute().add(
            RequestAttributeFactory.createAnyURIAttributeType(XACMLUtils.RESOURCE_IDENTIFIER, XACMLUtils.ISSUER, uri));

    // Create an action type
    org.jboss.security.xacml.core.model.context.ActionType actionType = new org.jboss.security.xacml.core.model.context.ActionType();
    actionType.getAttribute().add(
            RequestAttributeFactory.createStringAttributeType(XACMLUtils.ACTION_IDENTIFIER, XACMLUtils.ISSUER, action));

    // Create a Request Type
    RequestType requestType = new RequestType();
    requestType.getSubject().add(subject);
    requestType.getResource().add(resourceType);
    requestType.setAction(actionType);
    try {
      requestCtx.setRequest(requestType);
    } catch (IOException e) {
      logger.warn("Unable to set the xacml request type", e);
      return false;
    }

    PolicyDecisionPoint pdp = getPolicyDecisionPoint(xacmlPolicyFile);

    return pdp.evaluate(requestCtx).getDecision() == XACMLConstants.DECISION_PERMIT;
  }

  /**
   * @param xacmlAttachment
   * @return
   */
  private PolicyDecisionPoint getPolicyDecisionPoint(File xacmlFile) {
    // Build a JBoss PDP configuration. This is a custom jboss format, so we're just hacking it together here
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
    sb.append("<ns:jbosspdp xmlns:ns=\"urn:jboss:xacml:2.0\">");
    sb.append("<ns:Policies><ns:Policy><ns:Location>");
    sb.append(xacmlFile.toURI().toString());
    sb.append("</ns:Location></ns:Policy></ns:Policies><ns:Locators>");
    sb.append("<ns:Locator Name=\"org.jboss.security.xacml.locators.JBossPolicyLocator\">");
    sb.append("</ns:Locator></ns:Locators></ns:jbosspdp>");
    InputStream is = null;
    try {
      is = IOUtils.toInputStream(sb.toString(), "UTF-8");
      return new JBossPDP(is);
    } catch (IOException e) {
      // Only happens if 'UTF-8' is an invalid encoding, which it isn't
      throw new IllegalStateException("Unable to transform a string into a stream");
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.SecurityService#setAccessControl(org.opencastproject.mediapackage.MediaPackage,
   *      java.util.Set)
   */
  @Override
  public MediaPackage setAccessControl(MediaPackage mediapackage, List<AccessControlEntry> roleActions)
          throws MediaPackageException {
    // Get XACML representation of these role + action tuples
    String xacmlContent = null;
    try {
      xacmlContent = XACMLUtils.getXacml(mediapackage, roleActions);
    } catch (JAXBException e) {
      throw new MediaPackageException("Unable to generate xacml for mediapackage " + mediapackage.getIdentifier());
    }

    // add attachment
    String attachmentId = "xacmlpolicy";
    URI uri = workspace.getURI(mediapackage.getIdentifier().toString(), attachmentId);
    Attachment attachment = (Attachment) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .elementFromURI(uri, Attachment.TYPE, MediaPackageElements.XACML_POLICY);
    attachment.setIdentifier(attachmentId);
    mediapackage.add(attachment);

    try {
      workspace.put(mediapackage.getIdentifier().toString(), attachment.getIdentifier(), "xacml.xml",
              IOUtils.toInputStream(xacmlContent));
    } catch (IOException e) {
      throw new MediaPackageException("Can not store xacml for mediapackage " + mediapackage.getIdentifier());
    }
    attachment.setURI(uri);

    // return augmented mediapackage
    return mediapackage;
  }

  /**
   * Sets the workspace to use for retrieving XACML policies
   * 
   * @param workspace
   *          the workspace to set
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Declarative services callback to set the security service.
   * 
   * @param securityService
   *          the security service
   */
  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
