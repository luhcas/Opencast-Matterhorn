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
package org.opencastproject.integrationtest;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(JUnit4TestRunner.class)
public abstract class AbstractIntegrationTest {
  @Inject
  protected BundleContext bundleContext;

  @Configuration
  public static Option[] configuration() {
    return CoreOptions.options(
            CoreOptions.systemProperty( "org.ops4j.pax.logging.DefaultServiceLog.level" ).value( "DEBUG" ),
            PaxRunnerOptions.repositories("http://repository.opencastproject.org/nexus/content/groups/public"),
            CoreOptions.felix(),
            CoreOptions.provision(
            CoreOptions.wrappedBundle(CoreOptions.mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpcore").update(false)),
            CoreOptions.wrappedBundle(CoreOptions.mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpclient").update(false)),
            CoreOptions.mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-api").update(false),
//          CoreOptions.mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-service").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.commons").artifactId("com.springsource.org.apache.commons.logging").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.felix").artifactId("org.osgi.compendium").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.scr").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.metatype").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.eventadmin").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.fileinstall").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.servicemix.specs").artifactId("org.apache.servicemix.specs.javamail-api-1.4").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.servicemix.specs").artifactId("org.apache.servicemix.specs.jaxb-api-2.1").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.servicemix.specs").artifactId("org.apache.servicemix.specs.jaxws-api-2.1").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.servicemix.specs").artifactId("org.apache.servicemix.specs.saaj-api-1.3").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.servicemix.specs").artifactId("org.apache.servicemix.specs.stax-api-1.0").update(false),
            CoreOptions.mavenBundle().groupId("javax.ws.rs").artifactId("jsr311-api").version("1.1.A").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-jms_1.1_spec").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-jta_1.1_spec").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-annotation_1.0_spec").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-activation_1.1_spec").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-j2ee-connector_1.5_spec").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-j2ee-management_1.1_spec").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-ws-metadata_2.0_spec").update(false),
            CoreOptions.mavenBundle().groupId("joda-time").artifactId("joda-time").version("1.6").update(false),
            CoreOptions.mavenBundle().groupId("org.jdom").artifactId("com.springsource.org.jdom").version("1.0.0").update(false),
            CoreOptions.mavenBundle().groupId("org.ognl").artifactId("com.springsource.org.ognl").version("2.7.3").update(false),
            CoreOptions.mavenBundle().groupId("org.jboss.javassist").artifactId("com.springsource.javassist").version("3.9.0.GA").update(false),
            CoreOptions.mavenBundle().groupId("org.codehaus.jettison").artifactId("jettison").update(false),
            CoreOptions.mavenBundle().groupId("commons-io").artifactId("commons-io").update(false),
            CoreOptions.mavenBundle().groupId("commons-lang").artifactId("commons-lang").update(false),
            CoreOptions.mavenBundle().groupId("commons-codec").artifactId("commons-codec").update(false),
            CoreOptions.mavenBundle().groupId("commons-pool").artifactId("commons-pool").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.commons").artifactId("com.springsource.org.apache.commons.beanutils").version("1.7.0").update(false),
            CoreOptions.mavenBundle().groupId("commons-fileupload").artifactId("commons-fileupload").update(false),
            CoreOptions.mavenBundle().groupId("commons-collections").artifactId("commons-collections").version("3.2.1").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.mina").artifactId("mina-core").version("2.0.0-M6").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.mina").artifactId("mina-integration-jmx").version("2.0.0-M6").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.mina").artifactId("mina-integration-ognl").version("2.0.0-M6").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.mina").artifactId("mina-integration-beans").version("2.0.0-M6").update(false),
            CoreOptions.mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-service").update(false),
            CoreOptions.mavenBundle().groupId("org.springframework.osgi").artifactId("spring-osgi-io").version("1.2.0").update(false),
            CoreOptions.mavenBundle().groupId("org.springframework.osgi").artifactId("spring-osgi-core").version("1.2.0").update(false),
            CoreOptions.mavenBundle().groupId("org.springframework.osgi").artifactId("spring-osgi-extender").version("1.2.0").update(false),
            CoreOptions.mavenBundle().groupId("org.springframework").artifactId("org.springframework.web").version("2.5.6.A").update(false),
            CoreOptions.mavenBundle().groupId("org.springframework").artifactId("org.springframework.aop").version("2.5.6.A").update(false),
            CoreOptions.mavenBundle().groupId("org.springframework").artifactId("org.springframework.core").version("2.5.6.A").update(false),
            CoreOptions.mavenBundle().groupId("org.springframework").artifactId("org.springframework.beans").version("2.5.6.A").update(false),
            CoreOptions.mavenBundle().groupId("org.springframework").artifactId("org.springframework.context").version("2.5.6.A").update(false),
//            CoreOptions.mavenBundle().groupId("org.springframework").artifactId("org.springframework.context.support").version("2.5.6.A").update(false),
            CoreOptions.mavenBundle().groupId("org.aopalliance").artifactId("com.springsource.org.aopalliance").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.jaxb-impl").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.wsdl4j").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.xmlsec").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.wss4j").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.xmlschema").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.asm").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.xmlresolver").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.neethi").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.woodstox").update(false),
//            CoreOptions.mavenBundle().groupId("org.apache.activemq").artifactId("activemq-core").version("5.2.0").update(false),
//            CoreOptions.mavenBundle().groupId("org.apache.activemq").artifactId("activemq-ra").version("5.2.0").update(false),
//            CoreOptions.mavenBundle().groupId("org.apache.activemq").artifactId("activemq-pool").version("5.2.0").update(false),
//            CoreOptions.mavenBundle().groupId("org.apache.activemq").artifactId("activemq-console").version("5.2.0").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.webconsole").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-util").update(false).update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-media").update(false).update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-conductor-api").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-conductor").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-workflow-service-api").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-workflow-service-impl").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-authentication-api").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-composer-service-api").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-composer-service-impl").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-distribution-service-api").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-distribution-service-local-impl").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-engage-service-api").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-engage-service-impl").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-ingest-service-api").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-ingest-service-impl").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-inspection-service-api").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-inspection-service-impl").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-notification-service-api").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-scheduler-api").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-scheduler-impl").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-search-service-api").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-search-service-impl").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-working-file-repository-service-api").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-working-file-repository-service-impl").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-workspace-api").update(false),
            CoreOptions.mavenBundle().groupId("org.opencastproject").artifactId("opencast-workspace-impl").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.cxf").artifactId("cxf-bundle").version("2.3.0-20090930").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi").artifactId("cxf-dosgi-ri-discovery-local").version("1.1-20090930").update(false),
            CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi").artifactId("cxf-dosgi-ri-dsw-cxf").version("1.1-20090930").update(false)
            ));
  }

  @SuppressWarnings("unchecked")
  protected <T> T retrieveService(final Class<?> clazz) throws InterruptedException {
    ServiceTracker tracker = new ServiceTracker(bundleContext, clazz.getName(), null);
    tracker.open();
    T service = (T) tracker.waitForService(15 * 1000);
    tracker.close();
    Assert.assertNotNull(service);
    return service;
  }
  
}
