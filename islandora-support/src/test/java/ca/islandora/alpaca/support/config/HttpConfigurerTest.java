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

package ca.islandora.alpaca.support.config;

import static ca.islandora.alpaca.support.config.RequestConfigurerConfig.CONNECT_TIMEOUT_PROPERTY;
import static ca.islandora.alpaca.support.config.RequestConfigurerConfig.REQUEST_CONFIGURER_ENABLED_PROPERTY;
import static ca.islandora.alpaca.support.config.RequestConfigurerConfig.REQUEST_TIMEOUT_PROPERTY;
import static ca.islandora.alpaca.support.config.RequestConfigurerConfig.SOCKET_TIMEOUT_PROPERTY;
import static org.junit.Assert.assertEquals;

import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;


/**
 * Insures that the Camel HTTP component as configured by Blueprint is properly configured.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 * @author whikloj
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = HttpConfigurerTest.ContextConfig.class,
        loader = AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class HttpConfigurerTest {

    @Autowired
    private CamelContext context;

    /**
     * Ensure that the default RequestConfig for the HttpComponent carries the timeout values specified in the
     * blueprint xml.
     *
     * Note that the RequestConfig and RequestConfigConfigurer are difficult to test with mocking frameworks such as
     * Mockito due to the presence of final methods in the relevant HttpClient classes.
     *
     * @throws Exception
     */
    @Test
    public void testRequestConfig() throws Exception {
        final HttpComponent http = (HttpComponent) context.getComponent("http");

        assertEquals(11111, http.getSocketTimeout());
        assertEquals(22222, http.getConnectTimeout());
        assertEquals(33333, http.getConnectionRequestTimeout());
    }

    @BeforeClass
    public static void setProperties() {
        System.setProperty(REQUEST_CONFIGURER_ENABLED_PROPERTY, "true");
        System.setProperty(REQUEST_TIMEOUT_PROPERTY, "33333");
        System.setProperty(CONNECT_TIMEOUT_PROPERTY, "22222");
        System.setProperty(SOCKET_TIMEOUT_PROPERTY, "11111");
    }

    @Configuration
    @ComponentScan(basePackageClasses = {RequestConfigurerConfig.class, ActivemqConfig.class},
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {RequestConfigurerConfig.class, ActivemqConfig.class}))
    static class ContextConfig extends CamelConfiguration {

    }
}
