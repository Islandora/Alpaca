/*
 * Licensed to Islandora Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * The Islandora Foundation licenses this file to you under the MIT License.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.islandora.alpaca.karaf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.util.PathUtils.getBaseDir;
import static org.osgi.framework.Bundle.ACTIVE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;

import javax.inject.Inject;

import org.apache.karaf.features.FeaturesService;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;

/**
 * Test deployment in Karaf container.
 * @author whikloj
 * @since 2019-10-30
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@Ignore
public class KarafIT {

    private static Logger LOGGER = getLogger(KarafIT.class);

    @Inject
    protected FeaturesService featuresService;

    @Inject
    protected BundleContext bundleContext;

    @Configuration
    public Option[] config() throws Exception {

        final ConfigurationManager cm = new ConfigurationManager();

        final String version = cm.getProperty("project.version");
        final boolean debugRemote = Boolean.parseBoolean(cm.getProperty("debug.remote", "false"));
        final boolean debugExam = Boolean.parseBoolean(cm.getProperty("debug.keepExam", "false"));

        final String islandoraHttp = getBundleUri("islandora-http-client", version);
        final String isladnoraEvent = getBundleUri("islandora-event-support", version);
        final String islandoraIndexFcrepo = getBundleUri("islandora-indexing-fcrepo", version);
        final String islandoraIndexTriple = getBundleUri("islandora-indexing-triplestore", version);
        final String islandoraConnectDeriv = getBundleUri("islandora-connector-derivative", version);

        final String karafVersion = MavenUtils.getArtifactVersion("org.apache.karaf", "apache-karaf");
        final String fcrepoCamelVersion = MavenUtils.getArtifactVersion("org.fcrepo.camel",
                "fcrepo-camel");
        final String fcrepoCamelToolboxVersion = MavenUtils.getArtifactVersion("org.fcrepo.camel",
                "toolbox-features");
        final String activemqVersion = MavenUtils.getArtifactVersion("org.apache.activemq", "activemq-karaf");

        return options(
                when( debugRemote ).useOptions(
                       debugConfiguration( "5005", true )
                ),
                karafDistributionConfiguration()
                    .frameworkUrl(
                            maven()
                            .groupId("org.apache.karaf")
                            .artifactId("apache-karaf")
                            .version(karafVersion)
                            .type("zip")
                    )
                    .karafVersion(karafVersion)
                    .unpackDirectory(new File("build/exam"))
                    .useDeployFolder(false),
                when( debugExam ).useOptions(
                    keepRuntimeFolder()
                ),
                logLevel(LogLevel.INFO),
                configureConsole()
                    .ignoreLocalConsole()
                    .ignoreRemoteShell(),
                editConfigurationFilePut(
                    "etc/org.apache.karaf.features.repos.cfg",
                    "fcrepo-camel",
                    "mvn:org.fcrepo.camel/fcrepo-camel/" + fcrepoCamelVersion + "/xml/features"),
                editConfigurationFilePut(
                    "etc/org.apache.karaf.features.repos.cfg",
                    "fcrepo-camel-toolbox",
                    "mvn:org.fcrepo.camel/toolbox-features/" + fcrepoCamelToolboxVersion + "/xml/features"),
                editConfigurationFilePut(
                    "etc/org.apache.karaf.features.repos.cfg",
                    "activemq",
                    "mvn:org.apache.activemq/activemq-karaf/" + activemqVersion + "/xml/features"),
                editConfigurationFilePut(
                        "etc/org.apache.karaf.features.cfg",
                        "featuresBoot",
                        "standard"
                ),
                editConfigurationFilePut(
                        "etc/org.ops4j.pax.url.mvn.cfg",
                        "org.ops4j.pax.url.mvn.proxySupport",
                        "true"
                ),
                features(maven().groupId("org.apache.karaf.features").artifactId("standard")
                    .versionAsInProject().classifier("features").type("xml"), "scr"),
                features(maven().groupId("org.apache.camel.karaf").artifactId("apache-camel")
                    .type("xml").classifier("features").versionAsInProject(), "camel-blueprint",
                    "camel-http4", "camel-jackson", "camel-jsonpath", "camel-jackson", "camel-spring"),
                features(maven().groupId("org.apache.activemq").artifactId("activemq-karaf")
                        .type("xml").classifier("features").versionAsInProject(), "activemq-camel"),
                features(maven().groupId("org.fcrepo.camel").artifactId("fcrepo-camel")
                        .type("xml").classifier("features").versionAsInProject(), "fcrepo-camel"),
                mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-annotations")
                    .versionAsInProject().start(),
                mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpcore-osgi")
                    .versionAsInProject().start(),

                systemProperty("c.i.a.http-bundle").value(islandoraHttp),
                systemProperty("c.i.a.derivative-bundle").value(islandoraConnectDeriv),
                systemProperty("c.i.a.fcrepo-bundle").value(islandoraIndexFcrepo),
                systemProperty("c.i.a.triplestore-bundle").value(islandoraIndexTriple),

                bundle(islandoraHttp).start(),
                bundle(isladnoraEvent).start(),
                bundle(islandoraConnectDeriv).start(),
                bundle(islandoraIndexFcrepo).start(),
                bundle(islandoraIndexTriple).start(),
                editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg",
                        "log4j.logger.org.apache.camel.impl.converter", "ERROR, stdout")
        );

    }

    @Test
    public void testInstallation() throws Exception {
        assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-core")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("fcrepo-camel")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("activemq-camel")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-blueprint")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-http4")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-jackson")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-jsonpath")));
        assertNotNull(bundleContext);

        assertEquals(ACTIVE, bundleContext.getBundle(System.getProperty("c.i.a.http-bundle")).getState());
        assertEquals(ACTIVE, bundleContext.getBundle(System.getProperty("c.i.a.derivative-bundle")).getState());
        assertEquals(ACTIVE, bundleContext.getBundle(System.getProperty("c.i.a.fcrepo-bundle")).getState());
        assertEquals(ACTIVE, bundleContext.getBundle(System.getProperty("c.i.a.triplestore-bundle")).getState());
    }

    private static String getBundleUri(final String artifactId, final String version) {
        final File artifact = new File(getBaseDir() + "/../" + artifactId + "/build/libs/" +
                artifactId + "-" + version + ".jar");
        if (artifact.exists()) {
            return artifact.toURI().toString();
        }
        return "mvn:ca.islandora.alpaca/" + artifactId + "/" + version;
    }

    private String getFeaturesXml() throws Exception {
        return getClass().getClassLoader().getResource("features.xml").toURI().toString();
    }
}
