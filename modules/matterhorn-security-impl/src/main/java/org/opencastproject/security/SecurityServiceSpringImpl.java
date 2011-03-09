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
package org.opencastproject.security;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.jboss.security.xacml.core.JBossPDP;
import org.jboss.security.xacml.core.model.context.AttributeType;
import org.jboss.security.xacml.core.model.context.RequestType;
import org.jboss.security.xacml.core.model.context.SubjectType;
import org.jboss.security.xacml.core.model.policy.ActionMatchType;
import org.jboss.security.xacml.core.model.policy.ActionType;
import org.jboss.security.xacml.core.model.policy.ActionsType;
import org.jboss.security.xacml.core.model.policy.ApplyType;
import org.jboss.security.xacml.core.model.policy.AttributeDesignatorType;
import org.jboss.security.xacml.core.model.policy.AttributeValueType;
import org.jboss.security.xacml.core.model.policy.ConditionType;
import org.jboss.security.xacml.core.model.policy.EffectType;
import org.jboss.security.xacml.core.model.policy.ObjectFactory;
import org.jboss.security.xacml.core.model.policy.PolicyType;
import org.jboss.security.xacml.core.model.policy.ResourceMatchType;
import org.jboss.security.xacml.core.model.policy.ResourceType;
import org.jboss.security.xacml.core.model.policy.ResourcesType;
import org.jboss.security.xacml.core.model.policy.RuleType;
import org.jboss.security.xacml.core.model.policy.SubjectAttributeDesignatorType;
import org.jboss.security.xacml.core.model.policy.TargetType;
import org.jboss.security.xacml.factories.RequestAttributeFactory;
import org.jboss.security.xacml.factories.RequestResponseContextFactory;
import org.jboss.security.xacml.interfaces.PolicyDecisionPoint;
import org.jboss.security.xacml.interfaces.RequestContext;
import org.jboss.security.xacml.interfaces.XACMLConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

/**
 * A Spring Security implementation of {@link SecurityService}.
 */
public class SecurityServiceSpringImpl implements SecurityService {

  /** XACML rule for combining policies */
  public static final String RULE_COMBINING_ALG = "urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:permit-overrides";

  /** XACML urn for actions */
  public static final String ACTION_IDENTIFIER = "urn:oasis:names:tc:xacml:1.0:action:action-id";

  /** XACML urn for resources */
  public static final String RESOURCE_IDENTIFIER = "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

  /** XACML urn for subject */
  public static final String SUBJECT_IDENTIFIER = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";

  /** XACML urn for roles */
  public static final String SUBJECT_ROLE_IDENTIFIER = "urn:oasis:names:tc:xacml:2.0:subject:role";

  /** XACML urn for string equality */
  public static final String XACML_STRING_EQUAL = "urn:oasis:names:tc:xacml:1.0:function:string-equal";

  /** XACML urn for string equality */
  public static final String XACML_STRING_IS_IN = "urn:oasis:names:tc:xacml:1.0:function:string-is-in";

  /** W3C String data type */
  public static final String W3C_STRING = "http://www.w3.org/2001/XMLSchema#string";

  /** The policy assertion issuer */
  public static final String ISSUER = "matterhorn";

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SecurityServiceSpringImpl.class);

  /** The JAXB Context to use for marshaling XACML security policy documents */
  protected static JAXBContext jBossXacmlJaxbContext;

  /** The workspace */
  protected Workspace workspace;

  /** Static initializer for the single JAXB context */
  static {
    try {
      jBossXacmlJaxbContext = JAXBContext.newInstance("org.jboss.security.xacml.core.model.policy",
              PolicyType.class.getClassLoader());
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the workspace to use for retrieving XACML policies
   * 
   * @param workspace
   *          the workspace to set
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.SecurityService#getUserId()
   */
  @Override
  public String getUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return null;
    } else {
      Object principal = auth.getPrincipal();
      if (principal == null) {
        return null;
      }
      if (principal instanceof UserDetails) {
        UserDetails userDetails = (UserDetails) principal;
        return userDetails.getUsername();
      } else {
        return principal.toString();
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.SecurityService#getRoles()
   */
  @Override
  public String[] getRoles() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return ANONYMOUS;
    } else {
      Collection<GrantedAuthority> authorities = auth.getAuthorities();
      if (auth == null || authorities.size() == 0)
        return ANONYMOUS;
      List<String> roles = new ArrayList<String>(authorities.size());
      for (GrantedAuthority ga : authorities) {
        roles.add(ga.getAuthority());
      }
      Collections.sort(roles);
      return roles.toArray(new String[roles.size()]);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.SecurityService#getAccessControlList(org.opencastproject.mediapackage.MediaPackage)
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
      policy = ((JAXBElement<PolicyType>) jBossXacmlJaxbContext.createUnmarshaller().unmarshal(in)).getValue();
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
   * @see org.opencastproject.security.api.SecurityService#hasPermission(org.opencastproject.mediapackage.MediaPackage,
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

    // Create a subject type
    SubjectType subject = new SubjectType();
    subject.getAttribute().add(
            RequestAttributeFactory.createStringAttributeType(SUBJECT_IDENTIFIER, ISSUER, getUserId()));
    for (String role : getRoles()) {
      AttributeType attSubjectID = RequestAttributeFactory.createStringAttributeType(SUBJECT_ROLE_IDENTIFIER, ISSUER,
              role);
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
    resourceType.getAttribute()
            .add(RequestAttributeFactory.createAnyURIAttributeType(RESOURCE_IDENTIFIER, ISSUER, uri));

    // Create an action type
    org.jboss.security.xacml.core.model.context.ActionType actionType = new org.jboss.security.xacml.core.model.context.ActionType();
    actionType.getAttribute().add(RequestAttributeFactory.createStringAttributeType(ACTION_IDENTIFIER, ISSUER, action));

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
      xacmlContent = getXacml(mediapackage, roleActions);
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
   * Builds an xml string containing the xacml for the mediapackage.
   * 
   * @param mediapackage
   *          the mediapackage
   * @param accessControlList
   *          the tuples of roles to actions
   * @return
   * @throws JAXBException
   */
  protected String getXacml(MediaPackage mediapackage, List<AccessControlEntry> accessControlList) throws JAXBException {
    ObjectFactory jbossXacmlObjectFactory = new ObjectFactory();
    PolicyType policy = new PolicyType();
    policy.setPolicyId(mediapackage.getIdentifier().toString());
    policy.setVersion("2.0");
    policy.setRuleCombiningAlgId(RULE_COMBINING_ALG);

    // TODO: Add target/resources to rule
    TargetType policyTarget = new TargetType();
    ResourcesType resources = new ResourcesType();
    ResourceType resource = new ResourceType();
    ResourceMatchType resourceMatch = new ResourceMatchType();
    resourceMatch.setMatchId(XACML_STRING_EQUAL);
    AttributeValueType resourceAttributeValue = new AttributeValueType();
    resourceAttributeValue.setDataType(W3C_STRING);
    resourceAttributeValue.getContent().add(mediapackage.getIdentifier().toString());
    AttributeDesignatorType resourceDesignator = new AttributeDesignatorType();
    resourceDesignator.setAttributeId(RESOURCE_IDENTIFIER);
    resourceDesignator.setDataType(W3C_STRING);

    // now go back up the tree
    resourceMatch.setResourceAttributeDesignator(resourceDesignator);
    resourceMatch.setAttributeValue(resourceAttributeValue);
    resource.getResourceMatch().add(resourceMatch);
    resources.getResource().add(resource);
    policyTarget.setResources(resources);
    policy.setTarget(policyTarget);

    // Loop over roleActions and add a rule for each
    for (AccessControlEntry ace : accessControlList) {
      boolean allow = ace.isAllow();

      RuleType rule = new RuleType();
      rule.setRuleId(ace.getRole() + "_" + ace.getAction() + (allow ? "_Permit" : "_Deny"));
      if (allow) {
        rule.setEffect(EffectType.PERMIT);
      } else {
        rule.setEffect(EffectType.DENY);
      }

      TargetType target = new TargetType();
      ActionsType actions = new ActionsType();
      ActionType action = new ActionType();
      ActionMatchType actionMatch = new ActionMatchType();
      actionMatch.setMatchId(XACML_STRING_EQUAL);
      AttributeValueType attributeValue = new AttributeValueType();
      attributeValue.setDataType(W3C_STRING);
      attributeValue.getContent().add(ace.getAction());
      AttributeDesignatorType designator = new AttributeDesignatorType();
      designator.setAttributeId(ACTION_IDENTIFIER);
      designator.setDataType(W3C_STRING);

      // now go back up the tree
      actionMatch.setActionAttributeDesignator(designator);
      actionMatch.setAttributeValue(attributeValue);
      action.getActionMatch().add(actionMatch);
      actions.getAction().add(action);
      target.setActions(actions);
      rule.setTarget(target);

      ConditionType condition = new ConditionType();
      ApplyType apply = new ApplyType();
      apply.setFunctionId(XACML_STRING_IS_IN);

      AttributeValueType conditionAttributeValue = new AttributeValueType();
      conditionAttributeValue.setDataType(W3C_STRING);
      conditionAttributeValue.getContent().add(ace.getRole());

      SubjectAttributeDesignatorType subjectDesignator = new SubjectAttributeDesignatorType();
      subjectDesignator.setDataType(W3C_STRING);
      subjectDesignator.setAttributeId(SUBJECT_ROLE_IDENTIFIER);
      apply.getExpression().add(jbossXacmlObjectFactory.createAttributeValue(conditionAttributeValue));
      apply.getExpression().add(jbossXacmlObjectFactory.createSubjectAttributeDesignator(subjectDesignator));

      condition.setExpression(jbossXacmlObjectFactory.createApply(apply));
      rule.setCondition(condition);
      policy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(rule);
    }

    // Add the global deny rule
    RuleType deny = new RuleType();
    deny.setEffect(EffectType.DENY);
    deny.setRuleId("DenyRule");
    policy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(deny);

    // serialize to xml
    StringWriter writer = new StringWriter();
    jBossXacmlJaxbContext.createMarshaller().marshal(jbossXacmlObjectFactory.createPolicy(policy), writer);
    return writer.getBuffer().toString();
  }
}
